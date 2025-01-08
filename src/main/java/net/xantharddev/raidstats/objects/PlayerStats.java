package net.xantharddev.raidstats.objects;

import java.util.HashMap;
import java.util.UUID;

public class PlayerStats {
    private int kills;
    private int deaths;
    private double damageDealt;
    private int hitsDealt;
    private int hitsTaken;
    private double damageTaken;
    private int blocksPlaced;

    public PlayerStats(int kills, int deaths, int blocksPlaced, double damageDealt, double damageTaken, int hitsDealt, int hitsTaken) {
        this.blocksPlaced = blocksPlaced;
        this.damageDealt = damageDealt;
        this.damageTaken = damageTaken;
        this.deaths = deaths;
        this.kills = kills;
        this.hitsDealt = hitsDealt;
        this.hitsTaken = hitsTaken;
    }

    public PlayerStats() {}

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

    public int getDamageDealt() {
        return (int) damageDealt;
    }

    public void addDamageGiven(double damage) {
        this.damageDealt += damage;
        this.hitsDealt++;
    }

    public int getDamageTaken() {
        return (int) damageTaken;
    }

    public void addDamageTaken(double damage) {
        this.damageTaken += damage;
        this.hitsTaken++;
    }

    public int getHitsDealt() {
        return hitsDealt;
    }

    public int getHitsTaken() {
        return hitsTaken;
    }

    public int getBlocksPlaced() {
        return blocksPlaced;
    }

    public void addBlocksPlaced() {
        this.blocksPlaced++;
    }

    /*
    public boolean addBlocksCaught(List<Location> locations, Set<BlockLocation> blocksBlown) {
        boolean found = false;
        System.out.println(" ");
        System.out.println("----- START -----");

        // Print all blocks in blocksPlaced
        System.out.println("----- BLOCKS PLACED -----");
        for (BlockLocation block : blocksPlaced) {
            System.out.println(block);
        }

        // Print all incoming block locations
        System.out.println("----- INCOMING BLOCK LOCATIONS -----");
        for (Location location : locations) {
            System.out.println("Location: " + location + " -> BlockLocation: " + new BlockLocation(location));
        }

        System.out.println("----- BEGINNING BLOCKS MATCH CHECK -----");

        for (Location location : locations) {
            BlockLocation blockLocation = new BlockLocation(location);
            System.out.println("Checking Block Location: " + blockLocation);

            if (this.blocksPlaced.contains(blockLocation) && !blocksBlown.contains(blockLocation)) {
                System.out.println("Block matched and added to blocksBlown: " + blockLocation);
                blocksBlown.add(blockLocation);
                found = true;
            } else if (!this.blocksPlaced.contains(blockLocation)) {
                System.out.println("Block not found in blocksPlaced: " + blockLocation);
            } else if (blocksBlown.contains(blockLocation)) {
                System.out.println("Block already in blocksBlown: " + blockLocation);
            }
        }

        if (found) {
            this.blocksCaught++;
            System.out.println("Blocks Caught Incremented: " + this.blocksCaught);
        } else {
            System.out.println("No Blocks Caught Incremented.");
        }
        System.out.println("----- END -----");

        return found;
    }*/
}