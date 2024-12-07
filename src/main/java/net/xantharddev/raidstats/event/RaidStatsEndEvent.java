package net.xantharddev.raidstats.event;

import net.xantharddev.raidstats.objects.RaidObject;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RaidStatsEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final RaidObject raidObject;

    public RaidStatsEndEvent(RaidObject raidObject) {
        this.raidObject = raidObject;
    }

    public RaidObject getRaidObject() { return raidObject; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
