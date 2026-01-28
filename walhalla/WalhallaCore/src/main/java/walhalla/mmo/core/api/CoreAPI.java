package walhalla.mmo.core.api;

import org.bukkit.plugin.java.JavaPlugin;
import walhalla.mmo.core.progress.PlayerProgressService;
import walhalla.mmo.core.canon.CanonDataService;
import walhalla.mmo.core.economy.EconomyPriceEngine;

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

    public static Optional<CanonDataService> getCanonService() {
        return Optional.ofNullable(canon);
    }

    public static List<String> getChosenBranches(UUID playerId) {
        if (progress == null || playerId == null) return List.of();
        return progress.getChosenBranches(playerId);
    }

    public static PlayerData.PlayerLifecycleState getLifecycleState(UUID playerId) {
        if (progress == null || playerId == null) return PlayerData.PlayerLifecycleState.UNINITIALIZED;
        return progress.getLifecycleState(playerId);
    }
}
