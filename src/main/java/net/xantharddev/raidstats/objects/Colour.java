package net.xantharddev.raidstats.objects;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public class Colour {
    public static String colour(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static List<String> colourList(List<String> stringList) {
        return stringList.stream()
                .map(Colour::colour)
                .collect(Collectors.toList());
    }
}
