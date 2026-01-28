package walhalla.mmo.zones.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import walhalla.mmo.core.api.zones.*;
import walhalla.mmo.zones.model.ZoneDefinition;
import walhalla.mmo.zones.model.ZoneRuleset;

public class ZonesServiceImpl implements ZoneService {

    private final Map<String, ZoneDefinition> zonesById;
    private final Map<String, ZoneRuleset> rulesetsById;
    private final Map<UUID, String> currentZone = new ConcurrentHashMap<>();

    public ZonesServiceImpl(Map<String, ZoneDefinition> zonesById, Map<String, ZoneRuleset> rulesetsById) {
        this.zonesById = Map.copyOf(zonesById);
        this.rulesetsById = Map.copyOf(rulesetsById);
    }

    public void updatePlayerZone(UUID playerId, String zoneId) {
        if (playerId == null) return;
        if (zoneId == null) currentZone.remove(playerId);
        else currentZone.put(playerId, zoneId);
    }

    @Override
    public String getCurrentZoneId(UUID playerId) {
        if (playerId == null) return null;
        return currentZone.get(playerId);
    }

    @Override
    public ZoneRulesView getZoneRules(String zoneId) {
        ZoneDefinition zd = zoneId == null ? null : zonesById.get(zoneId);
        if (zd == null) {
            return ZoneRulesView.safeFallback();
        }
        ZoneRuleset rs = rulesetsById.get(zd.rulesetId());
        if (rs == null) {
            return ZoneRulesView.safeFallback();
        }
        return new ZoneRulesView(
                ZoneType.SAFE,
                rs.combatAllowed(),
                rs.pvpEnabled(),
                rs.friendlyFireEnabled(),
                rs.professionsAllowed() == null ? List.of() : rs.professionsAllowed(),
                rs.allowedTools() == null ? List.of() : rs.allowedTools(),
                rs.deathPenalty() == null ? DeathPenalty.NONE : rs.deathPenalty(),
                rs.respawnProtectionDurationMs(),
                rs.ccRulesetId() == null ? "DEFAULT" : rs.ccRulesetId(),
                rs.economyEnabled(),
                rs.zoneSpecificFlags() == null ? Map.of() : rs.zoneSpecificFlags()
        );
    }

    @Override
    public ZoneType getZoneType(String zoneId) {
        ZoneDefinition zd = zoneId == null ? null : zonesById.get(zoneId);
        return zd == null ? ZoneType.SAFE : ZoneType.SAFE;
    }

    @Override
    public Set<String> getAllZoneIds() {
        return zonesById.keySet();
    }

    public Optional<ZoneDefinition> getZoneDefinition(String zoneId) {
        return Optional.ofNullable(zonesById.get(zoneId));
    }

    /** Computes zone id at the player's location using first-match bounds. */
    public String resolveZoneIdFor(Player p) {
        if (p == null) return null;
        for (ZoneDefinition z : zonesById.values()) {
            if (z.bounds() == null) continue;
            if (z.bounds().contains(p.getLocation())) return z.zoneId();
        }
        // If none matches, fallback to a global SAFE zone if defined.
        if (zonesById.containsKey("global_safe")) return "global_safe";
        return null;
    }
}
