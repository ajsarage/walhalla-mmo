package walhalla.mmo.market;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.market.MarketUiBridge;

public class WalhallaMarketPlugin extends JavaPlugin {

    private MarketStore store;
    private MarketController controller;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.store = new MarketStore(this);
        this.store.load();

        this.controller = new MarketController(this, store);

        Bukkit.getPluginManager().registerEvents(controller, this);

        Bukkit.getServicesManager().register(MarketUiBridge.class, controller, this, ServicePriority.Normal);

        PluginCommand cmd = getCommand("walhalla-market");
        if (cmd != null) {
            WalhallaMarketAdminCommand exec = new WalhallaMarketAdminCommand(this, store);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }

        getLogger().info("WalhallaMarket enabled (Phase 14).");
    }

    @Override
    public void onDisable() {
        try {
            if (store != null) store.flush();
        } catch (Exception ignored) {}
        getLogger().info("WalhallaMarket disabled.");
    }
}
