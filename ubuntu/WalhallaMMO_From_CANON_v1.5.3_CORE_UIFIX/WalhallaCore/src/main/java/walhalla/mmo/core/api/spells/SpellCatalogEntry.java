package walhalla.mmo.core.api.spells;

public record SpellCatalogEntry(
        String spellId,
        String displayName,
        String element,
        boolean combined
) {}
