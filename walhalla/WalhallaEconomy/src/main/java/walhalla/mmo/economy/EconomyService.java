package walhalla.mmo.economy;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;
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
        return store.get(playerId);
    }

    @Override
    public long getBalance(UUID playerId, Currency currency) {
        // Implementation logic here
        if (currency == Currency.WCOIN) {
            return store.get(playerId);
        } else {
            return 0;
        }
    }

    @Override
    public void deposit(UUID playerId, long amount, String reason) {
        store.set(playerId, store.get(playerId) + amount);
    }

    @Override
    public void withdraw(UUID playerId, long amount, String reason) {
        long currentBalance = store.get(playerId);
        if (currentBalance >= amount) {
            store.set(playerId, currentBalance - amount);
        }
    }

    @Override
    public boolean has(UUID playerId, long amount) {
        return store.get(playerId) >= amount;
    }

    @Override
    public void setBalance(UUID playerId, long amount) {
        store.set(playerId, amount);
    }

    @Override
    public boolean transfer(UUID fromPlayerId, UUID toPlayerId, long amount, String reason) {
        if (has(fromPlayerId, amount)) {
            withdraw(fromPlayerId, amount, reason);
            deposit(toPlayerId, amount, reason);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSupported(Currency currency) {
        return currency == Currency.WCOIN;
    }
}
