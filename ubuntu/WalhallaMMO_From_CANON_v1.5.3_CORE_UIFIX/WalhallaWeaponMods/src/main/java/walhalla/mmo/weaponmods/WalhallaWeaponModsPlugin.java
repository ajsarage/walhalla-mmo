package walhalla.mmo.weaponmods;

import org.bukkit.plugin.java.JavaPlugin;

public class WalhallaWeaponModsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        getLogger().info("WalhallaWeaponModsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        
        getLogger().info("WalhallaWeaponModsPlugin disabled.");
    }
}
