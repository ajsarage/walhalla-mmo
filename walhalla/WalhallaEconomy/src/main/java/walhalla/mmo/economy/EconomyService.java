package walhalla.mmo.economy;

import walhalla.mmo.core.api.EconomyBridge;
import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.progress.PlayerProgressService;

public class EconomyService implements EconomyBridge {
    private final PlayerProgressService progress;
    private final EconomyStore store;

    public EconomyService(PlayerProgressService progress, EconomyStore store) {
        this.progress = progress;
        this.store = store;
    }

    @Override
    public long getBalance(UUID playerId) {
        // Implementation logic here
    }

    @Override
    public long getBalance(UUID playerId, Currency currency) {
        // Implementation logic here
    }
}
