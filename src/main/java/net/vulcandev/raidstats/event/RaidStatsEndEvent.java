package net.vulcandev.raidstats.event;

import net.vulcandev.raidstats.objects.RaidStats;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Custom event fired when a raid ends and stats are finalized.
 * Other plugins can listen to this event to access final raid statistics.
 */
public class RaidStatsEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final RaidStats raidStats;

    public RaidStatsEndEvent(RaidStats raidStats) {
        this.raidStats = raidStats;
    }

    /**
     * Gets the raid object containing all final statistics.
     *
     * @return The completed raid object
     */
    public RaidStats getRaidObject() { return raidStats; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
