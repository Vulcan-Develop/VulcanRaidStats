package net.vulcandev.raidstats.listener;

import com.golfing8.kore.event.RaidEndEvent;
import com.golfing8.kore.event.RaidStartEvent;
import net.vulcandev.raidstats.event.RaidStatsEndEvent;
import net.vulcandev.raidstats.manager.StatsManager;
import net.vulcandev.raidstats.objects.VulcanRaidStats;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listens for raid lifecycle events from FactionsKore.
 * Manages raid tracking initialization and cleanup, including grace period handling.
 */
public class RaidEventListener implements Listener {
    private final net.vulcandev.raidstats.VulcanRaidStats plugin;
    private final StatsManager statsManager;

    public RaidEventListener(net.vulcandev.raidstats.VulcanRaidStats plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    /**
     * Called when a raid starts.
     * Creates a new raid tracking object for stats collection.
     */
    @EventHandler
    public void onRaidStart(RaidStartEvent event) {
        // Running async to avoid blocking the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String raidingFactionId = event.getFactionRaiding();
            String defendingFactionId = event.getFactionRaided();

            // Initialize stats for both factions asynchronously
            statsManager.addRaid(new VulcanRaidStats(raidingFactionId, defendingFactionId, event.getRaid()));
        });
    }

    /**
     * Called when a raid ends.
     * Handles grace period logic and schedules raid cleanup.
     * During grace, stats are frozen to prevent padding.
     */
    @EventHandler
    public void onRaidEnd(RaidEndEvent event) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            int graceMinutes = plugin.getRaidTimer().getGrace(event.getFactionRaided());

            VulcanRaidStats vulcanRaidStats = statsManager.getRaidDefendingByFacID(event.getFactionRaided());
            if (graceMinutes <= 0) {
                callRaidEvent(vulcanRaidStats);
                statsManager.removeRaid(event.getFactionRaiding(), event.getFactionRaided());
                return;
            }

            // Convert grace period from minutes to milliseconds
            long graceValueMillis = (graceMinutes + 1L) * 60L * 1000L; // graceMinutes + 1 to account for any additional time if needed

            // Get the current Unix timestamp in milliseconds and calculate the grace end timestamp
            long graceEndTimestamp = System.currentTimeMillis() + graceValueMillis;

            // Set Grace to stop stat padding (Adding to stats while in grace)
            vulcanRaidStats.setPurgeTime(graceEndTimestamp);

            // Schedule the task to remove the raid entirely when grace is over
            long graceEndDelayTicks = (graceValueMillis / 50L); // Convert milliseconds to ticks (1 tick = 50ms)

            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                callRaidEvent(vulcanRaidStats);
                statsManager.removeRaid(event.getFactionRaiding(), event.getFactionRaided());
            }, graceEndDelayTicks);

        }, 5L); // 5 ticks delay
    }

    /**
     * Fires a custom RaidStatsEndEvent for other plugins to hook into.
     */
    private void callRaidEvent(VulcanRaidStats vulcanRaidStats) {
        RaidStatsEndEvent event = new RaidStatsEndEvent(vulcanRaidStats);
        Bukkit.getPluginManager().callEvent(event);
    }
}
