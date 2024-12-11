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
        this.closeItem = SimpleItem.builder()
                .setName(config.getString("gui.close.name", "&cClose"))
                .setMaterial(Material.valueOf(config.getString("gui.close.material", "SKULL_ITEM")))
                .setDamage((byte) config.getInt("gui.close.damage", 0))
                .setUrl(config.getString("gui.close.url"))
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
        if (index == closeSlot) {
            return closeItem;
        }
        return SimpleItem.builder().build();
    }

    @Override
    protected Map<Integer, SimpleItem> createDummyItems() {
        Map<Integer, SimpleItem> dummyItems = new HashMap<>();

        FileConfiguration config = plugin.getConfig();

        for (String statKey : config.getConfigurationSection("gui.stats").getKeys(false)) {
            RaidStatType statType = RaidStatType.valueOf(statKey.toUpperCase());
            int slot = config.getInt("gui.stats." + statKey + ".slot");

            Material material = Material.valueOf(config.getString("gui.stats." + statKey + ".material", "STONE").toUpperCase());
            byte damage = (byte) config.getInt("gui.stats." + statKey + ".damage", 0);
            String name = config.getString("gui.stats." + statKey + ".name", "&7Unknown Stat");
            List<String> lore = config.getStringList("gui.stats." + statKey + ".lore");
            String url = config.getString("gui.stats." + statKey + ".url", "");
            String rankFormat = config.getString("gui.stats." + statType.name().toLowerCase() + ".rank");

            Map<UUID, PlayerStats> topStats = new HashMap<>();
            Map<UUID, PlayerStats> defendingTopStats = raidObject.getTopStats(raidObject.getDefendingFaction(), statType, 7);

            if (statType != RaidStatType.BLOCKS_PLACED && statType != RaidStatType.BLOCKS_CAUGHT) {
                topStats = raidObject.getTopStats(raidObject.getRaidingFaction(), statType, 7);
            }

            List<String> updatedLore = new ArrayList<>();
            for (String line : lore) {
                if (line.contains("{raiding_ranks}") && !generateRankLines(topStats, statType, rankFormat, config.getString("gui.raidColour", "&c")).isEmpty()) {
                    line = line.replace("{raiding_ranks}", generateRankLines(topStats, statType, rankFormat, config.getString("gui.raidColour", "&c")));
                } else {
                    line = line.replace("{raiding_ranks}", "");
                }

                if (line.contains("{defending_ranks}") && !generateRankLines(defendingTopStats, statType, rankFormat, config.getString("gui.defendColour", "&d")).isEmpty()) {
                    line = line.replace("{defending_ranks}", generateRankLines(defendingTopStats, statType, rankFormat, config.getString("gui.defendColour", "&d")));
                } else {
                    line = line.replace("{defending_ranks}", "");
                }

                if (!line.isEmpty()) updatedLore.add(line);
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

    private String generateRankLines(Map<UUID, PlayerStats> stats, RaidStatType statType, String rankFormat, String color) {
        StringBuilder ranks = new StringBuilder();
        int rank = 1;

        if (stats.isEmpty()) {
            return "";
        }

        for (Map.Entry<UUID, PlayerStats> entryStat : stats.entrySet()) {
            String playerName = Bukkit.getServer().getOfflinePlayer(entryStat.getKey()).getName();
            int statValue = statType.getValue(entryStat.getValue());

            // If the stat type is DAMAGE_GIVEN or DAMAGE_TAKEN, convert the value to hearts
            if (statType == RaidStatType.DAMAGE_GIVEN || statType == RaidStatType.DAMAGE_TAKEN) {
                double hearts = statValue / 2.0;
                String heartsString = String.format("%.1f", hearts);
                rankFormat = rankFormat.replace("{hearts}", heartsString);
                if (statType == RaidStatType.DAMAGE_GIVEN) {
                    rankFormat = rankFormat.replace("{hits}", String.format("%,d", entryStat.getValue().getHits()));
                }

            }

            // Replace placeholders in the rank format
            ranks.append(rankFormat.replace("{rank}", String.format("%,d", rank))
                    .replace("{player_name}", playerName)
                    .replace("{stat_value}", String.format("%,d", statValue))
                    .replace("{color}", color));  // Include the color formatting
            rank++;
        }
        return ranks.toString();
    }


}