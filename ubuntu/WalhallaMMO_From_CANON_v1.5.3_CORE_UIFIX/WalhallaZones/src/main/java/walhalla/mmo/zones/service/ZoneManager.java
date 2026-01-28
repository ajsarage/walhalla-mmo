package walhalla.mmo.zones.service;

import java.io.File;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.zones.*;
import walhalla.mmo.zones.model.*;

/**
 * Phase 13: Zones service implementation.
 * Data-driven via WalhallaZones/zones.yml + rulesets.yml.
 */
public class ZoneManager implements ZoneService {

    private final JavaPlugin plugin;

    private final Map<String, ZoneDefinition> zonesById = new LinkedHashMap<>();
    private final Map<String, ZoneRuleset> rulesetsById = new LinkedHashMap<>();
    private final Map<UUID, String> currentZoneByPlayer = new HashMap<>();

    private String defaultZoneId = "global_safe";

    public ZoneManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        zonesById.clear();
        rulesetsById.clear();

        loadRulesets();
        loadZones();

        // Refresh online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayerZone(p);
        }

        plugin.getLogger().info("Zones loaded: " + zonesById.size() + ", rulesets: " + rulesetsById.size());
    }

    private void loadRulesets() {
        File f = new File(plugin.getDataFolder(), "rulesets.yml");
        if (!f.exists()) plugin.saveResource("rulesets.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);

        defaultZoneId = y.getString("defaults.defaultZoneId", defaultZoneId);

        var section = y.getConfigurationSection("rulesets");
        if (section == null) {
            plugin.getLogger().warning("rulesets.yml missing 'rulesets' section. Using fallback SAFE ruleset only.");
            ZoneRuleset safe = defaultSafeRuleset();
            rulesetsById.put(safe.rulesetId(), safe);
            return;
        }

        for (String id : section.getKeys(false)) {
            String path = "rulesets." + id;
            String name = y.getString(path + ".name", id);
            ZoneType type = ZoneType.valueOf(y.getString(path + ".zoneType", "SAFE").toUpperCase(Locale.ROOT));
            boolean pvp = y.getBoolean(path + ".pvpEnabled", false);
            boolean ff = y.getBoolean(path + ".friendlyFireEnabled", false);
            boolean combat = y.getBoolean(path + ".combatAllowed", true);
            List<String> professions = y.getStringList(path + ".professionsAllowed");
            List<String> tools = y.getStringList(path + ".allowedTools");
            DeathPenalty penalty = DeathPenalty.valueOf(y.getString(path + ".deathPenalty", "NONE").toUpperCase(Locale.ROOT));
            long respawnProtMs = y.getLong(path + ".respawnProtectionDurationMs", 10_000L);
            String cc = y.getString(path + ".ccRulesetId", "DEFAULT");
            boolean econ = y.getBoolean(path + ".economyEnabled", true);
            Map<String, Boolean> flags = new HashMap<>();
            var fsec = y.getConfigurationSection(path + ".zoneSpecificFlags");
            if (fsec != null) {
                for (String k : fsec.getKeys(false)) {
                    flags.put(k, fsec.getBoolean(k));
                }
            }

            ZoneRuleset rs = new ZoneRuleset(id, name, type, pvp, ff, combat, professions, tools, penalty, respawnProtMs, cc, econ, flags);
            rulesetsById.put(id, rs);
        }

        // Guarantee a SAFE fallback
        rulesetsById.putIfAbsent("SAFE", defaultSafeRuleset());
    }

    private void loadZones() {
        File f = new File(plugin.getDataFolder(), "zones.yml");
        if (!f.exists()) plugin.saveResource("zones.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);

        defaultZoneId = y.getString("defaults.defaultZoneId", defaultZoneId);

        var section = y.getConfigurationSection("zones");
        if (section == null) {
            plugin.getLogger().warning("zones.yml missing 'zones' section. Creating implicit global SAFE zone.");
            zonesById.put(defaultZoneId, new ZoneDefinition(defaultZoneId, "Global Safe", 1, "SAFE", null, Map.of()));
            return;
        }

        for (String id : section.getKeys(false)) {
            String path = "zones." + id;
            String name = y.getString(path + ".name", id);
            int version = y.getInt(path + ".version", 1);
            String rulesetId = y.getString(path + ".rulesetId", "SAFE");

            ZoneBox box = null;
            String world = y.getString(path + ".bounds.world", null);
            if (world != null && !world.isBlank()) {
                int minX = y.getInt(path + ".bounds.minX");
                int minY = y.getInt(path + ".bounds.minY");
                int minZ = y.getInt(path + ".bounds.minZ");
                int maxX = y.getInt(path + ".bounds.maxX");
                int maxY = y.getInt(path + ".bounds.maxY");
                int maxZ = y.getInt(path + ".bounds.maxZ");
                box = new ZoneBox(world, minX, minY, minZ, maxX, maxY, maxZ);
            }

            // Basic validation: ruleset must exist
            if (!rulesetsById.containsKey(rulesetId)) {
                plugin.getLogger().warning("Zone '" + id + "' references unknown rulesetId '" + rulesetId + "'. Using SAFE.");
                rulesetId = "SAFE";
            }

            zonesById.put(id, new ZoneDefinition(id, name, version, rulesetId, box, Map.of()));
        }

        zonesById.putIfAbsent(defaultZoneId, new ZoneDefinition(defaultZoneId, "Global Safe", 1, "SAFE", null, Map.of()));
    }

    private ZoneRuleset defaultSafeRuleset() {
        return new ZoneRuleset(
                "SAFE",
                "Safe Zone (Default)",
                ZoneType.SAFE,
                false,
                false,
                true,
                List.of(),
                List.of(),
                DeathPenalty.NONE,
                10_000L,
                "DEFAULT",
                true,
                Map.of()
        );
    }

    /** Updates and returns the current zone id for a player. */
    public String updatePlayerZone(Player p) {
        String zid = resolveZoneId(p.getLocation());
        if (zid == null) zid = defaultZoneId;
        currentZoneByPlayer.put(p.getUniqueId(), zid);
        return zid;
    }

    public String resolveZoneId(Location loc) {
        // First match with explicit bounds.
        for (ZoneDefinition def : zonesById.values()) {
            ZoneBox b = def.bounds();
            if (b != null && b.contains(loc)) return def.zoneId();
        }
        // Fallback to global/default
        return defaultZoneId;
    }

    // ---- ZoneService ----

    @Override
    public String getCurrentZoneId(UUID playerId) {
        return currentZoneByPlayer.get(playerId);
    }

    @Override
    public ZoneRulesView getZoneRules(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) return ZoneRulesView.safeFallback();
        ZoneDefinition def = zonesById.get(zoneId);
        if (def == null) return ZoneRulesView.safeFallback();
        ZoneRuleset rs = rulesetsById.getOrDefault(def.rulesetId(), defaultSafeRuleset());
        return new ZoneRulesView(
                rs.zoneType(),
                rs.combatAllowed(),
                rs.pvpEnabled(),
                rs.friendlyFireEnabled(),
                rs.professionsAllowed(),
                rs.allowedTools(),
                rs.deathPenalty(),
                rs.respawnProtectionDurationMs(),
                rs.ccRulesetId(),
                rs.economyEnabled(),
                rs.zoneSpecificFlags()
        );
    }

    @Override
    public ZoneType getZoneType(String zoneId) {
        return getZoneRules(zoneId).zoneType();
    }

    @Override
    public Set<String> getAllZoneIds() {
        return Collections.unmodifiableSet(zonesById.keySet());
    }
}
