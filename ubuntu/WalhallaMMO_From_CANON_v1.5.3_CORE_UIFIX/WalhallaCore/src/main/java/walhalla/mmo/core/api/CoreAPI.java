package walhalla.mmo.core.api;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.progress.*;
import walhalla.mmo.core.api.spells.*;
import walhalla.mmo.core.progress.PlayerProgressService;
import walhalla.mmo.core.canon.CanonDataService;
import walhalla.mmo.core.canon.CanonXpCaps;
import walhalla.mmo.core.economy.EconomyPriceEngine;
import walhalla.mmo.core.economy.PriceQuote;
import walhalla.mmo.core.api.zones.*;

/**
 * Core API (authority). UI and other plugins must treat this as read-only unless a contract says otherwise.
 *
 * Contracts:
 * - CONTRACT_COREAPI_PROGRESS_READONLY_v1
 * - CONTRACT_Z74_UI_SPELL_CATALOG_BRIDGE_v1
 */
public final class CoreAPI {

    private static JavaPlugin plugin;
    private static PlayerProgressService progress;
    private static CanonDataService canon;
    private static EconomyPriceEngine economyPrices;

    private CoreAPI() {}

    public static void bootstrap(JavaPlugin owningPlugin, PlayerProgressService progressService) {
        plugin = owningPlugin;
        progress = progressService;
    }
    /** Phase 7: Canon data service (data-driven core). */
    public static void setCanonService(CanonDataService canonService) {
        canon = canonService;
    }

    public static Optional<CanonDataService> getCanonService() {
        return Optional.ofNullable(canon);
    }

    public static Optional<CanonXpCaps> getCanonXpCaps() {
        return Optional.ofNullable(canon).map(CanonDataService::xpCaps);
    }

    public static Set<String> getCanonItemIds() {
        return canon == null ? Set.of() : canon.index().itemIds();
    }

    public static Set<String> getCanonStationIds() {
        return canon == null ? Set.of() : canon.index().stationIds();
    }

    // ------------------------------------------------------------------
    // ------------------------------------------------------------------
    // Economy prices (READ-ONLY) — Phase 12
    // ------------------------------------------------------------------

    public static void setEconomyPriceEngine(EconomyPriceEngine engine) {
        economyPrices = engine;
    }

    // ------------------------------------------------------------------
    // Zones (READ-ONLY) — Phase 13
    // ------------------------------------------------------------------
/** NPC sell quote for a given canonical itemId. */
    public static PriceQuote quoteNpcSell(String itemId, int tier, int quality) {
        if (economyPrices == null) return PriceQuote.of(walhalla.mmo.core.api.economy.Currency.WCOIN, 0L);
        return economyPrices.quoteNpcSell(itemId, tier, quality);
    }

    /** Station use fee quote derived from canon sink rules. */
    public static PriceQuote quoteStationUseFee(int tier) {
        if (economyPrices == null) return PriceQuote.of(walhalla.mmo.core.api.economy.Currency.WCOIN, 0L);
        return economyPrices.quoteStationUseFee(tier);
    }

// Progress (READ-ONLY) — CONTRACT_COREAPI_PROGRESS_READONLY_v1
    // ------------------------------------------------------------------

    /**
     * Legacy helper (kept for compatibility with early bootstrap UI).
     */
    public static Optional<ProgressView> getProgress(UUID playerId) {
        if (progress == null) return Optional.empty();
        PlayerProgressView p = progress.getPlayerProgress(playerId);
        // Provide a minimal view that older UIs can display.
        return Optional.of(new ProgressView(
                p.level(),
                p.totalXp(),
                Math.max(0L, p.xpForNextLevel() - p.xpIntoLevel()),
                Map.of(),
                Map.of()
        ));
    }

    public static Optional<PlayerProgressView> getPlayerProgress(UUID playerId) {
        if (progress == null) return Optional.empty();
        return Optional.of(progress.getPlayerProgress(playerId));
    }

    public static List<BranchProgressView> getAllBranchProgress(UUID playerId) {
        if (progress == null) return List.of();
        return progress.getAllBranchProgress(playerId);
    }

    public static List<String> getChosenBranches(UUID playerId) {
        if (progress == null) return List.of();
        return progress.getChosenBranches(playerId);
    }

    public static walhalla.mmo.core.progress.PlayerData.PlayerLifecycleState getLifecycleState(UUID playerId) {
        if (progress == null) return walhalla.mmo.core.progress.PlayerData.PlayerLifecycleState.UNINITIALIZED;
        return progress.getLifecycleState(playerId);
    }

    public static Optional<BranchProgressView> getBranchProgress(UUID playerId, String branchId) {
        if (progress == null) return Optional.empty();
        if (branchId == null || branchId.isBlank()) return Optional.empty();
        return Optional.of(progress.getBranchProgress(playerId, branchId.trim()));
    }

    public static List<WeaponAffinityProgressView> getWeaponAffinities(UUID playerId) {
        if (progress == null) return List.of();
        return progress.getWeaponAffinities(playerId);
    }

    public static List<ProfessionProgressView> getAllProfessionProgress(UUID playerId) {
        if (progress == null) return List.of();
        return progress.getAllProfessionProgress(playerId);
    }

    public static Optional<ProfessionProgressView> getProfessionProgress(UUID playerId, String professionId) {
        if (progress == null) return Optional.empty();
        if (professionId == null || professionId.isBlank()) return Optional.empty();
        return Optional.of(progress.getProfessionProgress(playerId, professionId.trim()));
    }


    public static Optional<AvailablePointsView> getAvailableRpgPoints(UUID playerId) {
        if (progress == null) return Optional.empty();
        return Optional.of(progress.getAvailableRpgPoints(playerId));
    }

    /**
     * Read-only node cost lookup (CONTRACT_COREAPI_PROGRESS_READONLY_v1).
     *
     * Nota canónica:
     * - El contrato exige este método para visualización en UI.
     * - El canon actual NO aporta una tabla numérica cerrada de costes de nodos (CONTRACT_PROGRESSION_UNLOCKS_v1 está abierto).
     * - Por lo tanto, mientras no exista una fuente canónica explícita, este método expone un valor determinista "desconocido" (0).
     */
    public static int getNodeCost(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return 0;
        // Fase actual: sin tabla de costes canónica consumible por Core.
        return 0;
    }


    public static Optional<SubBranchProgressView> getSubBranchProgress(UUID playerId, String subBranchId) {
        if (progress == null) return Optional.empty();
        if (subBranchId == null || subBranchId.isBlank()) return Optional.empty();
        return Optional.of(progress.getSubBranchProgress(playerId, subBranchId.trim()));
    }

    // ------------------------------------------------------------------
    // Spells catalog (READ-ONLY) — CONTRACT_Z74_UI_SPELL_CATALOG_BRIDGE_v1
    // ------------------------------------------------------------------

    public static List<SpellView> getAvailableSpells(UUID playerId) {
        SpellCatalogBridge bridge = Bukkit.getServicesManager().load(SpellCatalogBridge.class);
        if (bridge == null) return List.of();
        List<SpellView> out = new ArrayList<>();
        for (String id : bridge.getAllSpellIds()) {
            SpellCatalogEntry e = bridge.getSpell(id);
            if (e != null) {
                out.add(new SpellView(e.spellId(), e.displayName(), e.element(), e.combined()));
            }
        }
        out.sort(Comparator.comparing(SpellView::spellId));
        return out;
    }

    /**
     * UI helper required by bridge contract.
     * NOTE: Real gating depends on later progression/unlock contracts.
     */
    public static MenuEntryState getMenuEntryState(UUID playerId, String spellId) {
        return MenuEntryState.AVAILABLE;
    }

    // ------------------------------------------------------------------
    // Zones (READ-ONLY) — CONTRACT_ZONES_SERVICE_v1
    // ------------------------------------------------------------------

    /** Returns current zone id for an online player, or empty if Zones service is not present. */
    public static Optional<String> getCurrentZoneId(UUID playerId) {
        ZoneService zs = Bukkit.getServicesManager().load(ZoneService.class);
        if (zs == null) return Optional.empty();
        return Optional.ofNullable(zs.getCurrentZoneId(playerId));
    }

    /** Returns expanded rules for a zone, or a SAFE fallback if service is unavailable. */
    public static ZoneRulesView getZoneRules(String zoneId) {
        ZoneService zs = Bukkit.getServicesManager().load(ZoneService.class);
        if (zs == null) return ZoneRulesView.safeFallback();
        return zs.getZoneRules(zoneId);
    }
}
