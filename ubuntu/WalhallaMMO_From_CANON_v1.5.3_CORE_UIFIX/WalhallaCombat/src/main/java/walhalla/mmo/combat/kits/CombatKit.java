package walhalla.mmo.combat.kits;

public record CombatKit(
        String kitId,
        String branchId,
        String displayName,
        Ability basic,
        Ability secondary,
        Ability special
) {}
