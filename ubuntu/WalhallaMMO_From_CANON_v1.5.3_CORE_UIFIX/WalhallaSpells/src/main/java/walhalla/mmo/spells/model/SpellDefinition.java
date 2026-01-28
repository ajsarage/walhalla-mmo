package walhalla.mmo.spells.model;

import walhalla.mmo.core.api.combat.DamageType;

public record SpellDefinition(
        String id,
        String displayName,
        String branchId,
        SpellSlot slot,
        SpellCastType castType,
        DamageType damageType,
        double baseDamage,
        int cooldownTicks,
        int range,
        double radius,
        String statusId,
        int statusDurationTicks
) {}
