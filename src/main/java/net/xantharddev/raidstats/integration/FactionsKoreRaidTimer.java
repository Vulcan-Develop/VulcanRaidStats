package net.xantharddev.raidstats.integration;

import com.golfing8.kore.feature.RaidClaimFeature;
import com.golfing8.kore.feature.RaidClaimFeature.Pair;
import com.golfing8.kore.object.Raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FactionsKoreRaidTimer {
    private final RaidClaimFeature raidClaimFeature;

    public FactionsKoreRaidTimer(RaidClaimFeature raidClaimFeature) {
        this.raidClaimFeature = raidClaimFeature;
    }

    public Integer getGrace(String factionId) {
        if (raidClaimFeature == null) { return -1; }
        return raidClaimFeature.getGrace().getOrDefault(factionId, new RaidClaimFeature.Pair<>("N/A", -1)).getB();
    }

    public Map<String, Pair<String, Integer>> getGracePeriods() { return new HashMap<>(raidClaimFeature.getGrace()); }

    public List<Raid> getActiveRaids() { return new ArrayList<>(raidClaimFeature.getActiveRaids()); }
}