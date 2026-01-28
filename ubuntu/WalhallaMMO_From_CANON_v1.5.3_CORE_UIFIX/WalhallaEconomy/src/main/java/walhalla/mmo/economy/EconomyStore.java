package walhalla.mmo.economy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simple YAML-backed wallet store.
 */
public final class EconomyStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Long> balances = new HashMap<>();

    public EconomyStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        load();
    }

    private void load() {
        if (!file.exists()) {
            flush();
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String k : yml.getKeys(false)) {
            try {
                UUID id = UUID.fromString(k);
                long bal = yml.getLong(k, 0L);
                balances.put(id, Math.max(0L, bal));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public synchronized long get(UUID id) {
        return balances.getOrDefault(id, 0L);
    }

    public synchronized void set(UUID id, long value) {
        if (id == null) return;
        balances.put(id, Math.max(0L, value));
    }

    public synchronized void flush() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : balances.entrySet()) {
            yml.set(e.getKey().toString(), Math.max(0L, e.getValue() == null ? 0L : e.getValue()));
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save balances.yml: " + ex.getMessage());
        }
    }
}
