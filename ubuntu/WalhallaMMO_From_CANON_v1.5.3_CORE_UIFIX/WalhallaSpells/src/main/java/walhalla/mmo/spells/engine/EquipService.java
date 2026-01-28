package walhalla.mmo.spells.engine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import walhalla.mmo.spells.model.SpellSlot;

/**
 * Stores equipped spell ids per player per slot.
 * Persistence will be moved to Core when the canon defines the exact format.
 */
public class EquipService {

    private final Map<UUID, EnumMap<SpellSlot, String>> equipped = new ConcurrentHashMap<>();

    public String getEquipped(UUID playerId, SpellSlot slot) {
        return equipped.getOrDefault(playerId, new EnumMap<>(SpellSlot.class)).get(slot);
    }

    public void setEquipped(UUID playerId, SpellSlot slot, String spellId) {
        equipped.computeIfAbsent(playerId, k -> new EnumMap<>(SpellSlot.class)).put(slot, spellId);
    }

    public Map<SpellSlot, String> getAll(UUID playerId) {
        return Collections.unmodifiableMap(equipped.getOrDefault(playerId, new EnumMap<>(SpellSlot.class)));
    }
}
