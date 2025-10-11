package net.vulcandev.raidstats.listener;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import net.vulcandev.raidstats.manager.StatsManager;
import net.vulcandev.raidstats.objects.VulcanRaidStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;

/**
 * Tracks player statistics during active raids.
 * Monitors kills, deaths, damage, and blocks placed for both attacking and defending factions.
 */
public class StatsListener implements Listener {
    private final net.vulcandev.raidstats.VulcanRaidStats plugin;
    private final StatsManager statsManager;

    public StatsListener(net.vulcandev.raidstats.VulcanRaidStats plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    /**
     * Tracks blocks placed in enemy territory during raids.
     * Only counts blocks placed in base regions or raid outposts.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player player = event.getPlayer();
            if (player == null) return;

            Location loc = event.getBlock().getLocation();

            Faction fac = getFactionFromLoc(event.getBlock().getLocation());

            if (fac.isSystemFaction()) return;

            VulcanRaidStats raid = statsManager.getRaidDefendingByFacID(fac.getId());

            if (raid == null || raid.isGrace()) return;

            if (!isInBaseRegion(getFactionFromPlayer(player), loc)) return;

            raid.addBlocksPlaced(fac.getId(), player.getUniqueId());
        });
    }

    /**
     * Kills / Death's Raid Stats Tracking
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player deadPlayer = event.getEntity();
            Player killerPlayer = deadPlayer.getKiller();

            if (killerPlayer == null) return;

            // Retrieve factions for both players
            Faction deadFac = getFactionFromPlayer(deadPlayer);
            Faction killerFac = getFactionFromPlayer(killerPlayer);
            if (deadFac == null || killerFac == null) return;

            // Get the list of raids between these factions (Cases for raiding each other or RPost)
            List<VulcanRaidStats> raids = statsManager.getRaidsByFactionIds(deadFac.getId(), killerFac.getId());
            if (raids.isEmpty()) return;

            // Process each raid in the list
            for (VulcanRaidStats raid : raids) {
                // Skip raids in grace period
                if (raid.isGrace()) continue;

                boolean isAttackerRaiding = raid.getRaidingFaction().equals(killerFac.getId());

                updateKillTracking(raid, killerPlayer, deadPlayer, isAttackerRaiding);
            }
        });
    }

    private void updateKillTracking(VulcanRaidStats raid, Player killerPlayer, Player deadPlayer , boolean isAttackerRaiding) {
        String raidingFactionId = raid.getRaidingFaction();
        String defendingFactionId = raid.getDefendingFaction();

        if (isAttackerRaiding) {
            raid.addKill(raidingFactionId, killerPlayer.getUniqueId(), 1);
            raid.addDeath(defendingFactionId, deadPlayer.getUniqueId(), 1);
        } else {
            raid.addKill(defendingFactionId, killerPlayer.getUniqueId(), 1);
            raid.addDeath(raidingFactionId, deadPlayer.getUniqueId(), 1);
        }
    }

    /**
     * Damage Taken / Given Raid Stats Tracking
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (event.isCancelled()) return;

            if (!(event.getEntity() instanceof Player)) return;

            Player damagedPlayer = (Player) event.getEntity();
            double damage = event.getDamage();

            if (!(event.getDamager() instanceof Player)) return;

            Player attacker = (Player) event.getDamager();

            Faction damagedFac = getFactionFromPlayer(damagedPlayer);
            Faction attackerFac = getFactionFromPlayer(attacker);
            if (damagedFac == null || attackerFac == null) return;

            List<VulcanRaidStats> raids = statsManager.getRaidsByFactionIds(damagedFac.getId(), attackerFac.getId());
            if (raids.isEmpty()) return;

            for (VulcanRaidStats raid : raids) {
                if (raid.isGrace()) continue;

                boolean isAttackerRaiding = raid.getRaidingFaction().equals(attackerFac.getId());

                updateDamageStats(raid, attacker, damagedPlayer, damage, isAttackerRaiding);
            }
        });
    }

    private void updateDamageStats(VulcanRaidStats raid, Player attacker, Player damagedPlayer, double damage, boolean isAttackerRaiding) {
        String raidingFactionId = raid.getRaidingFaction();
        String defendingFactionId = raid.getDefendingFaction();

        if (isAttackerRaiding) {
            raid.addDamageGiven(raidingFactionId, attacker.getUniqueId(), damage);
            raid.addDamageTaken(defendingFactionId, damagedPlayer.getUniqueId(), damage);
        } else {
            raid.addDamageGiven(defendingFactionId, attacker.getUniqueId(), damage);
            raid.addDamageTaken(raidingFactionId, damagedPlayer.getUniqueId(), damage);
        }
    }

    private boolean isInBaseRegion(Faction fac, Location location) {
        if (isLocInRpost(location)) return true;
        return fac.isInBaseRegion(new FLocation(location));
    }

    private boolean isLocInRpost(Location location) {
        return statsManager.isLocInRPost(location);
    }

    private Faction getFactionFromLoc(Location loc) {
        return Board.getInstance().getFactionAt(new FLocation(loc));
    }

    private Faction getFactionFromPlayer(Player player) {
        return FPlayers.getInstance().getByPlayer(player).getFaction();
    }
}
