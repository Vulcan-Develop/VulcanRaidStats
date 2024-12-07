package net.xantharddev.raidstats.listener;

import com.golfing8.kore.event.RaidEndEvent;
import com.golfing8.kore.event.RaidStartEvent;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import net.xantharddev.raidstats.RaidStats;
import net.xantharddev.raidstats.event.RaidStatsEndEvent;
import net.xantharddev.raidstats.manager.StatsManager;
import net.xantharddev.raidstats.objects.RaidObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class RaidListener implements Listener {
    private final RaidStats plugin;
    private final StatsManager statsManager;

    public RaidListener(RaidStats plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onRaidStart(RaidStartEvent event) {
        // Running async to avoid blocking the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String raidingFactionId = event.getFactionRaiding();
            String defendingFactionId = event.getFactionRaided();;

            // Initialize stats for both factions asynchronously
            statsManager.addRaid(new RaidObject(raidingFactionId, defendingFactionId, event.getRaid()));
        });
    }

    @EventHandler
    public void onRaidEnd(RaidEndEvent event) {
        // Schedule the task to run after 5 ticks (0.25 seconds = 5 ticks in Minecraft)
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {

            // Grab the grace period in minutes
            int graceMinutes = plugin.getRaidTimer().getGrace(event.getFactionRaided());

            RaidObject raidObject = statsManager.getRaidByFactionIds(event.getFactionRaiding(), event.getFactionRaided());
            if (graceMinutes <= 0) {
                callRaidEvent(raidObject);
                statsManager.removeRaid(event.getFactionRaiding(), event.getFactionRaided());
                return;
            }

            // Convert grace period from minutes to milliseconds
            long graceValueMillis = (graceMinutes + 1L) * 60L * 1000L; // graceMinutes + 1 to account for any additional time if needed

            // Get the current Unix timestamp in milliseconds and calculate the grace end timestamp
            long graceEndTimestamp = System.currentTimeMillis() + graceValueMillis;

            // Set Grace to stop stat padding (Adding to stats while in grace)
            raidObject.setPurgeTime(graceEndTimestamp);

            // Schedule the task to remove the raid entirely when grace is over
            long graceEndDelayTicks = (graceValueMillis / 50L); // Convert milliseconds to ticks (1 tick = 50ms)

            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                statsManager.removeRaid(event.getFactionRaiding(), event.getFactionRaided());
                callRaidEvent(raidObject);
            }, graceEndDelayTicks);

        }, 5L); // 5 ticks delay
    }

    private void callRaidEvent(RaidObject raidObject) {
        RaidStatsEndEvent event = new RaidStatsEndEvent(raidObject);
        Bukkit.getPluginManager().callEvent(event);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.OBSIDIAN) return;
        Player player = event.getPlayer();
        if (player == null) return;

        Faction fac = getFactionFromPlayer(player);

        // We are checking if the player is involved in a raid; do this asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            RaidObject raid = statsManager.getRaidDefendingByFacID(fac.getId());

            if (raid == null || raid.isGrace()) return;

            if (!isInBaseRegion(fac, event.getBlockPlaced().getLocation())) return;

            raid.addBlocksPlaced(fac.getId(), player.getUniqueId(), 1);
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();
        Player killerPlayer = deadPlayer.getKiller();

        if (killerPlayer == null) return;

        // Perform async check for raid stats updates
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Faction deadFac = getFactionFromPlayer(deadPlayer);
            Faction killerFac = getFactionFromPlayer(killerPlayer);

            RaidObject raid = statsManager.getRaidByFactionIds(deadFac.getId(), killerFac.getId());

            if (raid == null || raid.isGrace()) return;

            String raidingFactionId, defendingFactionId;
            boolean bothPlayersInBR;

            // Determine roles and whether both players are in base region
            if (raid.getRaidingFaction().equals(killerFac.getId())) {
                raidingFactionId = killerFac.getId();
                defendingFactionId = deadFac.getId();
                bothPlayersInBR = isInBaseRegion(deadFac, deadPlayer.getLocation());
            } else {
                raidingFactionId = deadFac.getId();
                defendingFactionId = killerFac.getId();
                bothPlayersInBR = isInBaseRegion(killerFac, killerPlayer.getLocation());
            }

            if (!bothPlayersInBR) return;

            // Update stats for the involved players
            updateStats(raid, raidingFactionId, defendingFactionId, killerFac, killerPlayer, deadPlayer);
        });
    }

    private void updateStats(RaidObject raid, String raidingFactionId, String defendingFactionId, Faction killerFac, Player killerPlayer, Player deadPlayer) {
        // Determine if the killer is in the raiding faction
        boolean isKillerRaiding = raidingFactionId.equals(killerFac.getId());

        // Update the killer's and dead player's stats based on their faction roles
        if (isKillerRaiding) {
            raid.addKill(raidingFactionId, killerPlayer.getUniqueId(), 1);
            raid.addDeath(defendingFactionId, deadPlayer.getUniqueId(), 1);
        } else {
            raid.addKill(defendingFactionId, killerPlayer.getUniqueId(), 1);
            raid.addDeath(raidingFactionId, deadPlayer.getUniqueId(), 1);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player damagedPlayer = (Player) event.getEntity();
        double damage = event.getDamage();

        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        if (attacker == null) return;

        // Perform async check for raid stats updates
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Faction damagedFac = getFactionFromPlayer(damagedPlayer);
            Faction attackerFac = getFactionFromPlayer(attacker);

            RaidObject raid = statsManager.getRaidByFactionIds(damagedFac.getId(), attackerFac.getId());
            if (raid == null || raid.isGrace()) return;

            // Update damage stats for both players
            updateDamageStats(raid, attacker, damagedPlayer, damage, raid.getRaidingFaction().equals(attackerFac.getId()));
        });
    }

    private void updateDamageStats(RaidObject raid, Player attacker, Player damagedPlayer, double damage, boolean isRaiding) {
        String raidingFactionId = raid.getRaidingFaction();
        String defendingFactionId = raid.getDefendingFaction();

        if (isRaiding) {
            raid.addDamageGiven(raidingFactionId, attacker.getUniqueId(), damage);
            raid.addDamageTaken(defendingFactionId, damagedPlayer.getUniqueId(), damage);
        } else {
            raid.addDamageGiven(defendingFactionId, attacker.getUniqueId(), damage);
            raid.addDamageTaken(raidingFactionId, damagedPlayer.getUniqueId(), damage);
        }
    }

    private boolean isInBaseRegion(Faction fac, Location location) {
        return fac.isInBaseRegion(new FLocation(location));
    }

    private Faction getFactionFromPlayer(Player player) {
        return FPlayers.getInstance().getByPlayer(player).getFaction();
    }
}
