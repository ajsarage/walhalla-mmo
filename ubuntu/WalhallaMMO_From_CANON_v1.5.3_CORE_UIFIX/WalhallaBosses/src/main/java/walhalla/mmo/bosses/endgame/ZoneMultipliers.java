package walhalla.mmo.bosses.endgame;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import walhalla.mmo.core.api.zones.ZoneType;

import java.io.File;

/**
 * Reads the same multipliers file used by WalhallaZones (plugins/WalhallaZones/zones_multipliers.yml).
 * If not found, falls back to neutral multipliers (1.0).
 */
public final class ZoneMultipliers {

    private final double lootSafe;
    private final double lootContested;
    private final double lootDangerous;
    private final double lootBlack;

    private ZoneMultipliers(double s, double c, double d, double b) {
        this.lootSafe = s; this.lootContested = c; this.lootDangerous = d; this.lootBlack = b;
    }

    public double lootMultiplier(ZoneType type) {
        if (type == null) return 1.0;
        return switch (type) {
            case SAFE -> lootSafe;
            case CONTESTED -> lootContested;
            case DANGEROUS -> lootDangerous;
            case BLACK -> lootBlack;
        };
    }

    public static ZoneMultipliers loadFromZonesDataFolder(JavaPlugin plugin) {
        try {
            File zonesFolder = new File(plugin.getServer().getPluginsFolder(), "WalhallaZones");
            File f = new File(zonesFolder, "zones_multipliers.yml");
            if (!f.exists()) {
                return new ZoneMultipliers(1.0, 1.0, 1.0, 1.0);
            }
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            double s = y.getDouble("multipliers.SAFE.loot", 1.0);
            double c = y.getDouble("multipliers.CONTESTED.loot", 1.0);
            double d = y.getDouble("multipliers.DANGEROUS.loot", 1.0);
            double b = y.getDouble("multipliers.BLACK.loot", 1.0);
            return new ZoneMultipliers(s, c, d, b);
        } catch (Exception e) {
            return new ZoneMultipliers(1.0, 1.0, 1.0, 1.0);
        }
    }
}
