package walhalla.mmo.core.api.zones;

import java.util.List;
import java.util.Map;

/** Expanded zone rules for other systems (read-only). */
public record ZoneRulesView(
        ZoneType zoneType,
        boolean combatAllowed,
        boolean pvpEnabled,
        boolean friendlyFireEnabled,
        List<String> professionsAllowed,
        List<String> allowedTools,
        DeathPenalty deathPenalty,
        long respawnProtectionDurationMs,
        String ccRulesetId,
        boolean economyEnabled,
        Map<String, Boolean> zoneSpecificFlags
) {
    public static ZoneRulesView safeFallback() {
        return new ZoneRulesView(
                ZoneType.SAFE,
                true,
                false,
                false,
                List.of(),
                List.of(),
                DeathPenalty.NONE,
                10_000L,
                "DEFAULT",
                true,
                Map.of()
        );
    }
}
