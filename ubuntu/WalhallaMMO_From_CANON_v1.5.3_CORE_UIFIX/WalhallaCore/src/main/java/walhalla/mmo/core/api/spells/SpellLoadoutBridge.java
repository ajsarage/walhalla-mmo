package walhalla.mmo.core.api.spells;

import java.util.Optional;
import java.util.UUID;

/**
 * Bridge for equipping spells into slots.
 * This is NOT a player command path; UI uses it to request equip changes.
 */
public interface SpellLoadoutBridge {
    Optional<String> getEquipped(UUID playerId, String slotId);
    boolean setEquipped(UUID playerId, String slotId, String spellId);
}
