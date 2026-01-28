package walhalla.mmo.core.api.zones;

import java.util.Map;

/**
 * Minimal zone definition view (CONTRACT_ZONES_SERVICE_v1).
 */
public record ZoneDefinitionView(
        String zoneId,
        String zoneName,
        int version,
        String rulesetId,
        ZoneType zoneType,
        Map<String, Object> metadata
) {}
