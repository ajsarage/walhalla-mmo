package walhalla.mmo.combat;

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
    }
}
