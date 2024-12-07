package net.xantharddev.raidstats.objects;

@FunctionalInterface
public interface StatsUpdater {
    void update(PlayerStats stats);
}
