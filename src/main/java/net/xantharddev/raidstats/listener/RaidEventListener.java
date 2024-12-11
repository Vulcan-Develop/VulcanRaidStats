package net.xantharddev.raidstats.listener;

import com.golfing8.kore.event.RaidEndEvent;
import com.golfing8.kore.event.RaidStartEvent;
import net.xantharddev.raidstats.RaidStats;
import net.xantharddev.raidstats.event.RaidStatsEndEvent;
import net.xantharddev.raidstats.manager.StatsManager;
import net.xantharddev.raidstats.objects.RaidObject;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RaidEventListener implements Listener {
    private final RaidStats plugin;
    private final StatsManager statsManager;

    public RaidEventListener(RaidStats plugin, StatsManager statsManager) {
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
}
