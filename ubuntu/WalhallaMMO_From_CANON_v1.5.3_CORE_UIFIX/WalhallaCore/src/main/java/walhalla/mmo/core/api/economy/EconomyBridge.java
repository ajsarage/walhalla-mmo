package walhalla.mmo.core.api.economy;

import java.util.UUID;

/**
 * Economy bridge used by gameplay systems.
 *
 * Phase 12: Core is the authority over balances. Other plugins must request changes through this bridge.
 *
 * Notes:
 * - Legacy methods (getBalance/deposit/withdraw) refer to WCOIN for backwards compatibility inside this repo.
 * - Use the Currency overloads whenever possible.
 */
public interface EconomyBridge {

    default long getBalance(UUID playerId) {
        return getBalance(playerId, Currency.WCOIN);
    }

    long getBalance(UUID playerId, Currency currency);

    default void deposit(UUID playerId, long amount, String reason) {
        deposit(playerId, Currency.WCOIN, amount, reason, null);
    }

    default boolean withdraw(UUID playerId, long amount, String reason) {
        return withdraw(playerId, Currency.WCOIN, amount, reason, null);
    }

    /**
     * Deposit currency into a player's wallet.
     * @param txId Optional idempotency key. If provided and previously applied, the call is a no-op.
     */
    void deposit(UUID playerId, Currency currency, long amount, String reason, String txId);

    /**
     * Withdraw currency from a player's wallet.
     * @param txId Optional idempotency key. If provided and previously applied, the call is a no-op.
     * @return true if funds were withdrawn or tx was already applied.
     */
    boolean withdraw(UUID playerId, Currency currency, long amount, String reason, String txId);
}
