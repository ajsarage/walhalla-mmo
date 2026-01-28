package walhalla.mmo.economy;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Phase 12: Economy gameplay is executed by WalhallaCore (balances + audit).
 *
 * This plugin is intentionally lightweight in v1.2.0:
 * - keeps a dedicated namespace for future phases (market/trade UI)
 * - does NOT own balances
 * - does NOT register EconomyBridge
 */
public class WalhallaEconomyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("WalhallaEconomy enabled (Phase 12: UI/market placeholder; balances in Core).");
    }

    @Override
    public void onDisable() {
        getLogger().info("WalhallaEconomy disabled.");
    }
}
