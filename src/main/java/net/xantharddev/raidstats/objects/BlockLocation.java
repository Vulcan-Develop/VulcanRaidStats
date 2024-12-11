package net.xantharddev.raidstats.objects;

import org.bukkit.Location;

import java.util.Objects;

public class BlockLocation {
    private final String worldName;
    private final int x, y, z;

    public BlockLocation(Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BlockLocation that = (BlockLocation) obj;
        return x == that.x && y == that.y && z == that.z && Objects.equals(worldName, that.worldName);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z);
    }
}