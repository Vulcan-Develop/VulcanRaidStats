package net.xantharddev.raidstats.data;

import net.xantharddev.raidstats.RaidStats;
import net.xantharddev.raidstats.manager.StatsManager;
import net.xantharddev.raidstats.objects.PlayerStats;
import net.xantharddev.raidstats.objects.RaidObject;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;

import java.io.File;

public class DataManager {
    private final File dataFolder;
    private final StatsManager statsManager;
    private final RaidStats plugin;

    public DataManager(File dataFolder, StatsManager statsManager, RaidStats plugin) {
        this.statsManager = statsManager;
        this.plugin = plugin;
        if (dataFolder == null || !dataFolder.isDirectory()) {
            throw new IllegalArgumentException("The provided dataFolder is invalid or not a directory: " + dataFolder);
        }

        this.dataFolder = new File(dataFolder, "data");

        if (!this.dataFolder.exists()) {
            boolean created = this.dataFolder.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create 'data' folder at: " + this.dataFolder.getPath());
            }
        }
    }

    public void saveAllRaids() {
        File[] files = dataFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }

        for (RaidObject raidObject : statsManager.getAllRaids()) {
            String raidId = raidObject.getId().toString();
            File raidFile = new File(dataFolder, raidId + ".yml");

            Map<String, Object> raidData = new HashMap<>();
            raidData.put("raidingID", raidObject.getRaidingFaction());
            raidData.put("defendingID", raidObject.getDefendingFaction());

            raidData.put("raidingFaction", saveFactionStats(raidObject.getStatsForFaction(raidObject.getRaidingFaction())));
            raidData.put("defendingFaction", saveFactionStats(raidObject.getStatsForFaction(raidObject.getDefendingFaction())));

            saveRaidDataToFile(raidFile, raidData);
        }
    }


    private Map<String, Object> saveFactionStats(Map<UUID, PlayerStats> playerStats) {
        Map<String, Object> factionData = new HashMap<>();

        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            UUID playerUUID = entry.getKey();
            PlayerStats stats = entry.getValue();

            Map<String, Object> playerData = new HashMap<>();
            playerData.put("kills", stats.getKills());
            playerData.put("deaths", stats.getDeaths());
            playerData.put("hitsGiven", stats.getHitsDealt());
            playerData.put("hitsTaken", stats.getHitsTaken());
            playerData.put("blocksPlaced", stats.getBlocksPlaced());

            playerData.put("damageDealt", stats.getDamageDealt());
            playerData.put("damageTaken", stats.getDamageTaken());

            factionData.put(playerUUID.toString(), playerData);
        }

        return factionData;
    }

    private void saveRaidDataToFile(File raidFile, Map<String, Object> raidData) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(raidFile);
            for (Map.Entry<String, Object> entry : raidData.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            config.save(raidFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadAllRaids() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null) {
            for (File file : files) {
                String raidId = file.getName().replace(".yml", "");  // Get raidId from filename (without extension)
                Bukkit.getLogger().info("Loading raid data from file: " + file.getName() + " (Raid ID: " + raidId + ")");

                Map<String, Object> raidData = loadRaidDataFromFile(file);

                if (raidData != null) {
                    Bukkit.getLogger().info("Raid data successfully loaded for Raid ID: " + raidId);

                    String raidingFaction = (String) raidData.get("raidingID");
                    String defendingFaction = (String) raidData.get("defendingID");

                    Bukkit.getLogger().info("Raiding Faction: " + raidingFaction + ", Defending Faction: " + defendingFaction);

                    Map<String, Map<UUID, PlayerStats>> factionStats = new HashMap<>();
                    factionStats.put(raidingFaction, loadFactionStats(raidData.get("raidingFaction")));
                    factionStats.put(defendingFaction, loadFactionStats(raidData.get("defendingFaction")));

                    UUID uuid = UUID.fromString(raidId);

                    plugin.getRaidTimer().getActiveRaids().stream()
                            .filter(raid -> raid.getRaided().equals(defendingFaction) && raid.getFaction().equals(raidingFaction))
                            .findAny()
                            .ifPresent(raid -> {
                                Bukkit.getLogger().info("Found matching active raid for Raid ID: " + raidId + ", adding to statsManager.");
                                statsManager.addRaid(new RaidObject(uuid, raidingFaction, defendingFaction, -1, raid, factionStats));
                            });

                    plugin.getRaidTimer().getGracePeriods().entrySet().stream()
                            .filter(entry -> entry.getKey().equals(defendingFaction) && entry.getValue().getA().equals(raidingFaction))
                            .findAny()
                            .ifPresent(grace -> {
                                Bukkit.getLogger().info("Found grace period for Raid ID: " + raidId + ", calculating purge time.");

                                // Get the grace period in minutes
                                int graceMins = grace.getValue().getB();

                                if (graceMins <= 0) {
                                    Bukkit.getLogger().info("Grace period is non-positive for Raid ID: " + raidId + ", skipping.");
                                    return;
                                }

                                // Convert grace period from minutes to milliseconds
                                long graceValueMillis = (graceMins + 1L) * 60L * 1000L;

                                // Calculate the new purge time by adding the grace period to the current Unix timestamp
                                long purgeUnix = System.currentTimeMillis() + graceValueMillis;

                                // Create and add a new RaidObject with the calculated purge time
                                Bukkit.getLogger().info("New purge time calculated: " + purgeUnix + " for Raid ID: " + raidId);
                                statsManager.addRaid(new RaidObject(uuid, raidingFaction, defendingFaction, purgeUnix, factionStats));

                                // Schedule the task to remove the raid after the grace period ends
                                long graceEndDelayTicks = graceValueMillis / 50L; // Convert milliseconds to ticks (1 tick = 50ms)
                                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                                    Bukkit.getLogger().info("Grace period ended, removing raid for Raid ID: " + raidId);
                                    statsManager.removeRaid(raidingFaction, defendingFaction);
                                }, graceEndDelayTicks);
                            });
                }
            }
        }
    }

    private Map<String, Object> loadRaidDataFromFile(File raidFile) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(raidFile);
            Map<String, Object> raidData = new HashMap<>();

            raidData.put("raidingID", config.getString("raidingID"));
            raidData.put("defendingID", config.getString("defendingID"));

            raidData.put("raidingFaction", config.getConfigurationSection("raidingFaction").getValues(false));
            raidData.put("defendingFaction", config.getConfigurationSection("defendingFaction").getValues(false));

            return raidData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Map<UUID, PlayerStats> loadFactionStats(Object factionData) {
        Map<UUID, PlayerStats> playerStatsMap = new HashMap<>();

        if (factionData instanceof Map) {
            Map<String, Object> factionDataMap = (Map<String, Object>) factionData;

            for (Map.Entry<String, Object> entry : factionDataMap.entrySet()) {
                String playerUUIDString = entry.getKey();
                Object playerData = entry.getValue();

                // Ensure playerData is a MemorySection (which can hold a map-like structure)
                if (playerData instanceof MemorySection) {
                    MemorySection statsSection = (MemorySection) playerData;

                    // Retrieve individual stats from the MemorySection
                    int kills = statsSection.getInt("kills");
                    int deaths = statsSection.getInt("deaths");
                    int hitsGiven = statsSection.getInt("hitsGiven");
                    int hitsTaken = statsSection.getInt("hitsTaken");
                    int blocksPlaced = statsSection.getInt("blocksPlaced");

                    double damageDealt = statsSection.getDouble("damageDealt");
                    double damageTaken = statsSection.getDouble("damageTaken");

                    UUID playerUUID = UUID.fromString(playerUUIDString);
                    PlayerStats stats = new PlayerStats(kills, deaths, blocksPlaced, damageDealt, damageTaken, hitsGiven, hitsTaken);

                    playerStatsMap.put(playerUUID, stats);
                }
            }
        }

        return playerStatsMap;
    }

}