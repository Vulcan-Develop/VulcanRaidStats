package net.vulcandev.raidstats.gui;

import com.massivecraft.factions.Factions;
import me.plugin.libs.YamlDocument;
import net.vulcandev.raidstats.objects.PlayerStats;
import net.vulcandev.raidstats.objects.VulcanRaidStats;
import net.vulcandev.raidstats.objects.RaidStatType;
import net.xantharddev.vulcanlib.libs.Colour;
import net.xantharddev.vulcanlib.libs.GUI;
import net.xantharddev.vulcanlib.libs.SimpleItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;

/**
 * Interactive GUI for displaying raid statistics.
 * Shows overall stats, top players by category, and faction-specific information.
 * All display content is configurable via config.yml.
 */
public class RaidGUI extends GUI<Integer> {
    private static final int CLOSE_INDEX = 5;
    private static final int TOP_PLAYERS_LIMIT = 7;
    private static final int SLOTS_PER_ROW = 9;
    private static final int HEARTS_DIVISOR = 2;

    private final net.vulcandev.raidstats.VulcanRaidStats plugin;
    private final VulcanRaidStats vulcanRaidStats;
    private final String title;
    private final int size;
    private final int closeSlot;
    private final SimpleItem closeItem;
    private final SimpleItem fillerItem;

    public RaidGUI(net.vulcandev.raidstats.VulcanRaidStats plugin, VulcanRaidStats raid, Player user) {
        super(user, plugin.conf().getInt("gui.size", 3));
        this.plugin = plugin;
        this.vulcanRaidStats = raid;

        YamlDocument config = plugin.conf();

        // GUI Title
        this.title = Colour.colour(config.getString("gui.title", "&7{faction_name}'s Raid Stats")
                .replace("{raiding_name}", Factions.getInstance().getFactionById(vulcanRaidStats.getRaidingFaction()).getTag())
                .replace("{defending_name}", Factions.getInstance().getFactionById(vulcanRaidStats.getDefendingFaction()).getTag())
        );

        // Close Button Configuration
        this.closeSlot = config.getInt("gui.close.slot", 22);
        // Fetch and build the close item
        String closeName = config.getString("gui.close.name", "&cClose");
        Material closeMaterial = Material.valueOf(config.getString("gui.close.material", "SKULL_ITEM"));
        byte closeDamage = config.getByte("gui.close.damage");
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
                .setDamage(config.getByte("gui.filler.damage"))
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
        slotMap.put(closeSlot, CLOSE_INDEX);
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
        YamlDocument conf = plugin.conf();

        for (Object key : conf.getSection("gui.stats").getKeys(false)) {
            String statKey = (String) key;
            RaidStatType statType = statKey.equalsIgnoreCase("overall") ? null : RaidStatType.valueOf(statKey.toUpperCase());

            SimpleItem statItem = createStatItem(conf, statKey, statType);
            int slot = conf.getInt("gui.stats." + statKey + ".slot");
            dummyItems.put(slot, statItem);
        }

        // Fill empty slots with filler items
        for (int i = 0; i < size * SLOTS_PER_ROW; i++) dummyItems.putIfAbsent(i, fillerItem);

        return dummyItems;
    }

    /**
     * Creates a stat display item with appropriate lore based on stat type.
     */
    private SimpleItem createStatItem(YamlDocument conf, String statKey, RaidStatType statType) {
        String basePath = "gui.stats." + statKey;

        Material material = Material.valueOf(conf.getString(basePath + ".material", "STONE").toUpperCase());
        byte damage = conf.getByte(basePath + ".damage");
        String name = conf.getString(basePath + ".name", "&7Unknown Stat");
        List<String> lore = conf.getStringList(basePath + ".lore");
        String url = conf.getString(basePath + ".url", "");

        List<String> processedLore = statType == null
            ? replaceOverallPlaceholders(lore)
            : generateRankingLore(conf, statType, lore);

        return SimpleItem.builder()
                .setName(name)
                .setMaterial(material)
                .setDamage(damage)
                .setLore(processedLore)
                .setUrl(url)
                .build();
    }

    /**
     * Generates lore with player rankings for a specific stat type.
     */
    private List<String> generateRankingLore(YamlDocument conf, RaidStatType statType, List<String> lore) {
        List<String> updatedLore = new ArrayList<>();
        String rankFormat = conf.getString("gui.stats." + statType.name().toLowerCase() + ".rank");

        Map<UUID, PlayerStats> defendingTopStats = vulcanRaidStats.getTopStats(
            vulcanRaidStats.getDefendingFaction(), statType, TOP_PLAYERS_LIMIT);
        Map<UUID, PlayerStats> raidingTopStats = statType != RaidStatType.BLOCKS_PLACED
            ? vulcanRaidStats.getTopStats(vulcanRaidStats.getRaidingFaction(), statType, TOP_PLAYERS_LIMIT)
            : Collections.emptyMap();

        String raidColor = conf.getString("gui.raidColour", "&c");
        String defendColor = conf.getString("gui.defendColour", "&d");

        for (String line : lore) {
            if (line.contains("{raiding_ranks}")) {
                updatedLore.addAll(generateRankLines(raidingTopStats, statType, rankFormat, raidColor));
            } else if (line.contains("{defending_ranks}")) {
                updatedLore.addAll(generateRankLines(defendingTopStats, statType, rankFormat, defendColor));
            } else {
                updatedLore.add(Colour.colour(line));
            }
        }

        return updatedLore;
    }

    /**
     * Replaces placeholder text in overall stats display with actual values.
     * Handles both defending and attacking faction stats including kills, deaths, damage, and blocks.
     */
    public List<String> replaceOverallPlaceholders(List<String> configLines) {
        Map<RaidStatType, Integer> defendingTotals = vulcanRaidStats.getFactionTotals(vulcanRaidStats.getDefendingFaction());
        Map<RaidStatType, Integer> raidingTotals = vulcanRaidStats.getFactionTotals(vulcanRaidStats.getRaidingFaction());

        List<String> updatedLines = new ArrayList<>();
        for (String line : configLines) {
            line = replaceFactionPlaceholders(line, "defending", defendingTotals);
            line = replaceFactionPlaceholders(line, "attacking", raidingTotals);
            updatedLines.add(line);
        }

        return updatedLines;
    }

    /**
     * Replaces stat placeholders for a single faction in a line.
     */
    private String replaceFactionPlaceholders(String line, String prefix, Map<RaidStatType, Integer> totals) {
        // Basic stats
        line = line.replace("{" + prefix + "_kills}", formatStat(totals.getOrDefault(RaidStatType.KILLS, 0)));
        line = line.replace("{" + prefix + "_deaths}", formatStat(totals.getOrDefault(RaidStatType.DEATHS, 0)));
        line = line.replace("{" + prefix + "_blocks_placed}", formatStat(totals.getOrDefault(RaidStatType.BLOCKS_PLACED, 0)));

        // Damage stats
        int damageDealt = totals.getOrDefault(RaidStatType.DAMAGE_GIVEN, 0);
        int damageTaken = totals.getOrDefault(RaidStatType.DAMAGE_TAKEN, 0);

        line = line.replace("{" + prefix + "_damage_dealt}", formatStat(damageDealt));
        line = line.replace("{" + prefix + "_damage_taken}", formatStat(damageTaken));
        line = line.replace("{" + prefix + "_damage_dealt_hearts}", formatStat(damageDealt / HEARTS_DIVISOR));
        line = line.replace("{" + prefix + "_damage_taken_hearts}", formatStat(damageTaken / HEARTS_DIVISOR));

        // Hit stats
        line = line.replace("{" + prefix + "_damage_dealt_hits}", formatStat(totals.getOrDefault(RaidStatType.HITS_DEALT, 0)));
        line = line.replace("{" + prefix + "_damage_taken_hits}", formatStat(totals.getOrDefault(RaidStatType.HITS_TAKEN, 0)));

        return line;
    }

    /**
     * Formats a numeric stat with thousand separators.
     */
    private String formatStat(int value) {return String.format("%,d", value);}

    /**
     * Generates formatted ranking lines for top players in a specific stat category.
     * Converts damage values to hearts and applies faction colors.
     */
    private List<String> generateRankLines(Map<UUID, PlayerStats> stats, RaidStatType statType, String rankFormat, String color) {
        if (stats.isEmpty()) return Collections.emptyList();

        List<String> ranks = new ArrayList<>();
        int rank = 1;

        for (Map.Entry<UUID, PlayerStats> entryStat : stats.entrySet()) {
            String playerName = Bukkit.getOfflinePlayer(entryStat.getKey()).getName();
            PlayerStats playerStats = entryStat.getValue();
            int statValue = statType.getValue(playerStats);

            String currentRankFormat = rankFormat;

            // Process damage specific placeholders
            if (isDamageStat(statType)) {
                currentRankFormat = replaceDmgPlaceholders(currentRankFormat, statValue, playerStats, statType);
            }

            // Replace common placeholders
            String formattedRank = currentRankFormat
                    .replace("{rank}", formatStat(rank))
                    .replace("{player_name}", playerName)
                    .replace("{stat_value}", formatStat(statValue))
                    .replace("{color}", color);

            ranks.add(formattedRank);
            rank++;
        }

        return ranks;
    }

    /**
     * Checks if the stat type is damage-related.
     */
    private boolean isDamageStat(RaidStatType statType) {
        return statType == RaidStatType.DAMAGE_GIVEN || statType == RaidStatType.DAMAGE_TAKEN;
    }

    /**
     * Replaces damage-specific placeholders (hearts and hits) in rank format.
     */
    private String replaceDmgPlaceholders(String format, int statValue, PlayerStats stats, RaidStatType statType) {
        String heartsString = String.format("%,.0f", statValue / (double) HEARTS_DIVISOR);
        format = format.replace("{hearts}", heartsString);

        int hits = statType == RaidStatType.DAMAGE_GIVEN ? stats.getHitsDealt() : stats.getHitsTaken();
        format = format.replace("{hits}", formatStat(hits));

        return format;
    }
}