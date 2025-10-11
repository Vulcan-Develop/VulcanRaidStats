package net.vulcandev.raidstats.data;

import com.google.gson.reflect.TypeToken;
import net.vulcandev.raidstats.manager.StatsManager;
import net.vulcandev.raidstats.objects.VulcanRaidStats;
import net.xantharddev.vulcanlib.libs.DataUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistent storage of raid data to disk.
 * Saves and loads raids from JSON format, preserving stats across server restarts.
 */
public class DataManager {
    private final File raidsFile;
    private final StatsManager statsManager;
    private final net.vulcandev.raidstats.VulcanRaidStats plugin;

    public DataManager(File dataFolder, StatsManager statsManager, net.vulcandev.raidstats.VulcanRaidStats plugin) {
        this.statsManager = statsManager;
        this.plugin = plugin;
        if (dataFolder == null || !dataFolder.isDirectory()) {
            throw new IllegalArgumentException("The provided dataFolder is invalid or not a directory: " + dataFolder);
        }

        File dataFolder1 = new File(dataFolder, "data");
        this.raidsFile = new File(dataFolder1, "raids.json");

        if (!dataFolder1.exists()) {
            boolean created = dataFolder1.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create 'data' folder at: " + dataFolder1.getPath());
            }
        }
    }

    /**
     * Saves all active raids to JSON file.
     * Called during plugin shutdown to preserve raid data.
     */
    public void saveAllRaids() {
        DataUtils.saveToJson(raidsFile, statsManager.getAllRaids(), false);
    }

    /**
     * Loads all raids from JSON file on server startup.
     * Attempts to reattach raids to active FactionsKore raids and schedules grace period cleanup.
     */
    public void loadAllRaids() {
        // Load all raids from the single JSON file
        Type type = new TypeToken<List<VulcanRaidStats>>() {}.getType();
        List<VulcanRaidStats> raidsList = DataUtils.loadFromJson(raidsFile, type, ArrayList::new);

        if (raidsList != null && !raidsList.isEmpty()) {
            for (VulcanRaidStats vulcanRaidStats : raidsList) {
                String raidingFaction = vulcanRaidStats.getRaidingFaction();
                String defendingFaction = vulcanRaidStats.getDefendingFaction();

                // Try to attach to active raid
                plugin.getRaidTimer().getActiveRaids().stream()
                        .filter(raid -> raid.getRaided().equals(defendingFaction) && raid.getFaction().equals(raidingFaction))
                        .findAny()
                        .ifPresent(vulcanRaidStats::setKoreRaid);

                // If in grace period and has a valid purgeTime, schedule the purge task
                if (vulcanRaidStats.isGrace()) {
                    long currentTime = System.currentTimeMillis();
                    long purgeTime = vulcanRaidStats.getPurgeTime();

                    if (purgeTime > currentTime) {
                        long delayMillis = purgeTime - currentTime;
                        long delayTicks = delayMillis / 50L;

                        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> statsManager.removeRaid(raidingFaction, defendingFaction), delayTicks);
                    } else {
                        // Grace period expired, don't load this raid
                        continue;
                    }
                }

                statsManager.addRaid(vulcanRaidStats);
            }
        }
    }

}