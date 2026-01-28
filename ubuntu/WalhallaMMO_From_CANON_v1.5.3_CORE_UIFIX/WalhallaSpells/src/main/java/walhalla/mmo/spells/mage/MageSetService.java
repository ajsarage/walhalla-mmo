package walhalla.mmo.spells.mage;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.spells.engine.EquipService;
import walhalla.mmo.spells.model.SpellSlot;

/**
 * Stores per-player selected mage set and applies it to equipped spell slots.
 * Player never uses commands; OP may manage via /walhalla mage ...
 */
public final class MageSetService {

    private final JavaPlugin plugin;
    private final MageSetCatalog catalog;
    private final EquipService equips;

    private final Map<UUID, String> playerSets = new ConcurrentHashMap<>();
    private final File file;

    public MageSetService(JavaPlugin plugin, MageSetCatalog catalog, EquipService equips) {
        this.plugin = plugin;
        this.catalog = catalog;
        this.equips = equips;
        this.file = new File(plugin.getDataFolder(), "mage_sets.yml");
    }

    public void load() {
        playerSets.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String k : y.getKeys(false)) {
            try {
                UUID id = UUID.fromString(k);
                String setId = y.getString(k, "");
                if (setId != null && !setId.isBlank()) {
                    playerSets.put(id, setId.toUpperCase(Locale.ROOT));
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : playerSets.entrySet()) {
            y.set(e.getKey().toString(), e.getValue());
        }
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save mage_sets.yml: " + ex.getMessage());
        }
    }

    public Optional<String> getPlayerSetId(UUID playerId) {
        if (playerId == null) return Optional.empty();
        return Optional.ofNullable(playerSets.get(playerId));
    }

    public void ensureDefault(UUID playerId) {
        if (playerId == null) return;
        if (playerSets.containsKey(playerId)) return;
        List<String> ids = catalog.getAllSetIds();
        if (ids.isEmpty()) return;
        setPlayerSet(playerId, ids.get(0));
    }

    public boolean setPlayerSet(UUID playerId, String setId) {
        if (playerId == null || setId == null || setId.isBlank()) return false;
        String sid = setId.trim().toUpperCase(Locale.ROOT);
        Optional<MageSetDefinition> def = catalog.get(sid);
        if (def.isEmpty()) return false;
        playerSets.put(playerId, sid);
        applyToEquips(playerId, def.get());
        return true;
    }

    public void applyToEquips(UUID playerId, MageSetDefinition def) {
        if (playerId == null || def == null) return;
        equips.setEquipped(playerId, SpellSlot.PRIMARY, def.primarySpellId());
        equips.setEquipped(playerId, SpellSlot.SECONDARY, def.secondarySpellId());
        equips.setEquipped(playerId, SpellSlot.SPECIAL, def.specialSpellId());
    }
}
