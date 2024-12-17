package net.xantharddev.raidstats.listener;

import com.massivecraft.factions.*;
import net.xantharddev.raidstats.RaidStats;
import net.xantharddev.raidstats.manager.StatsManager;
import net.xantharddev.raidstats.objects.RaidObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.*;

public class StatsListener implements Listener {
    private final RaidStats plugin;
    private final StatsManager statsManager;

    public StatsListener(RaidStats plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player player = event.getPlayer();
            if (player == null) return;

            Location loc = event.getBlock().getLocation();

            Faction fac = getFactionFromLoc(event.getBlock().getLocation());

            if (fac.isSystemFaction()) return;

            RaidObject raid = statsManager.getRaidDefendingByFacID(fac.getId());

            if (raid == null || raid.isGrace()) return;

            if (!isInBaseRegion(getFactionFromPlayer(player), loc)) return;

            raid.addBlocksPlaced(fac.getId(), player.getUniqueId());
        });
    }

    /*
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Ensure the exploding entity is TNT
        if (event.getEntity() instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) event.getEntity();
            Location tntLocation = tnt.getLocation();

            // Get the faction associated with the TNT's location
            Faction faction = getFactionFromLoc(tntLocation);
            if (faction == null || faction.isSystemFaction() || !isInBaseRegion(faction, tntLocation)) return;

            // Cooldown check
            String factionId = faction.getId();

            // Check for an active raid
            RaidObject raid = statsManager.getRaidDefendingByFacID(factionId);
            if (raid == null || raid.isGrace()) return;

            // Filter exploded blocks with zero durability
            List<Location> locations = event.blockList().stream()
                    .filter(block -> block.getWorld().getDurabilityAt(block.getX(), block.getY(), block.getZ()) == 0)
                    .map(Block::getLocation)
                    .collect(Collectors.toList());

            // Check if blocks caught match the cannon-patched condition
            raid.addBlocksCaught(factionId, locations);
        }
    }*/


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

            // Get the list of raids between these factions
            List<RaidObject> raids = statsManager.getRaidsByFactionIds(deadFac.getId(), killerFac.getId());
            if (raids.isEmpty()) return;

            // Process each raid in the list
            for (RaidObject raid : raids) {
                // Skip raids in grace period
                if (raid.isGrace()) continue;

                String raidingFactionId, defendingFactionId;
                boolean bothPlayersInBR;

                // Determine if the killer belongs to the raiding faction
                if (raid.getRaidingFaction().equals(killerFac.getId())) {
                    raidingFactionId = killerFac.getId();
                    defendingFactionId = deadFac.getId();
                    bothPlayersInBR = isInBaseRegion(deadFac, deadPlayer.getLocation());
                } else {
                    raidingFactionId = deadFac.getId();
                    defendingFactionId = killerFac.getId();
                    bothPlayersInBR = isInBaseRegion(killerFac, killerPlayer.getLocation());
                }

                // Only update stats if both players are in the base region
                if (!bothPlayersInBR) continue;

                // Update stats for this raid
                updateStats(raid, raidingFactionId, defendingFactionId, killerFac, killerPlayer, deadPlayer);
            }
        });
    }

    private void updateStats(RaidObject raid, String raidingFactionId, String defendingFactionId, Faction killerFac, Player killerPlayer, Player deadPlayer) {
        boolean isKillerRaiding = raidingFactionId.equals(killerFac.getId());

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
        // Run asynchronously for performance
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Ensure the damaged entity is a player
            if (!(event.getEntity() instanceof Player)) return;
            Player damagedPlayer = (Player) event.getEntity();
            double damage = event.getDamage();

            // Ensure the damager is a player
            if (!(event.getDamager() instanceof Player)) return;
            Player attacker = (Player) event.getDamager();
            if (attacker == null) return;

            // Retrieve factions for both players
            Faction damagedFac = getFactionFromPlayer(damagedPlayer);
            Faction attackerFac = getFactionFromPlayer(attacker);
            if (damagedFac == null || attackerFac == null) return;

            // Get the list of raids between these factions
            List<RaidObject> raids = statsManager.getRaidsByFactionIds(damagedFac.getId(), attackerFac.getId());
            if (raids.isEmpty()) return;

            // Process each raid in the list
            for (RaidObject raid : raids) {
                // Skip if the raid is in grace period
                if (raid.isGrace()) continue;

                // Determine if the attacker belongs to the raiding faction
                boolean isAttackerRaiding = raid.getRaidingFaction().equals(attackerFac.getId());

                // Update raid damage stats
                updateDamageStats(raid, attacker, damagedPlayer, damage, isAttackerRaiding);
            }
        });
    }

    private void updateDamageStats(RaidObject raid, Player attacker, Player damagedPlayer, double damage, boolean isAttackerRaiding) {
        String raidingFactionId = raid.getRaidingFaction();
        String defendingFactionId = raid.getDefendingFaction();

        if (!isInBaseRegion(Factions.getInstance().getFactionById(defendingFactionId), attacker.getLocation())) return;

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
