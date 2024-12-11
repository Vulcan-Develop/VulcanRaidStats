package net.xantharddev.raidstats;

import com.golfing8.kore.FactionsKore;
import com.golfing8.kore.feature.RaidClaimFeature;
import net.xantharddev.raidstats.command.ViewRaidCommand;
import net.xantharddev.raidstats.data.DataManager;
import net.xantharddev.raidstats.gui.GUIListeners;
import net.xantharddev.raidstats.integration.FactionsKoreRaidTimer;
import net.xantharddev.raidstats.listener.CommandListener;
import net.xantharddev.raidstats.listener.RaidEventListener;
import net.xantharddev.raidstats.listener.StatsListener;
import net.xantharddev.raidstats.manager.StatsManager;
import net.xantharddev.raidstats.objects.Colour;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class RaidStats extends JavaPlugin {
    private FactionsKoreRaidTimer raidTimer;
    private DataManager dataManager;

    public FactionsKoreRaidTimer getRaidTimer() {return raidTimer;}

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        StatsManager statsManager = new StatsManager();
        Bukkit.getPluginManager().registerEvents(new StatsListener(this, statsManager), this);
        Bukkit.getPluginManager().registerEvents(new RaidEventListener(this, statsManager), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(this, statsManager), this);
        Bukkit.getPluginManager().registerEvents(new GUIListeners(), this);
        this.getCommand("viewraid").setExecutor(new ViewRaidCommand(this, statsManager));
        setupRaidTimer(statsManager);
        getLogger().info(Colour.colour("&6[RaidStats] &eRaid Stats Successfully enabled."));
    }

    private void setupRaidTimer(StatsManager statsManager) {
        getServer().getScheduler().runTaskLater(this, () -> {
            RaidClaimFeature feature = null;
            if (getServer().getPluginManager().getPlugin("FactionsKore") != null) {
                feature = FactionsKore.get().getFeature(RaidClaimFeature.class);
            }
            raidTimer = new FactionsKoreRaidTimer(feature);
            dataManager = new DataManager(getDataFolder(), statsManager, this);
            dataManager.loadAllRaids();
        }, 20L);
    }

    @Override
    public void onDisable() {
        dataManager.saveAllRaids();
    }
}
