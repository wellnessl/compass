package me.subhanafz.compasstrack.compasstracker;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

public class CompassTracker extends JavaPlugin {

    public static CompassTracker plugin;
    /** Runner UUID'leri (eklenme sirasi korunur). */
    public static final Set<UUID> Speedrunners = new LinkedHashSet<>();

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();

        Commands commands = new Commands();
        for (String name : new String[] {"giveCompass", "startTracking", "addSpeedrunners",
                "getSpeedrunners", "removeSpeedrunners"}) {
            getCommand(name).setExecutor(commands);
        }

        getServer().getPluginManager().registerEvents(new TrackerListener(), this);
        TrackerManager.startTask(this);

        getLogger().info("Compass Tracker has started up successfully!");
    }
}
