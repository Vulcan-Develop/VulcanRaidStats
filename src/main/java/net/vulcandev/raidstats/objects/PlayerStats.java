package net.vulcandev.raidstats.objects;

import lombok.Getter;

/**
 * Stores combat and raid statistics for an individual player.
 * Tracks kills, deaths, damage dealt/taken, and blocks placed during a raid.
 */
public class PlayerStats {
    @Getter
    private int kills;
    @Getter
    private int deaths;
    private double damageDealt;
    @Getter
    private int hitsDealt;
    @Getter
    private int hitsTaken;
    private double damageTaken;
    @Getter
    private int blocksPlaced;

    /**
     * Adds kills to the player's total.
     */
    public void addKills(int kills) {
        this.kills += kills;
    }

    /**
     * Adds deaths to the player's total.
     */
    public void addDeaths(int deaths) {
        this.deaths += deaths;
    }

    /**
     * Returns total damage dealt as an integer.
     */
    public int getDamageDealt() {
        return (int) damageDealt;
    }

    /**
     * Records damage dealt and increments hit counter.
     */
    public void addDamageGiven(double damage) {
        this.damageDealt += damage;
        this.hitsDealt++;
    }

    /**
     * Returns total damage taken as an integer.
     */
    public int getDamageTaken() {
        return (int) damageTaken;
    }

    /**
     * Records damage taken and increments hit counter.
     */
    public void addDamageTaken(double damage) {
        this.damageTaken += damage;
        this.hitsTaken++;
    }

    /**
     * Increments the blocks placed counter.
     */
    public void addBlocksPlaced() {
        this.blocksPlaced++;
    }
}