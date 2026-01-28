package walhalla.mmo.combat;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.combat.CombatBridge;
import walhalla.mmo.core.api.combat.DamageType;
import walhalla.mmo.core.api.progress.ProgressMutationBridge;

/**
 * Runtime combat authority: applies damage, tracks simple statuses and awards XP consistently.
 *
 * Phase 3 scope:
 * - Basic attack + ability/spell damage application
 * - Simple status container with durations
 * - XP awarding hooks (global/branch/affinity)
 */
public class CombatService implements CombatBridge {

    private final WalhallaCombatPlugin plugin;
    private final StatusEffectManager statusManager;
    private final ProgressMutationBridge progressMut;
    private final RespawnProtectionService respawnProtection;

    private final UUID serverSession = UUID.randomUUID();

    public CombatService(WalhallaCombatPlugin plugin, ProgressMutationBridge progressMut, RespawnProtectionService respawnProtection) {
        this.plugin = plugin;
        this.progressMut = progressMut;
        this.respawnProtection = respawnProtection;
        this.statusManager = new StatusEffectManager(plugin);
    }

    public StatusEffectManager statuses() {
        return statusManager;
    }

    @Override
    public void applyDamage(Player attacker, LivingEntity target, double baseDamage, DamageType type, String sourceId) {
        // Respawn protection (CONTRACT_PLAYER_DEATH_RESPAWN_v1)
        if (target instanceof Player tp && respawnProtection != null && respawnProtection.isProtected(tp.getUniqueId())) {
            return;
        }
        if (attacker == null || target == null) return;
        if (!CoreAPI.getPlayerProgress(attacker.getUniqueId()).isPresent()) return;
        if (!plugin.isPlayerActive(attacker.getUniqueId())) return;

        double finalDamage = Math.max(0.0, baseDamage);

        // Branch multiplier: inferred from weapon + chosen branches.
        String branch = inferCombatBranch(attacker);
        finalDamage *= branchMultiplier(branch);

        // Status interaction: if target is WET and FIRE damage, amplify a bit (no balance claims).
        if (type == DamageType.FIRE && statusManager.hasStatus(target, "WET")) {
            finalDamage *= 1.15;
        }

        // Apply damage using Bukkit to keep compatibility with vanilla mechanics.
        // Ensure a minimum of 0.1 so the hit registers.
        double dmg = Math.max(0.1, finalDamage);
        target.damage(dmg, attacker);

        // Award XP (small per hit; actual balance later). Only ACTIVE players will mutate (guarded in Core).
        if (progressMut != null) {
            progressMut.addGlobalXp(attacker.getUniqueId(), plugin.cfg().xpGlobalPerHit());
            progressMut.addBranchXp(attacker.getUniqueId(), branch, plugin.cfg().xpBranchPerHit());

            String affinity = inferAffinity(attacker);
            if (affinity != null) {
                progressMut.addAffinityXp(attacker.getUniqueId(), affinity, plugin.cfg().xpAffinityPerHit());
            }
        }

        // Combat feedback to attacker (actionbar).
        double hp = target.getHealth();
        double maxHp = Optional.ofNullable(target.getAttribute(Attribute.MAX_HEALTH))
                .map(a -> a.getValue()).orElse(20.0);
        plugin.feedback().sendHitFeedback(attacker, target, dmg, hp, maxHp, type);
    }

    @Override
    public void applyStatus(Player attacker, LivingEntity target, String statusId, int durationTicks, int stacks, String sourceId) {
        if (target == null || statusId == null || statusId.isBlank()) return;
        statusManager.apply(target, statusId.trim().toUpperCase(Locale.ROOT), durationTicks, Math.max(1, stacks));
        if (attacker != null) {
            plugin.feedback().sendStatusFeedback(attacker, target, statusId);
        }
    }

    @Override
    public UUID getCombatSessionId(Player player) {
        return serverSession;
    }

    public String inferCombatBranch(Player player) {
        // If player chose MAGO and is holding a staff-like item, treat as MAGO.
        ItemStack main = player.getInventory().getItemInMainHand();
        Material mat = main != null ? main.getType() : Material.AIR;

        boolean hasMage = CoreAPI.getChosenBranches(player.getUniqueId()).stream()
                .anyMatch(s -> s.equalsIgnoreCase("MAGO"));
        if (hasMage && (mat == Material.BLAZE_ROD || mat == Material.STICK)) return "MAGO";

        // Bow/crossbow -> CAZADOR
        if (mat == Material.BOW || mat == Material.CROSSBOW) return "CAZADOR";

        // Shield in offhand suggests TANQUE
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.getType() == Material.SHIELD) return "TANQUE";

        // Default
        return "GUERRERO";
    }

    private double branchMultiplier(String branch) {
        // Not balance; just a simple structural hook, configurable later.
        if (branch == null) return 1.0;
        return switch (branch.toUpperCase(Locale.ROOT)) {
            case "MAGO" -> plugin.cfg().multMage();
            case "TANQUE" -> plugin.cfg().multTank();
            case "CAZADOR" -> plugin.cfg().multHunter();
            case "GUERRERO" -> plugin.cfg().multWarrior();
            default -> 1.0;
        };
    }

    private String inferAffinity(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main == null) return null;
        Material mat = main.getType();
        // Simple affinity id naming. Can be replaced by weapon system later.
        if (mat == Material.BLAZE_ROD || mat == Material.STICK) return "STAFF";
        if (mat.name().endsWith("_SWORD") || mat.name().endsWith("_AXE") || mat == Material.MACE) return "MELEE";
        if (mat == Material.BOW || mat == Material.CROSSBOW) return "RANGED";
        return null;
    }
}
