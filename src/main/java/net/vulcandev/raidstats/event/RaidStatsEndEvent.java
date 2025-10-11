package net.vulcandev.raidstats.event;

import net.vulcandev.raidstats.objects.VulcanRaidStats;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Custom event fired when a raid ends and stats are finalized.
 * Other plugins can listen to this event to access final raid statistics.
 */
public class RaidStatsEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final VulcanRaidStats vulcanRaidStats;

    public RaidStatsEndEvent(VulcanRaidStats vulcanRaidStats) {
        this.vulcanRaidStats = vulcanRaidStats;
    }

    /**
     * Gets the raid object containing all final statistics.
     *
     * @return The completed raid object
     */
    public VulcanRaidStats getRaidObject() { return vulcanRaidStats; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
