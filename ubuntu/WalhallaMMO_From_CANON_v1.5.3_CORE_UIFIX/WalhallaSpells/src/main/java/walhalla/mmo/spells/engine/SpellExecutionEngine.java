package walhalla.mmo.spells.engine;

import java.util.Locale;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.combat.CombatBridge;
import walhalla.mmo.spells.catalog.YmlSpellCatalog;
import walhalla.mmo.spells.model.*;

/**
 * Executes spells using CombatBridge for damage/status and Core for state.
 */
public class SpellExecutionEngine {

    private final YmlSpellCatalog catalog;
    private final CooldownTracker cooldowns;
    private final EquipService equips;

    public SpellExecutionEngine(YmlSpellCatalog catalog, CooldownTracker cooldowns, EquipService equips) {
        this.catalog = catalog;
        this.cooldowns = cooldowns;
        this.equips = equips;
    }

    public boolean castEquipped(Player p, SpellSlot slot) {
        if (p == null) return false;
        if (!CoreAPI.getPlayerProgress(p.getUniqueId()).isPresent()) return false;

        String spellId = equips.getEquipped(p.getUniqueId(), slot);
        if (spellId == null) return false;

        Optional<SpellDefinition> defOpt = catalog.getDefinition(spellId);
        if (defOpt.isEmpty()) return false;
        SpellDefinition def = defOpt.get();

        // Cooldown gate
        if (cooldowns.isOnCooldown(p.getUniqueId(), def.id(), def.cooldownTicks())) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.4f, 0.6f);
            return true; // handled (feedback)
        }

        CombatBridge combat = Bukkit.getServicesManager().load(CombatBridge.class);
        if (combat == null) {
            p.sendMessage("§c[Walhalla] CombatBridge no disponible.");
            return false;
        }

        // Execute
        boolean success = switch (def.castType()) {
            case TARGET_RAY -> castRay(p, def, combat);
            case SELF_AOE -> castAoe(p, def, combat);
        };

        if (success) {
            cooldowns.markCast(p.getUniqueId(), def.id());
        }
        return success;
    }

    private boolean castRay(Player p, SpellDefinition def, CombatBridge combat) {
        int range = Math.max(2, def.range());
        RayTraceResult res = p.rayTraceEntities(range);
        if (res == null) {
            feedbackNoTarget(p);
            return true;
        }
        Entity hit = res.getHitEntity();
        if (!(hit instanceof LivingEntity le) || hit == p) {
            feedbackNoTarget(p);
            return true;
        }

        // VFX/SFX minimal
        p.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1.0, 0), 12, 0.2, 0.4, 0.2, 0.02);
        p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.7f, 1.2f);

        combat.applyDamage(p, le, def.baseDamage(), def.damageType(), def.id());
        if (def.statusId() != null && def.statusDurationTicks() > 0) {
            combat.applyStatus(p, le, def.statusId(), def.statusDurationTicks(), 1, def.id());
        }
        return true;
    }

    private boolean castAoe(Player p, SpellDefinition def, CombatBridge combat) {
        double radius = Math.max(1.0, def.radius());
        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(0, 1.0, 0), 10, radius/4, 0.4, radius/4, 0.02);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.1f);

        boolean hitAny = false;
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity le && le != p) {
                hitAny = true;
                combat.applyDamage(p, le, def.baseDamage(), def.damageType(), def.id());
                if (def.statusId() != null && def.statusDurationTicks() > 0) {
                    combat.applyStatus(p, le, def.statusId(), def.statusDurationTicks(), 1, def.id());
                }
            }
        }
        if (!hitAny) {
            feedbackNoTarget(p);
        }
        return true;
    }

    private void feedbackNoTarget(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.25f, 1.8f);
        p.sendActionBar(net.kyori.adventure.text.Component.text("NO_TARGETS", net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }
}
