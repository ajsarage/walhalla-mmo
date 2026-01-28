package walhalla.mmo.zones.model;

import java.util.Map;

/** Minimal zone definition for Phase 13 (CONTRACT_ZONES_v1). */
public record ZoneDefinition(
        String zoneId,
        String zoneName,
        int version,
        String rulesetId,
        ZoneBox bounds,
        Map<String, Object> metadata
) {}
