package walhalla.mmo.combat;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import walhalla.mmo.core.api.CombatBridge;
import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.progress.PlayerProgressService;

public class CombatService implements CombatBridge {
    private final WalhallaCombatPlugin plugin;
    private final PlayerProgressService progressMut;
    private final RespawnProtectionService respawnProtection;

    public CombatService(WalhallaCombatPlugin plugin, PlayerProgressService progressMut, RespawnProtectionService respawnProtection) {
        this.plugin = plugin;
        this.progressMut = progressMut;
        this.respawnProtection = respawnProtection;
    }

    @Override
    public void applyDamage(Player attacker, LivingEntity target, double baseDamage, DamageType type, String source) {
        // Implementation logic here
        if (respawnProtection.isProtected(target)) {
            return;
        }
        
        double finalDamage = calculateFinalDamage(baseDamage, type);
        target.damage(finalDamage, attacker);
    }

    private double calculateFinalDamage(double baseDamage, DamageType type) {
        // Additional logic to calculate final damage based on type
        switch (type) {
            case MELEE:
                return baseDamage * 1.2;
            case RANGED:
                return baseDamage * 0.8;
            default:
                return baseDamage;
        }
    }
}
