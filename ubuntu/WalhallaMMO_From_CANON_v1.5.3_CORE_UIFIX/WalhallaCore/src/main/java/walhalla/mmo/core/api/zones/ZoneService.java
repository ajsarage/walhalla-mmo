package walhalla.mmo.core.api.zones;

import java.util.Set;
import java.util.UUID;

/**
 * Read-only zones service (CONTRACT_ZONES_SERVICE_v1).
 * Implementations should be registered via Bukkit ServicesManager.
 */
public interface ZoneService {

    /** @return current zone id for player, or null if unknown */
    String getCurrentZoneId(UUID playerId);

    /** @return expanded rules for a zone; must never return null */
    ZoneRulesView getZoneRules(String zoneId);

    /** @return risk type for a zone */
    ZoneType getZoneType(String zoneId);

    /** @return all known zone ids */
    Set<String> getAllZoneIds();
}
