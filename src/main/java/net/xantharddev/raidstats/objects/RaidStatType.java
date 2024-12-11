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
            return stats.getBlocksPlacedAmount();
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
            return (int) stats.getDamageTaken();
        }
    },
    DAMAGE_GIVEN {
        @Override
        public int getValue(PlayerStats stats) {
            return (int) stats.getDamageDealt();
        }
    };

    public abstract int getValue(PlayerStats stats);
}