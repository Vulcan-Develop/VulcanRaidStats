package net.xantharddev.raidstats.objects;

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
    BLOCKS_CAUGHT {
        @Override
        public int getValue(PlayerStats stats) {
            return stats.getBlocksCaught();
        }
    },
    DAMAGE_TAKEN {
        @Override
        public int getValue(PlayerStats stats) {
            return (int) stats.getDamageTaken(); // Cast to int for comparison
        }
    },
    DAMAGE_GIVEN {
        @Override
        public int getValue(PlayerStats stats) {
            return (int) stats.getDamageDealt(); // Cast to int for comparison
        }
    };

    public abstract int getValue(PlayerStats stats);
}