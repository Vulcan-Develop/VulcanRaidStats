package net.xantharddev.raidstats.objects;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

public class PlayerStats {
    private int kills;
    private int deaths;
    private double damageDealt;
    private int hits;
    private double damageTaken;
    private Set<BlockLocation> blocksPlaced = new HashSet<>();
    private int blocksCaught;

    public PlayerStats(int kills, int deaths, int blocksCaught, Set<BlockLocation> blocksPlaced, double damageDealt, double damageTaken, int hits) {
        this.blocksCaught = blocksCaught;
        this.blocksPlaced = blocksPlaced;
        this.damageDealt = damageDealt;
        this.damageTaken = damageTaken;
        this.deaths = deaths;
        this.kills = kills;
        this.hits = hits;
    }

    public PlayerStats() {}

    // Getters and setters for each stat
    public int getKills() {
        return kills;
    }

    public void addKills(int kills) {
        this.kills += kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeaths(int deaths) {
        this.deaths += deaths;
    }

    public double getDamageDealt() {
        return damageDealt;
    }

    public void addDamageGiven(double damage) {
        this.damageDealt += damage;
        this.hits++;
    }

    public double getDamageTaken() {
        return damageTaken;
    }

    public void addDamageTaken(double damage) {
        this.damageTaken += damage;
        this.hits++;
    }

    public int getHits() {
        return hits;
    }

    public int getBlocksPlacedAmount() {
        return blocksPlaced.size();
    }

    public Set<BlockLocation> getBlocksPlaced() {
        return blocksPlaced;
    }

    public void addBlocksPlaced(Location location) {
        this.blocksPlaced.add(new BlockLocation(location));
    }

    public int getBlocksCaught() {
        return blocksCaught;
    }

    public void addBlocksCaught(Location location) {
        if (this.blocksPlaced.contains(new BlockLocation(location))) {
            this.blocksCaught++;
        }
    }
}