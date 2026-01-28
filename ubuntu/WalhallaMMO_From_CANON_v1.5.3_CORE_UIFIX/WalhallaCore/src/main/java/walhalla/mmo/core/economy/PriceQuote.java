package walhalla.mmo.core.economy;

import walhalla.mmo.core.api.economy.Currency;

/**
 * Immutable price quote for a single unit (or a fixed fee) in a given currency.
 */
public record PriceQuote(Currency currency, long amount) {
    public static PriceQuote of(Currency c, long a) {
        return new PriceQuote(c, Math.max(0L, a));
    }
}
