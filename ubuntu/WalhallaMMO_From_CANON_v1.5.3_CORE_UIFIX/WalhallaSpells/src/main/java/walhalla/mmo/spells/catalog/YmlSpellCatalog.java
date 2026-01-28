package walhalla.mmo.spells.catalog;

import java.io.File;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.combat.DamageType;
import walhalla.mmo.core.api.spells.SpellCatalogBridge;
import walhalla.mmo.core.api.spells.SpellCatalogEntry;
import walhalla.mmo.spells.model.*;

/**
 * Data-driven spell catalog.
 *
 * Important: Players do not type commands. If no spells exist, we generate canonical templates
 * so the system is playable from zero without requiring the user to hand-write YML.
 */
public class YmlSpellCatalog implements SpellCatalogBridge {

    private final JavaPlugin plugin;
    private final File dir;

    private final Map<String, SpellCatalogEntry> catalogEntries = new HashMap<>();
    private final Map<String, SpellDefinition> definitions = new HashMap<>();

    public YmlSpellCatalog(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "spells");
        if (!dir.exists()) dir.mkdirs();
    }

    public File getDir() { return dir; }

    public void reload() {
        if (!dir.exists()) dir.mkdirs();

        // If empty, generate starter templates (not balance, just structural placeholders).
        File[] existing = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (existing == null || existing.length == 0) {
            StarterTemplates.generate(plugin, dir);
        }

        catalogEntries.clear();
        definitions.clear();

        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return;

        for (File f : files) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);

            String id = yml.getString("spellId");
            if (id == null || id.isBlank()) {
                plugin.getLogger().warning("Skipping spell file without spellId: " + f.getName());
                continue;
            }
            id = id.trim();

            String displayName = opt(yml.getString("displayName"), id);

            // Core/UI-facing catalog fields (kept compatible with older minimal schema)
            String element = opt(yml.getString("element"), opt(yml.getString("branchId"), "GENERIC"));
            boolean combined = yml.getBoolean("combined", false);
            catalogEntries.put(id, new SpellCatalogEntry(id, displayName, element, combined));

            // Execution schema (Phase 3)
            String branchId = opt(yml.getString("branchId"), "GENERIC");
            SpellSlot slot = parseEnum(SpellSlot.class, yml.getString("slot"), SpellSlot.PRIMARY);
            SpellCastType castType = parseEnum(SpellCastType.class, yml.getString("castType"), SpellCastType.TARGET_RAY);
            DamageType dmgType = parseEnum(DamageType.class, yml.getString("damageType"), DamageType.MAGICAL);

            double baseDamage = yml.getDouble("baseDamage", 2.0);
            int cooldownTicks = Math.max(0, yml.getInt("cooldownTicks", 20));
            int range = Math.max(1, yml.getInt("range", 16));
            double radius = Math.max(0.0, yml.getDouble("radius", 4.0));

            String statusId = yml.getString("status.id");
            int statusDurationTicks = Math.max(0, yml.getInt("status.durationTicks", 0));

            definitions.put(id, new SpellDefinition(
                    id, displayName, branchId, slot, castType, dmgType, baseDamage,
                    cooldownTicks, range, radius,
                    statusId != null && !statusId.isBlank() ? statusId.trim() : null,
                    statusDurationTicks
            ));
        }
    }

    private static String opt(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String raw, T def) {
        if (raw == null || raw.isBlank()) return def;
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    // SpellCatalogBridge
    @Override
    public Set<String> getAllSpellIds() {
        return Collections.unmodifiableSet(catalogEntries.keySet());
    }

    @Override
    public SpellCatalogEntry getSpell(String spellId) {
        if (spellId == null) return null;
        return catalogEntries.get(spellId);
    }

    public Optional<SpellDefinition> getDefinition(String spellId) {
        if (spellId == null) return Optional.empty();
        return Optional.ofNullable(definitions.get(spellId));
    }

    public Collection<SpellDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }
}
