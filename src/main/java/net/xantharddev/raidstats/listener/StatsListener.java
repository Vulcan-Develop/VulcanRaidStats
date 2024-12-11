package net.xantharddev.raidstats.listener;

import com.massivecraft.factions.*;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.World;
import net.xantharddev.raidstats.RaidStats;
import net.xantharddev.raidstats.manager.StatsManager;
import net.xantharddev.raidstats.objects.RaidObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

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
            if (event.getBlockPlaced().getType() != Material.OBSIDIAN) return;
            Player player = event.getPlayer();
            if (player == null) return;

            Faction fac = getFactionFromPlayer(player);

            RaidObject raid = statsManager.getRaidDefendingByFacID(fac.getId());

            if (raid == null || raid.isGrace()) return;

            Location loc = event.getBlock().getLocation();

            if (!isInBaseRegion(fac, loc)) return;

            raid.addBlocksPlaced(fac.getId(), player.getUniqueId(), loc);
        });
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (event.getEntity() instanceof TNTPrimed) {
                TNTPrimed tnt = (TNTPrimed) event.getEntity();
                Location tntLocation = tnt.getLocation();

                Faction faction = Board.getInstance().getFactionAt(new FLocation(tntLocation));
                if (faction.isSystemFaction() || !isInBaseRegion(faction, tntLocation)) return;

                RaidObject raid = statsManager.getRaidDefendingByFacID(faction.getId());
                if (raid == null || raid.isGrace()) return;

                for (Block block : event.blockList()) {
                    BlockPosition pos = new BlockPosition(block.getX(), block.getY(), block.getZ());
                    World world = ((CraftWorld) block.getWorld()).getHandle();
                    if (world.getDurability(pos) == 0) raid.addBlocksCaught(faction.getId(), block.getLocation());
                }
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Player deadPlayer = event.getEntity();
            Player killerPlayer = deadPlayer.getKiller();

            if (killerPlayer == null) return;
            Faction deadFac = getFactionFromPlayer(deadPlayer);
            Faction killerFac = getFactionFromPlayer(killerPlayer);

            RaidObject raid = statsManager.getRaidByFactionIds(deadFac.getId(), killerFac.getId());

            if (raid == null || raid.isGrace()) return;

            String raidingFactionId, defendingFactionId;
            boolean bothPlayersInBR;

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

            updateStats(raid, raidingFactionId, defendingFactionId, killerFac, killerPlayer, deadPlayer);
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!(event.getEntity() instanceof Player)) return;

            Player damagedPlayer = (Player) event.getEntity();
            double damage = event.getDamage();

            if (!(event.getDamager() instanceof Player)) return;

            Player attacker = (Player) event.getDamager();
            if (attacker == null) return;

            Faction damagedFac = getFactionFromPlayer(damagedPlayer);
            Faction attackerFac = getFactionFromPlayer(attacker);

            RaidObject raid = statsManager.getRaidByFactionIds(damagedFac.getId(), attackerFac.getId());
            if (raid == null || raid.isGrace()) return;

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
