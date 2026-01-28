package walhalla.mmo.weapons;

import org.bukkit.plugin.java.JavaPlugin;

public class WalhallaWeaponsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        getLogger().info("WalhallaWeaponsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        
        getLogger().info("WalhallaWeaponsPlugin disabled.");
    }
}
