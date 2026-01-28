package walhalla.mmo.core.api.combat;

import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Cross-plugin combat bridge (Combat is the runtime authority).
 * Spells/Weapons call this to apply damage/status and award XP consistently.
 *
 * Player-facing commands are forbidden; this is internal API.
 */
public interface CombatBridge {

    /**
     * Applies combat damage from attacker -> target with the given damage type.
     * Implementations must:
     * - respect Core lifecycle state (only ACTIVE players can mutate XP)
     * - apply branch/affinity logic
     * - award XP where relevant
     */
    void applyDamage(Player attacker, LivingEntity target, double baseDamage, DamageType type, String sourceId);

    /**
     * Applies a status effect to the target (stacking rules are implementation-defined).
     */
    void applyStatus(Player attacker, LivingEntity target, String statusId, int durationTicks, int stacks, String sourceId);

    /**
     * Utility for UI/debug.
     */
    UUID getCombatSessionId(Player player);
}
