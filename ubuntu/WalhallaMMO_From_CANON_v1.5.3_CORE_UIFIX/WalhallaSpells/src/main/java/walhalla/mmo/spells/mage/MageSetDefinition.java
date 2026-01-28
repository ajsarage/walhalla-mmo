package walhalla.mmo.spells.mage;

import walhalla.mmo.spells.model.SpellSlot;

/** Immutable mage "Warframe-style" set definition (Phase 10). */
public record MageSetDefinition(
        String setId,
        String elementId,
        String primarySpellId,
        String secondarySpellId,
        String specialSpellId
) {
    public String spellForSlot(SpellSlot slot) {
        return switch (slot) {
            case PRIMARY -> primarySpellId;
            case SECONDARY -> secondarySpellId;
            case SPECIAL -> specialSpellId;
            case ULTIMATE -> null;
        };
    }
}
