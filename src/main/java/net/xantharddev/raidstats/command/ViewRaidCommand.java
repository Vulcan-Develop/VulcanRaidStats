package net.xantharddev.raidstats.command;

import net.xantharddev.raidstats.RaidStats;
import net.xantharddev.raidstats.gui.RaidGUI;
import net.xantharddev.raidstats.manager.StatsManager;
import net.xantharddev.raidstats.objects.RaidObject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ViewRaidCommand implements CommandExecutor {
    private final RaidStats plugin;
    private final StatsManager statsManager;

    public ViewRaidCommand(RaidStats plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("§cUsage: /viewraid <raidUUID>");
            return true;
        }

        try {
            UUID raidUUID = UUID.fromString(args[0]);
            RaidObject raid = statsManager.getRaidByUUID(raidUUID);

            if (raid == null) {
                player.sendMessage("§cRaid not found for the given UUID.");
                return true;
            }

            new RaidGUI(plugin, raid, player).open();
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid UUID format. Please check the UUID and try again.");
        }

        return true;
    }
}

