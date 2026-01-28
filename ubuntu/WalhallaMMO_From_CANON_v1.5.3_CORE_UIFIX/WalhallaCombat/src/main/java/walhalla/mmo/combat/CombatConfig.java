package walhalla.mmo.combat;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Combat configuration (NOT balance claims; purely configurable knobs).
 */
public class CombatConfig {

    private final FileConfiguration cfg;

    public CombatConfig(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    public long xpGlobalPerHit() { return Math.max(0L, cfg.getLong("xp.globalPerHit", 1L)); }
    public long xpBranchPerHit() { return Math.max(0L, cfg.getLong("xp.branchPerHit", 1L)); }
    public long xpAffinityPerHit() { return Math.max(0L, cfg.getLong("xp.affinityPerHit", 1L)); }

    public double multWarrior() { return cfg.getDouble("multiplier.GUERRERO", 1.0); }
    public double multTank() { return cfg.getDouble("multiplier.TANQUE", 1.0); }
    public double multHunter() { return cfg.getDouble("multiplier.CAZADOR", 1.0); }
    public double multMage() { return cfg.getDouble("multiplier.MAGO", 0.85); }
}
