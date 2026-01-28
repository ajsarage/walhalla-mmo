package walhalla.mmo.core.progress;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * YAML persistence (one file per player).
 *
 * Canon: XP persistence is deferred and performed via full snapshots (CONTRACT_XP_PERSISTENCE_v1, XP-2/XP-3/XP-4).
 */
public class PlayerProgressStore {

    private final JavaPlugin plugin;
    private final File dir;

    public PlayerProgressStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
    }

    public PlayerData load(UUID playerId) {
        File f = new File(dir, playerId.toString() + ".yml");
        PlayerData data = new PlayerData(playerId);
        if (!f.exists()) return data;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);

        // Lifecycle
        String st = yml.getString("lifecycle.state", "UNINITIALIZED");
        try {
            data.setLifecycleState(PlayerData.PlayerLifecycleState.valueOf(st));
        } catch (IllegalArgumentException ignored) {
            data.setLifecycleState(PlayerData.PlayerLifecycleState.UNINITIALIZED);
        }

        // Onboarding
        data.setChosenBranches(yml.getStringList("onboarding.chosenBranches"));

        // Global XP totals (support legacy key global.xp from earlier bootstrap)
        long globalTotal = yml.getLong("global.totalXp", yml.getLong("global.xp", 0L));
        data.setGlobalXpTotal(globalTotal);

        // RPG points
        data.setRpgPointsEarned(yml.getInt("points.rpg.earned", 0));
        data.setRpgPointsSpent(yml.getInt("points.rpg.spent", 0));

        // Economy
        data.setWcoin(yml.getLong("economy.wcoin", 0L));
        data.setWcraft(yml.getLong("economy.wcraft", 0L));

        // Branch XP
        readTotalsMap(yml, "xp.branch", data.getBranchXpTotal());

        // Sub-branch XP
        readTotalsMap(yml, "xp.subbranch", data.getSubBranchXpTotal());

        // Affinity XP
        readTotalsMap(yml, "xp.affinity", data.getAffinityXpTotal());

        // Profession XP
        readTotalsMap(yml, "xp.profession", data.getProfessionXpTotal());

        return data;
    }

    private void readTotalsMap(YamlConfiguration yml, String path, Map<String, Long> out) {
        out.clear();
        if (!yml.isConfigurationSection(path)) return;
        ConfigurationSection sec = yml.getConfigurationSection(path);
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            long v = yml.getLong(path + "." + key, 0L);
            out.put(key, Math.max(0L, v));
        }
    }

    public void save(PlayerData data) {
        File f = new File(dir, data.getPlayerId().toString() + ".yml");
        YamlConfiguration yml = new YamlConfiguration();

        // Lifecycle
        yml.set("lifecycle.state", data.getLifecycleState().name());

        // Onboarding
        yml.set("onboarding.chosenBranches", new ArrayList<>(data.getChosenBranches()));

        // Global totals
        yml.set("global.totalXp", data.getGlobalXpTotal());

        // RPG points
        yml.set("points.rpg.earned", data.getRpgPointsEarned());
        yml.set("points.rpg.spent", data.getRpgPointsSpent());

        // Economy
        yml.set("economy.wcoin", data.getWcoin());
        yml.set("economy.wcraft", data.getWcraft());

        // XP maps
        writeTotalsMap(yml, "xp.branch", data.getBranchXpTotal());
        writeTotalsMap(yml, "xp.subbranch", data.getSubBranchXpTotal());
        writeTotalsMap(yml, "xp.affinity", data.getAffinityXpTotal());
        writeTotalsMap(yml, "xp.profession", data.getProfessionXpTotal());

        try {
            yml.save(f);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed saving PlayerData for " + data.getPlayerId() + ": " + e.getMessage());
        }
    }

    private void writeTotalsMap(YamlConfiguration yml, String path, Map<String, Long> map) {
        if (map == null || map.isEmpty()) return;
        for (Map.Entry<String, Long> e : map.entrySet()) {
            String k = e.getKey();
            if (k == null || k.isBlank()) continue;
            yml.set(path + "." + k, Math.max(0L, e.getValue() == null ? 0L : e.getValue()));
        }
    }
}
