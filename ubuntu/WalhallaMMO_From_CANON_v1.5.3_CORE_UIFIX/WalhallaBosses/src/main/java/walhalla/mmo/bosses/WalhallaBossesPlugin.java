package walhalla.mmo.bosses;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.bosses.endgame.EndgameConfig;
import walhalla.mmo.bosses.endgame.EndgameLootRegistry;
import walhalla.mmo.bosses.endgame.ElitePromotionListener;
import walhalla.mmo.bosses.endgame.EntityDropListener;
import walhalla.mmo.bosses.endgame.WorldBossSpawner;

/**
 * Phase 13 (Endgame real):
 * - Elite enemies (promoted on first engagement in risky zones)
 * - Canon entity drops (ANNEX_ENTITY_DROPS_AND_LOOT_TABLES_v1.1) overriding vanilla drops
 * - Optional world bosses (data-driven, no player commands)
 */
public class WalhallaBossesPlugin extends JavaPlugin {

    private EndgameConfig config;
    private EndgameLootRegistry lootRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = EndgameConfig.load(this);

        // Canon loot registry (strict: if canon not available -> still enable bosses plugin,
        // but loot overrides will be disabled and logged).
        this.lootRegistry = EndgameLootRegistry.tryLoadFromCanon(this);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new EntityDropListener(this, config, lootRegistry), this);
        Bukkit.getPluginManager().registerEvents(new ElitePromotionListener(this, config), this);

        // World boss spawner (optional)
        WorldBossSpawner spawner = new WorldBossSpawner(this, config);
        spawner.start();

        getLogger().info("WalhallaBosses enabled (Phase 13 Endgame).");
    }

    @Override
    public void onDisable() {
        getLogger().info("WalhallaBosses disabled.");
    }
}
