package walhalla.mmo.core.api.spells;

import java.util.Set;

public interface SpellCatalogBridge {
    Set<String> getAllSpellIds();
    SpellCatalogEntry getSpell(String spellId);
}
