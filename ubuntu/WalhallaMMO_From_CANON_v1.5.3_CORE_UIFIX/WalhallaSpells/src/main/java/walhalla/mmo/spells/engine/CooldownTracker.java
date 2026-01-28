package walhalla.mmo.spells.engine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

/**
 * Simple per-player per-spell cooldown tracker.
 */
public class CooldownTracker {

    private final Map<UUID, Map<String, Long>> lastCastTick = new ConcurrentHashMap<>();

    public boolean isOnCooldown(UUID playerId, String spellId, int cooldownTicks) {
        if (cooldownTicks <= 0) return false;
        long now = Bukkit.getCurrentTick();
        long last = lastCastTick.getOrDefault(playerId, Map.of()).getOrDefault(spellId, -999999L);
        return (now - last) < cooldownTicks;
    }

    public void markCast(UUID playerId, String spellId) {
        long now = Bukkit.getCurrentTick();
        lastCastTick.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(spellId, now);
    }
}
