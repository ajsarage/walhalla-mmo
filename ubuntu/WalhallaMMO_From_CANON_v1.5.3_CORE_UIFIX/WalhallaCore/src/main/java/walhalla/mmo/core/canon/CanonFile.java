package walhalla.mmo.core.canon;

import java.util.Arrays;
import java.util.Locale;

/**
 * Canon data files embedded in WalhallaCore and extracted to disk on first run.
 * Phase 7: Core becomes fully data-driven (consumes Canon annexes).
 */
public enum CanonFile {
    INDEX_GENERAL("INDEX_GENERAL_CANON.txt", true),

    ANNEX_XP_CURVES("ANNEX_XP_CURVES_ALL_v1.txt", true),
    ANNEX_EQUIPMENT_TIERS("ANNEX_EQUIPMENT_TIERS_v1.txt", true),
    ANNEX_TOOL_PROGRESSION("ANNEX_TOOL_PROGRESSION_v1.txt", true),

    ANNEX_ITEM_CATALOG("ANNEX_ITEM_CATALOG_ALL_v1.txt", true),
    ANNEX_GATHERING_TABLE("ANNEX_GATHERING_RESOURCE_TABLE_v1.txt", true),

    ANNEX_CRAFTING_STATIONS("ANNEX_CRAFTING_STATIONS_v1.txt", true),
    ANNEX_REFINING_RECIPES("ANNEX_REFINING_RECIPES_v1.txt", true),
    ANNEX_CRAFTING_RECIPES("ANNEX_CRAFTING_RECIPES_ALL_v1.txt", true),

    ANNEX_ECONOMY_PRICES("ANNEX_ECONOMY_PRICES_BASE_v1.txt", true),
    ANNEX_ECONOMY_SINKS("ANNEX_ECONOMY_SINKS_v1.txt", true),

    ANNEX_COMBAT_KITS_WARRIOR("ANNEX_COMBAT_KITS_WARRIOR_v1.txt", true),
    ANNEX_COMBAT_KITS_TANK("ANNEX_COMBAT_KITS_TANK_v1.txt", true),
    ANNEX_COMBAT_KITS_HUNTER("ANNEX_COMBAT_KITS_HUNTER_v1.txt", true),

    ANNEX_MAGE_SETS("ANNEX_MAGE_WARFRAME_SETS_v1.txt", true),
    ANNEX_GAMEPLAY_LOOP("ANNEX_GAMEPLAY_LOOP_AND_ENDGAME_v1.txt", true),

    // Optional / future-facing annexes (still extracted so server data folder is complete)
    ANNEX_ENTITY_LOOT("ANNEX_ENTITY_DROPS_AND_LOOT_TABLES_v1.txt", false),
    ANNEX_ENTITY_LOOT_1_1("ANNEX_ENTITY_DROPS_AND_LOOT_TABLES_v1.1.txt", false),
    ANNEX_ECONOMY_WCRAFT_POLICY("ANNEX_ECONOMY_WCRAFT_POLICY_v1.txt", false),
    ANNEX_ECONOMY_WCRAFT_TRADE("ANNEX_ECONOMY_WCRAFT_TRADE_POLICY_v1.txt", false),
    ANNEX_ECONOMY_WCRAFT_WF("ANNEX_ECONOMY_WCRAFT_WARFRAME_MODEL_v1.txt", false);

    private final String fileName;
    private final boolean required;

    CanonFile(String fileName, boolean required) {
        this.fileName = fileName;
        this.required = required;
    }

    public String fileName() { return fileName; }
    public boolean required() { return required; }

    public static CanonFile fromFileName(String fileName) {
        if (fileName == null) return null;
        String f = fileName.trim();
        return Arrays.stream(values())
                .filter(v -> v.fileName.equalsIgnoreCase(f))
                .findFirst()
                .orElse(null);
    }

    public String resourcePath() {
        return "canon/" + fileName;
    }

    public static String[] requiredFileNames() {
        return Arrays.stream(values()).filter(CanonFile::required).map(CanonFile::fileName).toArray(String[]::new);
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT) + "(" + fileName + ")";
    }
}
