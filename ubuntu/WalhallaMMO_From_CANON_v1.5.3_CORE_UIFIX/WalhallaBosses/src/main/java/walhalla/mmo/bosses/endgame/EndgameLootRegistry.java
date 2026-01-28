package walhalla.mmo.bosses.endgame;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.canon.CanonDataService;
import walhalla.mmo.core.canon.CanonFile;

import java.util.*;
import java.util.logging.Level;

/**
 * Reads canonical entity loot annex (v1.1 preferred) and exposes drop entries keyed by canonical entity key.
 * This registry is read-only and does not invent data.
 */
public final class EndgameLootRegistry {

    private final Map<String, List<DropEntry>> dropsByEntityKey;

    private EndgameLootRegistry(Map<String, List<DropEntry>> map) {
        this.dropsByEntityKey = Map.copyOf(map);
    }

    public List<DropEntry> getDropsFor(String entityKey) {
        if (entityKey == null) return List.of();
        return dropsByEntityKey.getOrDefault(entityKey, List.of());
    }

    public boolean isLoaded() { return !dropsByEntityKey.isEmpty(); }

    public static EndgameLootRegistry tryLoadFromCanon(JavaPlugin plugin) {
        try {
            Optional<CanonDataService> canonOpt = CoreAPI.getCanonService();
            if (canonOpt.isEmpty()) {
                plugin.getLogger().warning("CanonDataService not available via CoreAPI; entity drops overrides disabled.");
                return new EndgameLootRegistry(Map.of());
            }
            CanonDataService canon = canonOpt.get();

            String txt = canon.getRaw(CanonFile.ANNEX_ENTITY_LOOT_1_1).orElse(null);
            if (txt == null || txt.isBlank()) {
                txt = canon.getRaw(CanonFile.ANNEX_ENTITY_LOOT).orElse(null);
            }
            if (txt == null || txt.isBlank()) {
                plugin.getLogger().warning("Entity loot annex not found in canon; overrides disabled.");
                return new EndgameLootRegistry(Map.of());
            }

            Map<String, List<DropEntry>> parsed = EntityLootParser.parse(txt);
            plugin.getLogger().info("Loaded entity loot tables: " + parsed.size() + " entities");
            return new EndgameLootRegistry(parsed);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load entity loot annex; overrides disabled.", e);
            return new EndgameLootRegistry(Map.of());
        }
    }
}
