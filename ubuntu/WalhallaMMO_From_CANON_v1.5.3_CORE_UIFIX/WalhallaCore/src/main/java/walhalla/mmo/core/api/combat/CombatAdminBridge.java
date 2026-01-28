package walhalla.mmo.core.api.combat;

import java.util.Optional;
import java.util.UUID;

/**
 * OP/Admin bridge for combat systems (kits reload/inspect) without exposing player commands.
 * Implemented and registered by WalhallaCombat.
 */
public interface CombatAdminBridge {

    /** Reload combat kit data from the canon annexes. Returns true on success. */
    boolean reloadKits();

    /** Human-readable status for /walhalla combat status. */
    String getStatus();

    /** Assign a kit to a player (debug/admin). */
    boolean setPlayerKit(UUID playerId, String kitId);

    /** Current kit id for player, if any. */
    Optional<String> getPlayerKit(UUID playerId);
}
