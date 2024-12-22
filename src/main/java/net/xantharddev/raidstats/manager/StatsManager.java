package net.xantharddev.raidstats.manager;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import net.xantharddev.raidstats.RaidStats;
import net.xantharddev.raidstats.objects.RaidObject;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {
    private final RaidStats plugin;
    private final List<RaidObject> raids;

    public StatsManager(RaidStats plugin) {
        this.plugin = plugin;
        this.raids = new ArrayList<>();
        syncRaids();
    }

    private void syncRaids() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Map<String, RaidObject> raidMap = raids.stream()
                    .collect(Collectors.toMap(
                            raidObject -> generateKey(raidObject.getDefendingFaction(), raidObject.getRaidingFaction()),
                            raidObject -> raidObject
                    ));

            plugin.getRaidTimer().getActiveRaids().forEach(koreRaid -> {
                String key = generateKey(koreRaid.getRaided(), koreRaid.getFaction());
                RaidObject raidObject = raidMap.get(key);
                if (raidObject != null) {
                    raidObject.setKoreRaid(koreRaid);
                }
            });
        },0L, 600L);
    }

    private String generateKey(String defendingFaction, String raidingFaction) {
        return defendingFaction + "::" + raidingFaction;
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
                .filter(raid -> isDefendingFaction(raid, factionId))
                .findFirst()
                .orElse(null);
    }

    public boolean doesFactionOwnRaidingOutpost(String factionId) {
        if (plugin.getRaidingOutpost() == null || plugin.getRaidingOutpost().getOutpost() == null || plugin.getRaidingOutpost().getOutpost().getOwner() == null) return false;
        return plugin.getRaidingOutpost().getOutpost().getOwner().equals(factionId);
    }

    public boolean isLocInRPost(Location loc) {
        if (plugin.getRaidingOutpost() == null || plugin.getRaidingOutpost().getOutpost() == null || plugin.getRaidingOutpost().getOutpost().getOwner() == null) return false;
        Faction fac = Board.getInstance().getFactionAt(new FLocation(loc));
        return fac != null && fac.getTag().equals("RaidOutpost");
    }

    private boolean isDefendingFaction(RaidObject raid, String factionId) {
        return raid.getDefendingFaction().equals(factionId) || doesFactionOwnRaidingOutpost(factionId);
    }

    /**
     * Retrieves a specific raid by two faction IDs.
     *
     * @param faction1 The ID of the first faction (either raiding or defending).
     * @param faction2 The ID of the second faction (either raiding or defending).
     * @return The corresponding RaidObject, or null if not found.
     */
    public List<RaidObject> getRaidsByFactionIds(String faction1, String faction2) {
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