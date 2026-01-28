package walhalla.mmo.combat.kits;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Simple persistence for player kit selection (admin/debug until UI sub-branch selection is implemented).
 */
public final class PlayerKitStore {

    private final File file;
    private final Map<UUID, String> kits = new HashMap<>();

    public PlayerKitStore(File file) {
        this.file = file;
    }

    public void load() throws IOException {
        kits.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        var sec = yml.getConfigurationSection("players");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(k);
                String kit = sec.getString(k);
                if (kit != null && !kit.isBlank()) kits.put(id, kit.trim());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() throws IOException {
        YamlConfiguration yml = new YamlConfiguration();
        for (var e : kits.entrySet()) {
            yml.set("players." + e.getKey().toString(), e.getValue());
        }
        yml.save(file);
    }

    public Optional<String> getKit(UUID playerId) {
        return Optional.ofNullable(kits.get(playerId));
    }

    public void setKit(UUID playerId, String kitId) {
        if (playerId == null) return;
        if (kitId == null || kitId.isBlank()) kits.remove(playerId);
        else kits.put(playerId, kitId.trim());
    }
}
