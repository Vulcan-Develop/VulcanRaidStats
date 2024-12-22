package net.xantharddev.raidstats.objects;

import com.golfing8.kore.object.Raid;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

public class RaidObject {
    private final UUID id;
    private final String raidingFaction;
    private final String defendingFaction;
    private final Map<String, Map<UUID, PlayerStats>> factionStats; // Maps faction name (Defend / Raid) -> (player UUID -> stats)
    private Raid koreRaid;
    private long purgeTime = -1L;
    private Set<BlockLocation> blocksBlown = new HashSet<>();

    public RaidObject(String raidingFaction, String defendingFaction, Raid koreRaid) {
        this.id = UUID.randomUUID();
        this.koreRaid = koreRaid;
        this.raidingFaction = raidingFaction;
        this.defendingFaction = defendingFaction;
        this.factionStats = new HashMap<>();

        // Initialize stats maps for both factions
        this.factionStats.put(raidingFaction, new HashMap<>());
        this.factionStats.put(defendingFaction, new HashMap<>());
    }

    // Constructor for loading from YAML
    public RaidObject(UUID id, String raidingFaction, String defendingFaction, long purgeTime, Raid koreRaid, Map<String, Map<UUID, PlayerStats>> factionStats) {
        this.id = id;
        this.raidingFaction = raidingFaction;
        this.defendingFaction = defendingFaction;
        this.koreRaid = koreRaid;
        this.purgeTime = purgeTime;
        this.factionStats = factionStats;
    }

    public RaidObject(UUID id, String raidingFaction, String defendingFaction, long purgeTime, Map<String, Map<UUID, PlayerStats>> factionStats) {
        this.id = id;
        this.raidingFaction = raidingFaction;
        this.defendingFaction = defendingFaction;
        this.koreRaid = null;
        this.purgeTime = purgeTime;
        this.factionStats = factionStats;
    }

    public boolean isGrace() {return purgeTime != -1;}

    public void setPurgeTime(long purgeTime) {this.purgeTime = purgeTime;}

    public UUID getId() {return id;}

    public Raid getKoreRaid() { return koreRaid; }

    public void setKoreRaid(Raid raid) { this.koreRaid = raid; }

    public String getRaidingFaction() {
        return raidingFaction;
    }

    public String getDefendingFaction() {
        return defendingFaction;
    }

    public Map<UUID, PlayerStats> getStatsForFaction(String faction) {
        return factionStats.getOrDefault(faction, new HashMap<>());
    }

    public void updatePlayerStats(String faction, UUID playerUUID, StatsUpdater updater) {
        factionStats.computeIfAbsent(faction, f -> new HashMap<>())
                    .compute(playerUUID, (uuid, stats) -> {
                        if (stats == null) stats = new PlayerStats();
                        updater.update(stats);
                        return stats;
                    });
    }

    public Map<UUID, PlayerStats> getTopStats(String factionId, RaidStatType statType, int limit) {
        Map<UUID, PlayerStats> factionStats = getStatsForFaction(factionId);

        return factionStats.entrySet().stream()
                .sorted((entry1, entry2) -> {
                    int stat1 = statType.getValue(entry1.getValue());
                    int stat2 = statType.getValue(entry2.getValue());
                    return Integer.compare(stat2, stat1); // Descending order
                })
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, // Merge function in case of duplicate keys
                        LinkedHashMap::new
                ));
    }

    /**
     * Adds a kill to the specified player in the specified faction.
     *
     * @param faction   The faction of the player (raiding or defending).
     * @param playerUUID The UUID of the player.
     * @param kills     The number of kills to add (can be negative for decrement).
     */
    public void addKill(String faction, UUID playerUUID, int kills) {
        updatePlayerStats(faction, playerUUID, stats -> stats.addKills(kills));
    }

    /**
     * Adds a death to the specified player in the specified faction.
     *
     * @param faction   The faction of the player (raiding or defending).
     * @param playerUUID The UUID of the player.
     * @param deaths    The number of deaths to add (can be negative for decrement).
     */
    public void addDeath(String faction, UUID playerUUID, int deaths) {
        updatePlayerStats(faction, playerUUID, stats -> stats.addDeaths(deaths));
    }

    /**
     * Adds damage given to the specified player in the specified faction.
     *
     * @param faction   The faction of the player (raiding or defending).
     * @param playerUUID The UUID of the player.
     * @param damage    The amount of damage given to add (can be negative for decrement).
     */
    public void addDamageGiven(String faction, UUID playerUUID, double damage) {
        updatePlayerStats(faction, playerUUID, stats -> stats.addDamageGiven(damage));
    }

    /**
     * Adds damage taken to the specified player in the specified faction.
     *
     * @param faction   The faction of the player (raiding or defending).
     * @param playerUUID The UUID of the player.
     * @param damage    The amount of damage taken to add (can be negative for decrement).
     */
    public void addDamageTaken(String faction, UUID playerUUID, double damage) {
        updatePlayerStats(faction, playerUUID, stats -> stats.addDamageTaken(damage));
    }

    /**
     * Adds blocks placed to the specified player in the specified faction.
     *
     * @param faction   The faction of the player (raiding or defending).
     * @param playerUUID The UUID of the player.
     */
    public void addBlocksPlaced(String faction, UUID playerUUID) {
        updatePlayerStats(faction, playerUUID, PlayerStats::addBlocksPlaced);
    }

    public Map<RaidStatType, Integer> getFactionTotals(String factionId) {
        // Initialize totals map with default values for both integer and double stats
        Map<RaidStatType, Integer> combinedTotals = new EnumMap<>(RaidStatType.class);

        // Initialize the map with zero values for all stat types
        Arrays.stream(RaidStatType.values())
                .forEach(statType -> combinedTotals.put(statType, 0));

        // Get the stats for the given faction
        Map<UUID, PlayerStats> statsForFaction = getStatsForFaction(factionId);

        // Sum up the stats for all players in the faction
        for (PlayerStats stats : statsForFaction.values()) {
            combinedTotals.merge(RaidStatType.KILLS, stats.getKills(), Integer::sum);
            combinedTotals.merge(RaidStatType.DEATHS, stats.getDeaths(), Integer::sum);
            combinedTotals.merge(RaidStatType.BLOCKS_PLACED, stats.getBlocksPlaced(), Integer::sum);
            combinedTotals.merge(RaidStatType.HITS_DEALT, stats.getHitsDealt(), Integer::sum);
            combinedTotals.merge(RaidStatType.HITS_TAKEN, stats.getHitsTaken(), Integer::sum);
            combinedTotals.merge(RaidStatType.DAMAGE_GIVEN, stats.getDamageDealt(), Integer::sum);
            combinedTotals.merge(RaidStatType.DAMAGE_TAKEN, stats.getDamageTaken(), Integer::sum);
        }

        return combinedTotals;
    }



    /**
     * Adds blocks caught to the specified player in the specified faction.
     *
     * @param faction   The faction of the player (raiding or defending).
     * @param locations Location of the block placed
     *//*
    public boolean addBlocksCaught(String faction, List<Location> locations) {
        Map<UUID, PlayerStats> defendingPlayers = getStatsForFaction(faction);

        // Iterate through each player's stats in the faction
        for (PlayerStats stats : defendingPlayers.values()) {
            if (stats.addBlocksCaught(locations, blocksBlown)) {
                return true;
            }
        }
        return false;
    }*/
}
