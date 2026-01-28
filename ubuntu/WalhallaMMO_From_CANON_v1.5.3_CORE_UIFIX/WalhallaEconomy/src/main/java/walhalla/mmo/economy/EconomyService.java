package walhalla.mmo.economy;

import java.util.UUID;

import walhalla.mmo.core.api.economy.Currency;
import walhalla.mmo.core.api.economy.EconomyBridge;

/**
 * Economy authority.
 */
public final class EconomyService implements EconomyBridge {

    private final EconomyStore store;

    public EconomyService(EconomyStore store) {
        this.store = store;
    }

    @Override
    public long getBalance(UUID playerId) {
        if (playerId == null) return 0L;
        return store.get(playerId);
    }

    @Override
    public void deposit(UUID playerId, long amount, String reason) {
        if (playerId == null) return;
        if (amount <= 0) return;
        long cur = store.get(playerId);
        long next;
        try {
            next = Math.addExact(cur, amount);
        } catch (ArithmeticException ex) {
            next = Long.MAX_VALUE;
        }
        store.set(playerId, next);
        store.flush();
    }

    @Override
    public boolean withdraw(UUID playerId, long amount, String reason) {
        if (playerId == null) return false;
        if (amount <= 0) return false;
        long cur = store.get(playerId);
        if (cur < amount) return false;
        store.set(playerId, cur - amount);
        store.flush();
        return true;
    }

    @Override
    public long getBalance(UUID playerId, Currency currency) {
        if (playerId == null) return 0L;
        return store.get(playerId);
    }

    @Override
    public void deposit(UUID playerId, Currency currency, long amount, String reason, String txId) {
        if (playerId == null) return;
        if (amount <= 0) return;
        long cur = store.get(playerId);
        long next;
        try {
            next = Math.addExact(cur, amount);
        } catch (ArithmeticException ex) {
            next = Long.MAX_VALUE;
        }
        store.set(playerId, next);
        store.flush();
    }

    @Override
    public boolean withdraw(UUID playerId, Currency currency, long amount, String reason, String txId) {
        if (playerId == null) return false;
        if (amount <= 0) return false;
        long cur = store.get(playerId);
        if (cur < amount) return false;
        store.set(playerId, cur - amount);
        store.flush();
        return true;
    }
}
