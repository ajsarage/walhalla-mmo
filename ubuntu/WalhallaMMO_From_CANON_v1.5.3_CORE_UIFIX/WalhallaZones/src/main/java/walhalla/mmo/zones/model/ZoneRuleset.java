package walhalla.mmo.zones.model;

import java.util.List;
import java.util.Map;
import walhalla.mmo.core.api.zones.DeathPenalty;
import walhalla.mmo.core.api.zones.ZoneType;

/** Zone ruleset (CONTRACT_ZONE_RULESETS_v1). */
public record ZoneRuleset(
        String rulesetId,
        String name,
        ZoneType zoneType,
        boolean pvpEnabled,
        boolean friendlyFireEnabled,
        boolean combatAllowed,
        List<String> professionsAllowed,
        List<String> allowedTools,
        DeathPenalty deathPenalty,
        long respawnProtectionDurationMs,
        String ccRulesetId,
        boolean economyEnabled,
        Map<String, Boolean> zoneSpecificFlags
) {}
