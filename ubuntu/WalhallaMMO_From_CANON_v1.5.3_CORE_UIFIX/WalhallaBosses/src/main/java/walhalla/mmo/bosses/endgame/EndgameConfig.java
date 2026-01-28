package walhalla.mmo.bosses.endgame;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class EndgameConfig {

    public final boolean overrideVanillaDrops;
    public final boolean eliteEnabled;

    public final double eliteChanceContested;
    public final double eliteChanceDangerous;
    public final double eliteChanceBlack;

    public final double eliteHealthMultiplierContested;
    public final double eliteHealthMultiplierDangerous;
    public final double eliteHealthMultiplierBlack;

    public final double eliteExtraLootRollsContested;
    public final double eliteExtraLootRollsDangerous;
    public final double eliteExtraLootRollsBlack;

    public final boolean worldBossesEnabled;
    public final List<WorldBossSpec> worldBosses;

    private EndgameConfig(
            boolean overrideVanillaDrops,
            boolean eliteEnabled,
            double eliteChanceContested,
            double eliteChanceDangerous,
            double eliteChanceBlack,
            double eliteHealthMultiplierContested,
            double eliteHealthMultiplierDangerous,
            double eliteHealthMultiplierBlack,
            double eliteExtraLootRollsContested,
            double eliteExtraLootRollsDangerous,
            double eliteExtraLootRollsBlack,
            boolean worldBossesEnabled,
            List<WorldBossSpec> worldBosses
    ) {
        this.overrideVanillaDrops = overrideVanillaDrops;
        this.eliteEnabled = eliteEnabled;
        this.eliteChanceContested = eliteChanceContested;
        this.eliteChanceDangerous = eliteChanceDangerous;
        this.eliteChanceBlack = eliteChanceBlack;
        this.eliteHealthMultiplierContested = eliteHealthMultiplierContested;
        this.eliteHealthMultiplierDangerous = eliteHealthMultiplierDangerous;
        this.eliteHealthMultiplierBlack = eliteHealthMultiplierBlack;
        this.eliteExtraLootRollsContested = eliteExtraLootRollsContested;
        this.eliteExtraLootRollsDangerous = eliteExtraLootRollsDangerous;
        this.eliteExtraLootRollsBlack = eliteExtraLootRollsBlack;
        this.worldBossesEnabled = worldBossesEnabled;
        this.worldBosses = worldBosses;
    }

    public static EndgameConfig load(JavaPlugin plugin) {
        boolean overrideDrops = plugin.getConfig().getBoolean("endgame.overrideVanillaDrops", true);
        boolean eliteEnabled = plugin.getConfig().getBoolean("endgame.elites.enabled", true);

        double cC = plugin.getConfig().getDouble("endgame.elites.chance.CONTESTED", 0.08);
        double cD = plugin.getConfig().getDouble("endgame.elites.chance.DANGEROUS", 0.15);
        double cB = plugin.getConfig().getDouble("endgame.elites.chance.BLACK", 0.25);

        double hC = plugin.getConfig().getDouble("endgame.elites.healthMultiplier.CONTESTED", 1.25);
        double hD = plugin.getConfig().getDouble("endgame.elites.healthMultiplier.DANGEROUS", 1.6);
        double hB = plugin.getConfig().getDouble("endgame.elites.healthMultiplier.BLACK", 2.0);

        double rC = plugin.getConfig().getDouble("endgame.elites.extraLootRolls.CONTESTED", 1.0);
        double rD = plugin.getConfig().getDouble("endgame.elites.extraLootRolls.DANGEROUS", 2.0);
        double rB = plugin.getConfig().getDouble("endgame.elites.extraLootRolls.BLACK", 3.0);

        boolean wbEnabled = plugin.getConfig().getBoolean("endgame.worldBosses.enabled", false);

        List<WorldBossSpec> bosses = new ArrayList<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("endgame.worldBosses.list");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection b = sec.getConfigurationSection(key);
                if (b == null) continue;
                bosses.add(WorldBossSpec.fromSection(key, b));
            }
        }

        return new EndgameConfig(overrideDrops, eliteEnabled, cC, cD, cB, hC, hD, hB, rC, rD, rB, wbEnabled, bosses);
    }
}
