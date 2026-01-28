package walhalla.mmo.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import walhalla.mmo.core.api.combat.DamageType;
import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.zones.ZoneRulesView;

/**
 * Hooks vanilla melee/ranged hits and routes them through CombatService for XP + feedback.
 */
public class BasicAttackListener implements Listener {

    private final CombatService combat;
    private final RespawnProtectionService respawnProtection;

    public BasicAttackListener(CombatService combat, RespawnProtectionService respawnProtection) {
        this.combat = combat;
        this.respawnProtection = respawnProtection;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity le)) return;

        // Any aggressive action cancels respawn protection (CONTRACT_PLAYER_DEATH_RESPAWN_v1)
        if (respawnProtection != null && respawnProtection.isProtected(p.getUniqueId())) {
            respawnProtection.cancel(p.getUniqueId());
        }

        // Zone rules gating (CANON_ZONES_AND_DIFFICULTY_v1 + CONTRACT_ZONES_SERVICE_v1)
        if (le instanceof Player targetPlayer) {
            String zid = CoreAPI.getCurrentZoneId(p.getUniqueId()).orElse(null);
            ZoneRulesView rules = CoreAPI.getZoneRules(zid);
            if (!rules.pvpEnabled()) {
                e.setCancelled(true);
                return;
            }
        }

        // Cancel vanilla damage and re-apply via combat service so we control the pipeline.
        double base = e.getDamage();
        e.setCancelled(true);

        combat.applyDamage(p, le, base, DamageType.PHYSICAL, "BASIC_ATTACK");
    }
}
