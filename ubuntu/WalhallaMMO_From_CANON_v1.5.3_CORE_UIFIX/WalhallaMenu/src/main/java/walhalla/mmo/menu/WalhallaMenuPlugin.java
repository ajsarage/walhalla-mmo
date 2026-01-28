package walhalla.mmo.menu;

import org.bukkit.plugin.java.JavaPlugin;

public class WalhallaMenuPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        getLogger().info("WalhallaMenuPlugin enabled.");
    }

    @Override
    public void onDisable() {
        
        getLogger().info("WalhallaMenuPlugin disabled.");
    }
}
