package walhalla.mmo.spells.engine;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import walhalla.mmo.core.api.spells.SpellLoadoutBridge;
import walhalla.mmo.spells.model.SpellSlot;

public class SpellLoadoutBridgeImpl implements SpellLoadoutBridge {

    private final EquipService equips;

    public SpellLoadoutBridgeImpl(EquipService equips) {
        this.equips = equips;
    }

    @Override
    public Optional<String> getEquipped(UUID playerId, String slotId) {
        SpellSlot slot = parse(slotId);
        if (slot == null) return Optional.empty();
        return Optional.ofNullable(equips.getEquipped(playerId, slot));
    }

    @Override
    public boolean setEquipped(UUID playerId, String slotId, String spellId) {
        SpellSlot slot = parse(slotId);
        if (slot == null) return false;
        if (spellId == null || spellId.isBlank()) return false;
        equips.setEquipped(playerId, slot, spellId.trim());
        return true;
    }

    private SpellSlot parse(String slotId) {
        if (slotId == null || slotId.isBlank()) return null;
        try {
            return SpellSlot.valueOf(slotId.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
