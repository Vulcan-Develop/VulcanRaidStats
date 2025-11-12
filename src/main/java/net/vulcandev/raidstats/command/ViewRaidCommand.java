package net.vulcandev.raidstats.command;

import net.vulcandev.raidstats.gui.RaidGUI;
import net.vulcandev.raidstats.manager.StatsManager;
import net.vulcandev.raidstats.objects.RaidStats;
import net.xantharddev.vulcanlib.command.VulcanCommand;
import net.xantharddev.vulcanlib.command.args.ArgumentType;
import net.xantharddev.vulcanlib.command.args.CommandArgument;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command that allows players to view detailed raid statistics via a GUI.
 * Usage: /viewraid <raidUUID>
 */
public class ViewRaidCommand {
    /**
     * Creates and configures the viewraid command with proper validation and tab completion.
     *
     * @param plugin The plugin instance
     * @param statsManager The stats manager for accessing raid data
     * @return The configured command ready to be registered
     */
    public static VulcanCommand create(net.vulcandev.raidstats.VulcanRaidStats plugin, StatsManager statsManager) {
        return VulcanCommand.create("viewraid")
                .description("View raid statistics")
                .alias("vr", "raidstats")
                .playerOnly()
                .argument(CommandArgument.of("raidUUID", ArgumentType.STRING)
                        .description("The UUID of the raid to view")
                        .validator(input -> {
                            try {
                                UUID.fromString(input);
                                return true;
                            } catch (IllegalArgumentException e) {
                                return false;
                            }
                        })
                        .required()
                        .completer((sender, partial) ->
                                statsManager.getAllRaids().stream()
                                        .map(raid -> raid.getId().toString())
                                        .filter(uuid -> uuid.toLowerCase().startsWith(partial.toLowerCase()))
                                        .collect(Collectors.toList())
                        )
                        .build())
                .execute((sender, ctx) -> {
                    Player player = (Player) sender;
                    String uuidString = ctx.getString("raidUUID", "");

                    try {
                        UUID raidUUID = UUID.fromString(uuidString);
                        RaidStats raid = statsManager.getRaidByUUID(raidUUID);

                        if (raid == null) {
                            player.sendMessage("§cRaid not found for the given UUID.");
                            return;
                        }

                        new RaidGUI(plugin, raid, player).open();
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid UUID format. Please check the UUID and try again.");
                    }
                })
                .build();
    }
}

