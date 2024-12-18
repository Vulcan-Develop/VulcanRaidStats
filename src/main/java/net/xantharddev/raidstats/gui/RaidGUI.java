package net.xantharddev.raidstats.gui;

import com.massivecraft.factions.Factions;
import net.xantharddev.raidstats.RaidStats;
import net.xantharddev.raidstats.objects.Colour;
import net.xantharddev.raidstats.objects.PlayerStats;
import net.xantharddev.raidstats.objects.RaidObject;
import net.xantharddev.raidstats.objects.RaidStatType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;

public class RaidGUI extends GUI<Integer> {
    private final RaidStats plugin;
    private final RaidObject raidObject;

    private final String title;
    private final int size;
    private final int closeSlot;
    private final SimpleItem closeItem;
    private final SimpleItem fillerItem;

    private static final int CLOSE_INDEX = 5;

    public RaidGUI(RaidStats plugin, RaidObject raid, Player user) {
        super(user, plugin.getConfig().getInt("gui.size", 3));
        this.plugin = plugin;
        this.raidObject = raid;

        FileConfiguration config = plugin.getConfig();

        // GUI Title
        this.title = Colour.colour(config.getString("gui.title", "&7{faction_name}'s Raid Stats")
                .replace("{raiding_name}", Factions.getInstance().getFactionById(raidObject.getRaidingFaction()).getTag())
                .replace("{defending_name}", Factions.getInstance().getFactionById(raidObject.getDefendingFaction()).getTag())
        );

        // Close Button Configuration
        this.closeSlot = config.getInt("gui.close.slot", 22);
        // Fetch and build the close item
        String closeName = config.getString("gui.close.name", "&cClose");
        Material closeMaterial = Material.valueOf(config.getString("gui.close.material", "SKULL_ITEM"));
        byte closeDamage = (byte) config.getInt("gui.close.damage", 0);
        String closeUrl = config.getString("gui.close.url");

        // Create the close item
        this.closeItem = SimpleItem.builder()
                .setName(closeName)
                .setMaterial(closeMaterial)
                .setDamage(closeDamage)
                .setUrl(closeUrl)
                .build();

        // Filler Item Configuration
        this.fillerItem = SimpleItem.builder()
                .setName(config.getString("gui.filler.name", "&7"))
                .setMaterial(Material.valueOf(config.getString("gui.filler.material", "STAINED_GLASS_PANE")))
                .setDamage((byte) config.getInt("gui.filler.damage", 7))
                .build();

        this.size = config.getInt("gui.size", 3);

        build();
    }

    @Override
    protected String getName() {
        return title;
    }

    @Override
    protected String parse(String toParse, Integer index) {
        return toParse;
    }

    @Override
    protected void onClick(Integer index, ClickType clickType) {
        if (index == CLOSE_INDEX) user.closeInventory();
    }

    @Override
    protected Map<Integer, Integer> createSlotMap() {
        Map<Integer, Integer> slotMap = new HashMap<>();
        slotMap.put(closeSlot, CLOSE_INDEX); // Close Menu
        return slotMap;
    }

    @Override
    protected SimpleItem getItem(Integer index) {
        if (index == CLOSE_INDEX) return closeItem;
        return SimpleItem.builder().build();
    }

    @Override
    protected Map<Integer, SimpleItem> createDummyItems() {
        Map<Integer, SimpleItem> dummyItems = new HashMap<>();

        FileConfiguration config = plugin.getConfig();

        for (String statKey : config.getConfigurationSection("gui.stats").getKeys(false)) {
            RaidStatType statType = statKey.equalsIgnoreCase("overall") ? null : RaidStatType.valueOf(statKey.toUpperCase());
            int slot = config.getInt("gui.stats." + statKey + ".slot");

            Material material = Material.valueOf(config.getString("gui.stats." + statKey + ".material", "STONE").toUpperCase());
            byte damage = (byte) config.getInt("gui.stats." + statKey + ".damage", 0);
            String name = config.getString("gui.stats." + statKey + ".name", "&7Unknown Stat");
            List<String> lore = config.getStringList("gui.stats." + statKey + ".lore");
            String url = config.getString("gui.stats." + statKey + ".url", "");

            List<String> updatedLore = new ArrayList<>();
            if (statType == null) {
                // Replace placeholders with the faction stats for overall
                updatedLore = replaceOverallPlaceholders(lore);
            } else {
                String rankFormat = config.getString("gui.stats." + statType.name().toLowerCase() + ".rank");

                Map<UUID, PlayerStats> topStats = new HashMap<>();
                Map<UUID, PlayerStats> defendingTopStats = raidObject.getTopStats(raidObject.getDefendingFaction(), statType, 7);

                if (statType != RaidStatType.BLOCKS_PLACED) {
                    topStats = raidObject.getTopStats(raidObject.getRaidingFaction(), statType, 7);
                }

                for (String line : lore) {
                    if (line.contains("{raiding_ranks}")) {
                        List<String> raidingRanks = generateRankLines(topStats, statType, rankFormat, config.getString("gui.raidColour", "&c"));
                        if (!raidingRanks.isEmpty()) {
                            updatedLore.addAll(raidingRanks); // Add all rank lines
                        }
                    } else if (line.contains("{defending_ranks}")) {
                        List<String> defendingRanks = generateRankLines(defendingTopStats, statType, rankFormat, config.getString("gui.defendColour", "&d"));
                        if (!defendingRanks.isEmpty()) {
                            updatedLore.addAll(defendingRanks); // Add all rank lines
                        }
                    } else {
                        updatedLore.add(Colour.colour(line)); // Add other lines unchanged
                    }
                }
            }

            SimpleItem statItem = SimpleItem.builder()
                    .setName(name)
                    .setMaterial(material)
                    .setDamage(damage)
                    .setLore(updatedLore)
                    .setUrl(url)
                    .build();

            dummyItems.put(slot, statItem);
        }

        for (int i = 0; i < size * 9; i++) {
            dummyItems.putIfAbsent(i, fillerItem);
        }

        return dummyItems;
    }

    public List<String> replaceOverallPlaceholders(List<String> configLines) {
        // Get the totals for the specified faction
        Map<RaidStatType, Integer> defendingTotals = raidObject.getFactionTotals(raidObject.getDefendingFaction());
        Map<RaidStatType, Integer> raidingTotals = raidObject.getFactionTotals(raidObject.getRaidingFaction());

        // Get the hit stats for both factions
        int defendingHitsDealt = defendingTotals.getOrDefault(RaidStatType.HITS_DEALT, 0);
        int defendingHitsTaken = defendingTotals.getOrDefault(RaidStatType.HITS_TAKEN, 0);
        int raidingHitsDealt = raidingTotals.getOrDefault(RaidStatType.HITS_DEALT, 0);
        int raidingHitsTaken = raidingTotals.getOrDefault(RaidStatType.HITS_TAKEN, 0);

        // Calculate hearts (damage divided by 2 as integers)
        int defendingDamageDealtHearts = defendingTotals.getOrDefault(RaidStatType.DAMAGE_GIVEN, 0) / 2;
        int defendingDamageTakenHearts = defendingTotals.getOrDefault(RaidStatType.DAMAGE_TAKEN, 0) / 2;
        int raidingDamageDealtHearts = raidingTotals.getOrDefault(RaidStatType.DAMAGE_GIVEN, 0) / 2;
        int raidingDamageTakenHearts = raidingTotals.getOrDefault(RaidStatType.DAMAGE_TAKEN, 0) / 2;

        // Iterate over the config lines and replace placeholders with actual values
        List<String> updatedLines = new ArrayList<>();
        for (String line : configLines) {
            // Replace placeholders for defending faction
            line = line.replace("{defending_kills}", String.format("%,d", defendingTotals.getOrDefault(RaidStatType.KILLS, 0)));
            line = line.replace("{defending_deaths}", String.format("%,d", defendingTotals.getOrDefault(RaidStatType.DEATHS, 0)));
            line = line.replace("{defending_blocks_placed}", String.format("%,d", defendingTotals.getOrDefault(RaidStatType.BLOCKS_PLACED, 0)));

            // Replace damage-related placeholders, defaulting to 0 if not available
            line = line.replace("{defending_damage_dealt}", String.format("%,d", defendingTotals.getOrDefault(RaidStatType.DAMAGE_GIVEN, 0)));
            line = line.replace("{defending_damage_taken}", String.format("%,d", defendingTotals.getOrDefault(RaidStatType.DAMAGE_TAKEN, 0)));
            line = line.replace("{defending_damage_dealt_hits}", String.format("%,d", defendingHitsDealt));
            line = line.replace("{defending_damage_dealt_hearts}", String.format("%,d", defendingDamageDealtHearts));
            line = line.replace("{defending_damage_taken_hits}", String.format("%,d", defendingHitsTaken));
            line = line.replace("{defending_damage_taken_hearts}", String.format("%,d", defendingDamageTakenHearts));

            // Replace placeholders for raiding faction
            line = line.replace("{attacking_kills}", String.format("%,d", raidingTotals.getOrDefault(RaidStatType.KILLS, 0)));
            line = line.replace("{attacking_deaths}", String.format("%,d", raidingTotals.getOrDefault(RaidStatType.DEATHS, 0)));
            line = line.replace("{attacking_blocks_placed}", String.format("%,d", raidingTotals.getOrDefault(RaidStatType.BLOCKS_PLACED, 0)));

            // Replace damage-related placeholders, defaulting to 0 if not available
            line = line.replace("{attacking_damage_dealt}", String.format("%,d", raidingTotals.getOrDefault(RaidStatType.DAMAGE_GIVEN, 0)));
            line = line.replace("{attacking_damage_taken}", String.format("%,d", raidingTotals.getOrDefault(RaidStatType.DAMAGE_TAKEN, 0)));
            line = line.replace("{attacking_damage_dealt_hits}", String.format("%,d", raidingHitsDealt));
            line = line.replace("{attacking_damage_dealt_hearts}", String.format("%,d", raidingDamageDealtHearts));
            line = line.replace("{attacking_damage_taken_hits}", String.format("%,d", raidingHitsTaken));
            line = line.replace("{attacking_damage_taken_hearts}", String.format("%,d", raidingDamageTakenHearts));

            // Add line to updated list if it contains any replacements
            updatedLines.add(line);
        }

        return updatedLines;
    }

    private List<String> generateRankLines(Map<UUID, PlayerStats> stats, RaidStatType statType, String rankFormat, String color) {
        List<String> ranks = new ArrayList<>();
        int rank = 1;

        if (stats.isEmpty()) {
            return new ArrayList<>();
        }

        for (Map.Entry<UUID, PlayerStats> entryStat : stats.entrySet()) {
            String playerName = Bukkit.getServer().getOfflinePlayer(entryStat.getKey()).getName();
            int statValue = statType.getValue(entryStat.getValue());

            // Start with a fresh copy of rankFormat for this rank
            String currentRankFormat = rankFormat;

            // If the stat type is DAMAGE_GIVEN or DAMAGE_TAKEN, convert the value to hearts
            if (statType == RaidStatType.DAMAGE_GIVEN || statType == RaidStatType.DAMAGE_TAKEN) {
                // Convert stat value to hearts and format
                String heartsString = String.format("%.1f", statValue / 2.0);
                currentRankFormat = currentRankFormat.replace("{hearts}", heartsString);

                // Replace hits based on the stat type
                int hits = statType == RaidStatType.DAMAGE_GIVEN ? entryStat.getValue().getHitsDealt() : entryStat.getValue().getHitsTaken();
                currentRankFormat = currentRankFormat.replace("{hits}", String.format("%,d", hits));
            }

            // Replace placeholders in the rank format
            String formattedRank = currentRankFormat
                    .replace("{rank}", String.format("%,d", rank))
                    .replace("{player_name}", playerName)
                    .replace("{stat_value}", String.format("%,d", statValue))
                    .replace("{color}", color);

            ranks.add(formattedRank); // Add new line after each rank
            rank++;
        }

        return ranks;
    }
}