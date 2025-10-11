package net.vulcandev.raidstats.objects;

/**
 * Functional interface for updating player statistics.
 * Used to encapsulate stat modification logic when updating raid stats.
 */
@FunctionalInterface
public interface StatsUpdater {
    /**
     * Updates the given player stats object.
     *
     * @param stats The player stats to modify
     */
    void update(PlayerStats stats);
}
