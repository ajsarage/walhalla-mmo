package walhalla.mmo.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements STATUS_RESPAWN_PROTECTION from CONTRACT_PLAYER_DEATH_RESPAWN_v1
 * using a simple time-based guard.
 */
public class RespawnProtectionService {

    private final Map<UUID, Long> protectedUntilMs = new ConcurrentHashMap<>();

    public void apply(UUID playerId, long durationMs) {
        if (playerId == null) return;
        long until = System.currentTimeMillis() + Math.max(0L, durationMs);
        protectedUntilMs.put(playerId, until);
    }

    public boolean isProtected(UUID playerId) {
        if (playerId == null) return false;
        Long until = protectedUntilMs.get(playerId);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            protectedUntilMs.remove(playerId);
            return false;
        }
        return true;
    }

    /** Cancels protection immediately (e.g. on aggressive action). */
    public void cancel(UUID playerId) {
        if (playerId == null) return;
        protectedUntilMs.remove(playerId);
    }
}
