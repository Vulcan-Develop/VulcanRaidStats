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
            List<RaidObject> raids = statsManager.getRaidsByFactionIds(deadFac.getId(), killerFac.getId());
            if (raids.isEmpty()) return;

            // Process each raid in the list
            for (RaidObject raid : raids) {
                // Skip raids in grace period
                if (raid.isGrace()) continue;

                boolean isAttackerRaiding = raid.getRaidingFaction().equals(killerFac.getId());

                updateKillTracking(raid, killerPlayer, deadPlayer, isAttackerRaiding);
            }
        });
    }

    private void updateKillTracking(RaidObject raid, Player killerPlayer, Player deadPlayer , boolean isAttackerRaiding) {
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

            List<RaidObject> raids = statsManager.getRaidsByFactionIds(damagedFac.getId(), attackerFac.getId());
            if (raids.isEmpty()) return;

            for (RaidObject raid : raids) {
                if (raid.isGrace()) continue;

                boolean isAttackerRaiding = raid.getRaidingFaction().equals(attackerFac.getId());

                updateDamageStats(raid, attacker, damagedPlayer, damage, isAttackerRaiding);
            }
        });
    }

    private void updateDamageStats(RaidObject raid, Player attacker, Player damagedPlayer, double damage, boolean isAttackerRaiding) {
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
