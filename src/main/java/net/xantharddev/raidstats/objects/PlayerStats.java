package net.xantharddev.raidstats.objects;

import java.util.HashMap;
import java.util.Map;

public class PlayerStats {
    private int kills;
    private int deaths;
    private double damageDealt;
    private double damageTaken;
    private int blocksPlaced;
    private int blocksCaught;

    public PlayerStats(int kills, int deaths, int blocksCaught, int blocksPlaced, double damageDealt, double damageTaken) {
        this.blocksCaught = blocksCaught;
        this.blocksPlaced = blocksPlaced;
        this.damageDealt = damageDealt;
        this.damageTaken = damageTaken;
        this.deaths = deaths;
        this.kills = kills;
    }

    public PlayerStats() {}

    // Getters and setters for each stat
    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void addKills(int kills) {
        this.kills += kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void addDeaths(int deaths) {
        this.deaths += deaths;
    }

    public double getDamageDealt() {
        return damageDealt;
    }

    public void setDamageDealt(double damageDealt) {
        this.damageDealt = damageDealt;
    }

    public void addDamageGiven(double damage) {
        this.damageDealt += damage;
    }

    public double getDamageTaken() {
        return damageTaken;
    }

    public void setDamageTaken(double damageTaken) {
        this.damageTaken = damageTaken;
    }

    public void addDamageTaken(double damage) {
        this.damageTaken += damage;
    }

    public int getBlocksPlaced() {
        return blocksPlaced;
    }

    public void setBlocksPlaced(int blocksPlaced) {
        this.blocksPlaced = blocksPlaced;
    }

    public void addBlocksPlaced(int blocks) {
        this.blocksPlaced += blocks;
    }

    public int getBlocksCaught() {
        return blocksCaught;
    }

    public void setBlocksCaught(int blocksCaught) {
        this.blocksCaught = blocksCaught;
    }

    public void addBlocksCaught(int blocks) {
        this.blocksCaught += blocks;
    }

    // Method to create a PlayerStats object from a Map
    public static PlayerStats fromMap(Map<String, Object> data) {
        PlayerStats stats = new PlayerStats();

        if (data.containsKey("kills")) {
            stats.setKills((Integer) data.get("kills"));
        }
        if (data.containsKey("deaths")) {
            stats.setDeaths((Integer) data.get("deaths"));
        }
        if (data.containsKey("damageGiven")) {
            stats.setDamageDealt((Double) data.get("damageGiven"));
        }
        if (data.containsKey("damageTaken")) {
            stats.setDamageTaken((Double) data.get("damageTaken"));
        }
        if (data.containsKey("blocksPlaced")) {
            stats.setBlocksPlaced((Integer) data.get("blocksPlaced"));
        }
        if (data.containsKey("blocksCaught")) {
            stats.setBlocksCaught((Integer) data.get("blocksCaught"));
        }

        return stats;
    }

    // Method to convert PlayerStats object to a Map
    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("kills", kills);
        data.put("deaths", deaths);
        data.put("damageGiven", damageDealt);
        data.put("damageTaken", damageTaken);
        data.put("blocksPlaced", blocksPlaced);
        data.put("blocksCaught", blocksCaught);
        return data;
    }
}