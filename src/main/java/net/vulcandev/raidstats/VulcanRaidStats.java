package net.vulcandev.raidstats;

import com.golfing8.kore.FactionsKore;
import com.golfing8.kore.feature.RaidClaimFeature;
import com.golfing8.kore.feature.RaidingOutpostFeature;
import lombok.Getter;
import me.plugin.libs.YamlDocument;
import net.vulcandev.raidstats.command.ViewRaidCommand;
import net.vulcandev.raidstats.data.DataManager;
import net.vulcandev.raidstats.integration.FactionsKoreRaidTimer;
import net.vulcandev.raidstats.listener.CommandListener;
import net.vulcandev.raidstats.listener.RaidEventListener;
import net.vulcandev.raidstats.listener.StatsListener;
import net.vulcandev.raidstats.manager.StatsManager;
import net.vulcandev.vulcanloader.loader.VulcanPlugin;
import net.xantharddev.vulcanlib.ConfigFile;
import net.xantharddev.vulcanlib.Logger;
import org.bukkit.Bukkit;

/**
 * Main plugin class for RaidStats.
 * Tracks detailed statistics during faction raids including kills, deaths, damage, and blocks placed.
 * Integrates with FactionsKore to monitor active raids and grace periods.
 */
public final class VulcanRaidStats extends VulcanPlugin {
    // Handles saving and loading of raid data to disk
    private DataManager dataManager;

    // Integration wrapper for FactionsKore raid timers and grace periods
    @Getter
    private FactionsKoreRaidTimer raidTimer;

    // Reference to the raiding outpost feature if available
    @Getter
    private RaidingOutpostFeature raidingOutpost;

    // Plugin configuration file
    private YamlDocument conf;

    public YamlDocument conf() { return this.conf; }

    /**
     * Called when the plugin is enabled.
     * Initializes config, registers event listeners, and sets up the raid tracking system.
     */
    @Override
    public void onSecureEnable() {
        conf = ConfigFile.createConfig(this, "config.yml");
        StatsManager statsManager = new StatsManager(this);
        Bukkit.getPluginManager().registerEvents(new StatsListener(this, statsManager), this);
        Bukkit.getPluginManager().registerEvents(new RaidEventListener(this, statsManager), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(this, statsManager), this);
        ViewRaidCommand.create(this, statsManager).register(this);
        setupRaidTimer(statsManager);
        Logger.log(this, "Raid Stats Successfully enabled.");
    }

    /**
     * Sets up the raid timer integration with FactionsKore.
     * Runs after a short delay to ensure FactionsKore is fully loaded.
     */
    private void setupRaidTimer(StatsManager statsManager) {
        getServer().getScheduler().runTaskLater(this, () -> {
            RaidClaimFeature outpost = null;
            raidingOutpost = null;
            if (getServer().getPluginManager().getPlugin("FactionsKore") != null) {
                raidingOutpost = FactionsKore.get().getFeature(RaidingOutpostFeature.class);
                outpost = FactionsKore.get().getFeature(RaidClaimFeature.class);
            }
            raidTimer = new FactionsKoreRaidTimer(outpost);
            dataManager = new DataManager(getDataFolder(), statsManager, this);
            dataManager.loadAllRaids();
        }, 40L);
    }

    /**
     * Called when the plugin is disabled.
     * Saves all active raid data to prevent data loss.
     */
    @Override
    public void onSecureDisable() {
        dataManager.saveAllRaids();
    }
}
