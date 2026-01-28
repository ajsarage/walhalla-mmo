package walhalla.mmo.events;

import org.bukkit.plugin.java.JavaPlugin;

public class WalhallaEventsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        getLogger().info("WalhallaEventsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        
        getLogger().info("WalhallaEventsPlugin disabled.");
    }
}
