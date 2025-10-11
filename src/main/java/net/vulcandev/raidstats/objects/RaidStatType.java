package net.vulcandev.raidstats.objects;

/**
 * Enum representing different types of raid statistics that can be tracked.
 * Each type defines how to extract its value from a PlayerStats object.
 */
public enum RaidStatType {
    KILLS {
        @Override
        public int getValue(PlayerStats stats) {
            return stats.getKills();
        }
    },
    DEATHS {
        @Override
        public int getValue(PlayerStats stats) {
            return stats.getDeaths();
        }
    },
    BLOCKS_PLACED {
        @Override
        public int getValue(PlayerStats stats) {
            return stats.getBlocksPlaced();
        }
    },
    DAMAGE_TAKEN {
        @Override
        public int getValue(PlayerStats stats) {
            return (int) stats.getDamageTaken();
        }
    },
    DAMAGE_GIVEN {
        @Override
        public int getValue(PlayerStats stats) {
            return (int) stats.getDamageDealt();
        }
    },
    HITS_DEALT {
        @Override
        public int getValue(PlayerStats stats) {
            return stats.getHitsDealt();
        }
    },
    HITS_TAKEN {
        @Override
        public int getValue(PlayerStats stats) {
            return stats.getHitsTaken();
        }
    };

    /**
     * Extracts the stat value from a PlayerStats object.
     *
     * @param stats The player stats to extract from
     * @return The numeric value for this stat type
     */
    public abstract int getValue(PlayerStats stats);
}