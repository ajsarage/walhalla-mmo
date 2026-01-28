package walhalla.mmo.bosses.endgame;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.zones.ZoneRulesView;
import walhalla.mmo.core.api.zones.ZoneType;

import java.util.UUID;

/**
 * Endgame combat activity:
 * - Enemies can become ELITE in risky zones.
 * - Promotion happens on first player engagement (first hit) to avoid needing location->zone queries.
 */
public final class ElitePromotionListener implements Listener {

    private final JavaPlugin plugin;
    private final EndgameConfig cfg;

    public ElitePromotionListener(JavaPlugin plugin, EndgameConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!cfg.eliteEnabled) return;
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity mob)) return;
        if (EliteTags.isElite(mob)) return;

        // Only hostile-ish entities: ignore players and non-living already filtered
        // Promotion rule uses zone of attacker.
        ZoneType zoneType = ZoneType.SAFE;
        var zoneIdOpt = CoreAPI.getCurrentZoneId(p.getUniqueId());
        if (zoneIdOpt.isPresent()) {
            var zoneRulesOpt = CoreAPI.getZoneRules(zoneIdOpt.get());
            if (zoneRulesOpt != null) {
                zoneType = zoneRulesOpt.zoneType();
            }
        }

        double chance = switch (zoneType) {
            case BLACK -> cfg.eliteChanceBlack;
            case DANGEROUS -> cfg.eliteChanceDangerous;
            case CONTESTED -> cfg.eliteChanceContested;
            default -> 0.0;
        };
        if (chance <= 0) return;

        if (Math.random() > chance) return;

        promote(mob, zoneType);
    }

    private void promote(LivingEntity mob, ZoneType zoneType) {
        mob.addScoreboardTag("WH_ELITE");

        double healthMult = switch (zoneType) {
            case BLACK -> cfg.eliteHealthMultiplierBlack;
            case DANGEROUS -> cfg.eliteHealthMultiplierDangerous;
            case CONTESTED -> cfg.eliteHealthMultiplierContested;
            default -> 1.0;
        };

        AttributeInstance max = mob.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) {
            double newMax = Math.max(1.0, max.getBaseValue() * healthMult);
            max.setBaseValue(newMax);
            mob.setHealth(Math.min(newMax, mob.getHealth() + (newMax * 0.2)));
        }

        mob.setCustomNameVisible(true);
        String baseName = mob.getType().name();
        mob.setCustomName("§cELITE §7" + baseName);
    }
}
