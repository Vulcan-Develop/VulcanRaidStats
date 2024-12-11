package net.xantharddev.raidstats.listener;

import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Factions;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.xantharddev.raidstats.RaidStats;
import net.xantharddev.raidstats.manager.StatsManager;
import net.xantharddev.raidstats.objects.Colour;
import net.xantharddev.raidstats.objects.RaidObject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import com.golfing8.kore.feature.RaidClaimFeature.Pair;

import java.util.List;
import java.util.Map;

public class CommandListener implements Listener {
    private final RaidStats plugin;
    private final StatsManager statsManager;
    private final List<String> validCommands;
    private final List<String> validStaffRaidCommands;
    private final List<String> validClearCommands;

    public CommandListener(RaidStats plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;

        // Load configurable commands from config.yml
        this.validCommands = plugin.getConfig().getStringList("valid-commands");
        this.validStaffRaidCommands = plugin.getConfig().getStringList("valid-staff-raid-commands");
        this.validClearCommands = plugin.getConfig().getStringList("valid-clear-commands");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        if (!isValidCommand(command)) return;

        // Check if the player has the necessary permission
        boolean hasRaidClaimPermission = player.hasPermission("factionskore.admin.raid-claim");

        // Handle clear command (permission check is required)
        if (isClearCommand(command) && hasRaidClaimPermission) {
            statsManager.clearAllRaids();
            return;
        }

        // If the command is a staff command or the player doesn't have permission, cancel the event
        if (!isValidStaffCommand(command) && hasRaidClaimPermission) {
            return;  // Let the staff command pass through if the player has permission
        }

        event.setCancelled(true);

        displayActiveRaids(player);
        displayActiveGracePeriods(player);
    }

    private boolean isClearCommand(String command) {
        return validClearCommands.stream().anyMatch(command::startsWith);
    }

    private boolean isValidCommand(String command) {
        return validCommands.stream().anyMatch(command::startsWith);
    }

    private boolean isValidStaffCommand(String command) {
        return validStaffRaidCommands.stream().anyMatch(command::startsWith);
    }

    private void displayActiveRaids(Player player) {
        sendMessage(player, "messages.active-raids-header");

        List<RaidObject> allRaids = statsManager.getAllRaids();

        if (allRaids.isEmpty()) {
            sendMessage(player, "messages.no-active-raids");
            return;
        }

        int number = 1;
        boolean activeRaids = false;

        for (RaidObject raid : allRaids) {
            if (raid.isGrace()) continue;

            if (!raid.getKoreRaid().isDiscovered()) {
                String raidingID = raid.getRaidingFaction();
                if (!FPlayers.getInstance().getByPlayer(player).getFactionId().equals(raidingID) && !player.hasPermission("factionskore.admin.raid-claim")) continue;
            }

            activeRaids = true;
            String raidingFaction = getFactionTag(raid.getRaidingFaction());
            String defendingFaction = getFactionTag(raid.getDefendingFaction());

            String message = plugin.getConfig().getString("messages.active-raid")
                    .replace("{number}", String.valueOf(number))
                    .replace("{raidingFaction}", raidingFaction)
                    .replace("{defendingFaction}", defendingFaction)
                    .replace("{timeSinceStart}", String.valueOf(raid.getKoreRaid().getTimeSinceStart()))
                    .replace("{timeLeft}", String.valueOf(raid.getKoreRaid().getTimeLeft()));

            sendClickableMessage(player, message, "/viewraid " + raid.getId().toString(),
                    plugin.getConfig().getString("messages.raid-hover-text"));
            number++;
        }

        if (!activeRaids) {
            sendMessage(player, "messages.no-active-raids");
        }
    }

    private void displayActiveGracePeriods(Player player) {
        sendMessage(player, "messages.active-grace-header");

        Map<String, Pair<String, Integer>> graceData = plugin.getRaidTimer().getGracePeriods();

        if (graceData.isEmpty()) {
            sendMessage(player, "messages.no-active-grace");
            return;
        }

        for (Map.Entry<String, Pair<String, Integer>> entry : graceData.entrySet()) {
            String defendingFactionId = entry.getKey();
            Pair<String, Integer> graceDetails = entry.getValue();
            String raidingFactionId = graceDetails.getA();
            int graceTimeLeft = graceDetails.getB();

            RaidObject raid = statsManager.getRaidDefendingByFacID(defendingFactionId);
            if (raid == null) continue;

            String raidingFaction = getFactionTag(raidingFactionId);
            String defendingFaction = getFactionTag(defendingFactionId);
            String graceTimeLeftFormatted = graceTimeLeft > 0 ? graceTimeLeft + "m" : "No grace time left";

            String message = plugin.getConfig().getString("messages.active-grace")
                    .replace("{defendingFaction}", defendingFaction)
                    .replace("{raidingFaction}", raidingFaction)
                    .replace("{graceTimeLeft}", graceTimeLeftFormatted);

            sendClickableMessage(player, message, "/viewraid " + raid.getId().toString(),
                    plugin.getConfig().getString("messages.grace-hover-text"));
        }
    }


    private String getFactionTag(String factionId) {
        return Factions.getInstance().getFactionById(factionId).getTag();
    }

    private void sendClickableMessage(Player player, String message, String command, String hoverText) {
        TextComponent textComponent = new TextComponent(Colour.colour(message));
        textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Colour.colour(hoverText)).create()));
        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        player.spigot().sendMessage(textComponent);
    }

    private void sendMessage(Player player, String configKey) {
        player.sendMessage(Colour.colour(plugin.getConfig().getString(configKey)));
    }
}