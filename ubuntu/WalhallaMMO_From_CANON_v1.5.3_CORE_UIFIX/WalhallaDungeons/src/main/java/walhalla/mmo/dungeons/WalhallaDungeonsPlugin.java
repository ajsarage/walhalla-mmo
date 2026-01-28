package walhalla.mmo.dungeons;

import org.bukkit.plugin.java.JavaPlugin;

public class WalhallaDungeonsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        getLogger().info("WalhallaDungeonsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        
        getLogger().info("WalhallaDungeonsPlugin disabled.");
    }
}
