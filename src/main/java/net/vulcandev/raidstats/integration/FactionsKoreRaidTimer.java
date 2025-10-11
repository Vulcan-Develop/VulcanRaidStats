package net.vulcandev.raidstats.integration;

import com.golfing8.kore.feature.RaidClaimFeature;
import com.golfing8.kore.feature.RaidClaimFeature.Pair;
import com.golfing8.kore.object.Raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration wrapper for FactionsKore's raid timing features.
 * Provides access to active raids and grace period information.
 */
public class FactionsKoreRaidTimer {
    private final RaidClaimFeature raidClaimFeature;

    public FactionsKoreRaidTimer(RaidClaimFeature raidClaimFeature) {
        this.raidClaimFeature = raidClaimFeature;
    }

    /**
     * Gets the remaining grace period time for a faction.
     *
     * @param factionId The faction ID to check
     * @return Grace time remaining in minutes, or -1 if no grace period
     */
    public Integer getGrace(String factionId) {
        if (raidClaimFeature == null) { return -1; }
        return raidClaimFeature.getGrace().getOrDefault(factionId, new RaidClaimFeature.Pair<>("N/A", -1)).getB();
    }

    /**
     * Gets all active grace periods.
     *
     * @return Map of faction ID to grace period details (raiding faction ID, time remaining)
     */
    public Map<String, Pair<String, Integer>> getGracePeriods() { return new HashMap<>(raidClaimFeature.getGrace()); }

    /**
     * Gets all currently active raids from FactionsKore.
     *
     * @return List of active Raid objects
     */
    public List<Raid> getActiveRaids() { return new ArrayList<>(raidClaimFeature.getActiveRaids()); }
}