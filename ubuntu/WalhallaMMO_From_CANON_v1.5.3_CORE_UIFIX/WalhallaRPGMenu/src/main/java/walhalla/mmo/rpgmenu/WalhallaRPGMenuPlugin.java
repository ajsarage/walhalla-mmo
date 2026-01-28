package walhalla.mmo.rpgmenu;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class WalhallaRPGMenuPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        RpgMenuController ctrl = new RpgMenuController(this);
        Bukkit.getPluginManager().registerEvents(ctrl, this);

        getLogger().info("WalhallaRPGMenu enabled (Phase 3).");
    }

    @Override
    public void onDisable() {
        getLogger().info("WalhallaRPGMenu disabled.");
    }
}
