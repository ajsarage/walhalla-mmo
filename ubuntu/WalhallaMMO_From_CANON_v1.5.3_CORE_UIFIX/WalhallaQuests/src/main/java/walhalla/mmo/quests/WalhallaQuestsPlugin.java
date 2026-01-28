package walhalla.mmo.quests;

import org.bukkit.plugin.java.JavaPlugin;

public class WalhallaQuestsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        getLogger().info("WalhallaQuestsPlugin enabled.");
    }

    @Override
    public void onDisable() {
        
        getLogger().info("WalhallaQuestsPlugin disabled.");
    }
}
