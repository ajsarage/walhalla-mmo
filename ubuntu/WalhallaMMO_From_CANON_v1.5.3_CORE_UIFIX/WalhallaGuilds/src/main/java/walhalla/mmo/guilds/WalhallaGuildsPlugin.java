package walhalla.mmo.guilds;

import org.bukkit.plugin.java.JavaPlugin;

public class WalhallaGuildsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        getLogger().info("WalhallaGuildsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        
        getLogger().info("WalhallaGuildsPlugin disabled.");
    }
}
