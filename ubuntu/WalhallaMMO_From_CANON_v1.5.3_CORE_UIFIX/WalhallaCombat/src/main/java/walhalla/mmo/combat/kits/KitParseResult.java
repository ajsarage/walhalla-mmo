package walhalla.mmo.combat.kits;

import java.util.List;

public record KitParseResult(
        List<CombatKit> kits,
        List<String> warnings
) {}
