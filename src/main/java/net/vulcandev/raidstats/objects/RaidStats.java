package net.vulcandev.raidstats.objects;

import com.golfing8.kore.object.Raid;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a single raid with all associated statistics for both factions.
 * Tracks player stats, manages grace periods, and provides stat aggregation methods.
 */
public class RaidStats {
    @Getter
    private final UUID id;
    @Getter
    private final String raidingFaction;
    @Getter
    private final String defendingFaction;
    // Maps faction ID -> (player UUID -> stats)
    private final Map<String, Map<UUID, PlayerStats>> factionStats;
    @Setter
    @Getter
    private transient Raid koreRaid;
    @Getter
    @Setter
    private long purgeTime = -1L;

    public RaidStats(String raidingFaction, String defendingFaction, Raid koreRaid) {
        this.id = UUID.randomUUID();
        this.koreRaid = koreRaid;
        this.raidingFaction = raidingFaction;
        this.defendingFaction = defendingFaction;
        this.factionStats = new HashMap<>();

        // Initialize stats maps for both factions
        this.factionStats.put(raidingFaction, new HashMap<>());
        this.factionStats.put(defendingFaction, new HashMap<>());
    }

    /**
     * Checks if this raid is in grace period (stats frozen).
     */
    public boolean isGrace() {return purgeTime != -1;}

    /**
     * Gets all player stats for a specific faction.
     */
    public Map<UUID, PlayerStats> getStatsForFaction(String faction) {
        return factionStats.getOrDefault(faction, new HashMap<>());
    }

    /**
     * Gets stats for a specific player in a faction.
     */
    public PlayerStats getPlayerStats(String faction, UUID playerUUID) {
        return factionStats.getOrDefault(faction, new HashMap<>()).get(playerUUID);
    }

    /**
     * Updates a player's stats using a functional updater.
     * Creates a new PlayerStats object if one doesn't exist.
     */
    public void updatePlayerStats(String faction, UUID playerUUID, StatsUpdater updater) {
        factionStats.computeIfAbsent(faction, f -> new HashMap<>())
                    .compute(playerUUID, (uuid, stats) -> {
                        if (stats == null) stats = new PlayerStats();
                        updater.update(stats);
                        return stats;
                    });
    }

    /**
     * Gets the top players for a specific stat type, sorted in descending order.
     *
     * @param factionId The faction to get stats for
     * @param statType The type of stat to rank by
     * @param limit Maximum number of players to return
     * @return Ordered map of player UUIDs to their stats
     */
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

    /**
     * Calculates the total stats for all players in a faction.
     * Aggregates individual player stats into faction-wide totals.
     *
     * @param factionId The faction to calculate totals for
     * @return Map of stat types to their total values
     */
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
}
