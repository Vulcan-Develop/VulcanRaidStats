package net.xantharddev.raidstats.manager;

import net.xantharddev.raidstats.objects.RaidObject;

import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {
    private final List<RaidObject> raids;

    public StatsManager() {
        this.raids = new ArrayList<>();
    }

    /**
     * Adds a new raid to the manager.
     *
     * @param raid The RaidObject object to add.
     */
    public void addRaid(RaidObject raid) {
        raids.add(raid);
    }

    /**
     * Retrieves a raid by its unique UUID.
     *
     * @param uuid The UUID of the raid to find.
     * @return The corresponding RaidObject, or null if not found.
     */
    public RaidObject getRaidByUUID(UUID uuid) {
        return raids.stream().filter(r -> r.getId().equals(uuid)).findFirst().orElse(null);
    }

    /**
     * Retrieves the raid where a specific faction is defending.
     *
     * @param factionId The ID of the defending faction.
     * @return The corresponding RaidObject, or null if not found.
     */
    public RaidObject getRaidDefendingByFacID(String factionId) {
        return raids.stream()
                .filter(raid -> raid.getDefendingFaction().equals(factionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves all raids involving a specific faction as either raiding or defending.
     *
     * @param factionId The ID of the faction.
     * @return A list of RaidObject objects that involve the specified faction.
     */
    public List<RaidObject> getRaidsByFacID(String factionId) {
        return raids.stream()
                .filter(raid -> raid.getRaidingFaction().equals(factionId) || raid.getDefendingFaction().equals(factionId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific raid by two faction IDs.
     *
     * @param faction1 The ID of the first faction (either raiding or defending).
     * @param faction2 The ID of the second faction (either raiding or defending).
     * @return The corresponding RaidObject, or null if not found.
     */
    public RaidObject getRaidByFactionIds(String faction1, String faction2) {
        return raids.stream()
                .filter(raid -> (raid.getRaidingFaction().equals(faction1) && raid.getDefendingFaction().equals(faction2)) ||
                        (raid.getRaidingFaction().equals(faction2) && raid.getDefendingFaction().equals(faction1)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Removes a raid from the manager based on raiding and defending faction IDs.
     *
     * @param raidingFaction  The ID of the raiding faction.
     * @param defendingFaction The ID of the defending faction.
     */
    public void removeRaid(String raidingFaction, String defendingFaction) {
        raids.removeIf(raid -> raid.getRaidingFaction().equals(raidingFaction) &&
                raid.getDefendingFaction().equals(defendingFaction));
    }

    /**
     * Retrieves all ongoing raids.
     *
     * @return A list of all RaidObject objects.
     */
    public List<RaidObject> getAllRaids() {
        return new ArrayList<>(raids);
    }

    /**
     * Clears all ongoing raids from the manager.
     */
    public void clearAllRaids() {
        raids.clear();
    }
}