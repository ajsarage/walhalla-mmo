package walhalla.mmo.core.api.spells;

public record SpellView(
        String spellId,
        String displayName,
        String element,
        boolean combined
) {}
