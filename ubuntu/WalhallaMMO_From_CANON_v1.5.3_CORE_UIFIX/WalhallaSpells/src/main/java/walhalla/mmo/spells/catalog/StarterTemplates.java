package walhalla.mmo.spells.catalog;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import walhalla.mmo.core.api.combat.DamageType;
import walhalla.mmo.spells.model.SpellCastType;
import walhalla.mmo.spells.model.SpellSlot;

/**
 * Generates starter spell templates when the project is truly empty.
 * These are structural examples so Phase 3 is playable without manual YML creation.
 */
public final class StarterTemplates {

    private StarterTemplates() {}

    public static void generate(JavaPlugin plugin, File dir) {
        plugin.getLogger().info("No spells found. Generating starter spell templates...");
        write(dir, "MAGO_PRIMARY_1.yml", spell("MAGO_PRIMARY_1", "MAGO: PRIMARIO 1", "MAGO", SpellSlot.PRIMARY, SpellCastType.TARGET_RAY, DamageType.MAGICAL, 3.0, 20, 16, 0.0, null, 0));
        write(dir, "MAGO_SECONDARY_1.yml", spell("MAGO_SECONDARY_1", "MAGO: SECUNDARIO 1", "MAGO", SpellSlot.SECONDARY, SpellCastType.SELF_AOE, DamageType.FIRE, 2.0, 60, 0, 4.0, "BURN", 60));
        write(dir, "MAGO_SPECIAL_1.yml", spell("MAGO_SPECIAL_1", "MAGO: ESPECIAL 1", "MAGO", SpellSlot.SPECIAL, SpellCastType.TARGET_RAY, DamageType.WATER, 2.0, 80, 18, 0.0, "WET", 80));

        write(dir, "GUERRERO_SECONDARY_1.yml", spell("GUERRERO_SECONDARY_1", "GUERRERO: SECUNDARIO 1", "GUERRERO", SpellSlot.SECONDARY, SpellCastType.SELF_AOE, DamageType.PHYSICAL, 2.0, 80, 0, 3.5, null, 0));
        write(dir, "GUERRERO_SPECIAL_1.yml", spell("GUERRERO_SPECIAL_1", "GUERRERO: ESPECIAL 1", "GUERRERO", SpellSlot.SPECIAL, SpellCastType.TARGET_RAY, DamageType.PHYSICAL, 2.5, 120, 10, 0.0, "STUN", 40));

        write(dir, "TANQUE_SECONDARY_1.yml", spell("TANQUE_SECONDARY_1", "TANQUE: SECUNDARIO 1", "TANQUE", SpellSlot.SECONDARY, SpellCastType.SELF_AOE, DamageType.EARTH, 1.5, 100, 0, 4.0, "SLOWED", 80));
        write(dir, "TANQUE_SPECIAL_1.yml", spell("TANQUE_SPECIAL_1", "TANQUE: ESPECIAL 1", "TANQUE", SpellSlot.SPECIAL, SpellCastType.SELF_AOE, DamageType.EARTH, 2.0, 160, 0, 4.0, "ARMOR_BREAK", 80));

        write(dir, "CAZADOR_SECONDARY_1.yml", spell("CAZADOR_SECONDARY_1", "CAZADOR: SECUNDARIO 1", "CAZADOR", SpellSlot.SECONDARY, SpellCastType.TARGET_RAY, DamageType.AIR, 2.0, 80, 20, 0.0, "SLOWED", 60));
        write(dir, "CAZADOR_SPECIAL_1.yml", spell("CAZADOR_SPECIAL_1", "CAZADOR: ESPECIAL 1", "CAZADOR", SpellSlot.SPECIAL, SpellCastType.TARGET_RAY, DamageType.AIR, 2.5, 140, 24, 0.0, null, 0));
    }

    private static YamlConfiguration spell(String id, String name, String branchId, SpellSlot slot, SpellCastType castType,
                                          DamageType damageType, double baseDamage, int cooldownTicks, int range,
                                          double radius, String statusId, int statusDurationTicks) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("spellId", id);
        y.set("displayName", name);
        y.set("branchId", branchId);
        y.set("element", branchId); // legacy catalog field
        y.set("combined", false);
        y.set("slot", slot.name());
        y.set("castType", castType.name());
        y.set("damageType", damageType.name());
        y.set("baseDamage", baseDamage);
        y.set("cooldownTicks", cooldownTicks);
        if (range > 0) y.set("range", range);
        if (radius > 0) y.set("radius", radius);
        if (statusId != null) {
            y.set("status.id", statusId.toUpperCase(Locale.ROOT));
            y.set("status.durationTicks", statusDurationTicks);
        }
        return y;
    }

    private static void write(File dir, String fileName, YamlConfiguration y) {
        try {
            y.save(new File(dir, fileName));
        } catch (IOException e) {
            // best-effort; plugin will just run with fewer spells
        }
    }
}
