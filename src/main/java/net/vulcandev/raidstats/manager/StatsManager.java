package net.vulcandev.raidstats.manager;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import net.vulcandev.raidstats.objects.RaidStats;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages all active raid tracking objects.
 * Handles raid creation, lookup, removal, and synchronization with FactionsKore.
 */
public class StatsManager {
    private final net.vulcandev.raidstats.VulcanRaidStats plugin;
    private final List<RaidStats> raids;

    public StatsManager(net.vulcandev.raidstats.VulcanRaidStats plugin) {
        this.plugin = plugin;
        this.raids = new ArrayList<>();
        syncRaids();
    }

    /**
     * Periodically syncs raid objects with FactionsKore's active raids.
     * Ensures our raid data stays in sync with the external raid system.
     */
    private void syncRaids() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if(plugin.getRaidTimer() == null || plugin.getRaidTimer().getActiveRaids() == null) return;
            Map<String, RaidStats> raidMap = raids.stream()
                    .collect(Collectors.toMap(
                            raidObject -> generateKey(raidObject.getDefendingFaction(), raidObject.getRaidingFaction()),
                            raidObject -> raidObject
                    ));

            plugin.getRaidTimer().getActiveRaids().forEach(koreRaid -> {
                String key = generateKey(koreRaid.getRaided(), koreRaid.getFaction());
                RaidStats raidStats = raidMap.get(key);
                if (raidStats != null) {
                    raidStats.setKoreRaid(koreRaid);
                }
            });
        },600L, 600L);
    }

    /**
     * Generates a unique key for a raid based on defending and raiding faction IDs.
     */
    private String generateKey(String defendingFaction, String raidingFaction) {
        return defendingFaction + "::" + raidingFaction;
    }

    /**
     * Adds a new raid to the manager.
     *
     * @param raid The RaidStats object to add.
     */
    public void addRaid(RaidStats raid) {
        raids.add(raid);
    }

    /**
     * Retrieves a raid by its unique UUID.
     *
     * @param uuid The UUID of the raid to find.
     * @return The corresponding RaidStats, or null if not found.
     */
    public RaidStats getRaidByUUID(UUID uuid) {
        return raids.stream().filter(r -> r.getId().equals(uuid)).findFirst().orElse(null);
    }

    /**
     * Retrieves the raid where a specific faction is defending.
     *
     * @param factionId The ID of the defending faction.
     * @return The corresponding RaidStats, or null if not found.
     */
    public RaidStats getRaidDefendingByFacID(String factionId) {
        return raids.stream()
                .filter(raid -> isDefendingFaction(raid, factionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a faction owns the raiding outpost.
     */
    public boolean doesFactionOwnRaidingOutpost(String factionId) {
        if (plugin.getRaidingOutpost() == null || plugin.getRaidingOutpost().getOutpost() == null || plugin.getRaidingOutpost().getOutpost().getOwner() == null) return false;
        return plugin.getRaidingOutpost().getOutpost().getOwner().equals(factionId);
    }

    /**
     * Checks if a location is within the raiding outpost territory.
     */
    public boolean isLocInRPost(Location loc) {
        if (plugin.getRaidingOutpost() == null || plugin.getRaidingOutpost().getOutpost() == null || plugin.getRaidingOutpost().getOutpost().getOwner() == null) return false;
        Faction fac = Board.getInstance().getFactionAt(new FLocation(loc));
        return fac != null && fac.getTag().equals("RaidOutpost");
    }

    /**
     * Determines if a faction is defending in a raid (either as the defending faction or outpost owner).
     */
    private boolean isDefendingFaction(RaidStats raid, String factionId) {
        return raid.getDefendingFaction().equals(factionId) || doesFactionOwnRaidingOutpost(factionId);
    }

    /**
     * Retrieves a specific raid by two faction IDs.
     *
     * @param faction1 The ID of the first faction (either raiding or defending).
     * @param faction2 The ID of the second faction (either raiding or defending).
     * @return The corresponding RaidStats, or null if not found.
     */
    public List<RaidStats> getRaidsByFactionIds(String faction1, String faction2) {
        return raids.stream()
                .filter(raid -> (raid.getRaidingFaction().equals(faction1) && isDefendingFaction(raid, faction2)) ||
                        (raid.getRaidingFaction().equals(faction2) && isDefendingFaction(raid, faction1)))
                .collect(Collectors.toList());
    }

    /**
     * Removes a raid from the manager based on raiding and defending faction IDs.
     *
     * @param raidingFaction  The ID of the raiding faction.
     * @param defendingFaction The ID of the defending faction.
     */
    public void removeRaid(String raidingFaction, String defendingFaction) {
        raids.removeIf(raid -> raid.getRaidingFaction().equals(raidingFaction) &&
                isDefendingFaction(raid, defendingFaction));
    }

    /**
     * Retrieves all ongoing raids.
     *
     * @return A list of all RaidStats objects.
     */
    public List<RaidStats> getAllRaids() {
        return new ArrayList<>(raids);
    }

    /**
     * Clears all ongoing raids from the manager.
     */
    public void clearAllRaids() {
        raids.clear();
    }
}