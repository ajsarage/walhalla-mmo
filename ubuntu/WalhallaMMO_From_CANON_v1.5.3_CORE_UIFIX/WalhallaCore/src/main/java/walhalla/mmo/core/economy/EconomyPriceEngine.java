package walhalla.mmo.core.economy;

import walhalla.mmo.core.api.economy.Currency;
import walhalla.mmo.core.canon.CanonDataService;
import walhalla.mmo.core.canon.CanonFile;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 12: Canon-driven base price engine.
 *
 * Source of truth:
 * - ANNEX_ECONOMY_PRICES_BASE_v1.txt
 *
 * Implementation notes:
 * - The annex defines BASE reference prices (not market prices).
 * - We use these base prices for NPC sell/buy quotes and fee calculations.
 */
public final class EconomyPriceEngine {

    private static final Pattern TIER_WCOIN = Pattern.compile("T(\\d)\\s*:\\s*(\\d+)\\s*WCoin", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIER_WCRAFT = Pattern.compile("T(\\d)\\s*:\\s*(\\d+)\\s*WCraft", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULT_Q = Pattern.compile("Q(\\d)\\s*:\\s*x([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);

    private final Map<Integer, Long> rawWCoinByTier = new HashMap<>();
    private final Map<Integer, PriceQuote> weaponArmorBaseByTier = new HashMap<>();
    private final Map<Integer, PriceQuote> staffBaseByTier = new HashMap<>();
    private final Map<Integer, Double> qualityMul = new HashMap<>();

    private double refinedMul = 2.5;
    private double componentMul = 1.8;

    public EconomyPriceEngine(CanonDataService canon) {
        reload(canon);
    }

    public void reload(CanonDataService canon) {
        rawWCoinByTier.clear();
        weaponArmorBaseByTier.clear();
        staffBaseByTier.clear();
        qualityMul.clear();

        // sensible defaults (will be overwritten if found)
        refinedMul = 2.5;
        componentMul = 1.8;
        qualityMul.put(1, 1.1);
        qualityMul.put(2, 1.25);
        qualityMul.put(3, 1.5);
        qualityMul.put(4, 2.0);
        qualityMul.put(5, 3.0);

        String txt = canon.getRaw(CanonFile.ANNEX_ECONOMY_PRICES)
                .orElseThrow(() -> new IllegalStateException("Missing canon annex: " + CanonFile.ANNEX_ECONOMY_PRICES));
        if (txt == null) return;

        String[] lines = txt.split("\r?\n");
        String section = "";

        for (String lineRaw : lines) {
            String line = lineRaw.trim();
            if (line.isBlank()) continue;

            // section tracking (very lightweight, based on canonical headings)
            String up = line.toUpperCase(Locale.ROOT);
            if (up.contains("RECURSOS BRUTOS")) section = "RAW";
            else if (up.contains("ARMAS") && up.contains("ARMADURAS")) section = "WEAPON_ARMOR";
            else if (up.contains("BASTONES") && up.contains("MAGIA")) section = "STAFF";

            // line matchers (re-used across sections)
            Matcher m1 = TIER_WCOIN.matcher(line);
            Matcher m2 = TIER_WCRAFT.matcher(line);
            Matcher mq = MULT_Q.matcher(line);

            // Quality multipliers are global in the annex (not tied to a section)
            if (mq.find()) {
                try {
                    int q = Integer.parseInt(mq.group(1));
                    double mul = Double.parseDouble(mq.group(2));
                    qualityMul.put(q, mul);
                } catch (NumberFormatException ignored) {
                    // ignore malformed line; canon remains source of truth
                }
                continue;
            }

            if ("RAW".equals(section)) {
                if (m1.find()) {
                    int tier = Integer.parseInt(m1.group(1));
                    long val = Long.parseLong(m1.group(2));
                    rawWCoinByTier.put(tier, Math.max(0L, val));
                }
                continue;
            }

            if ("WEAPON_ARMOR".equals(section)) {
                if (m1.find()) {
                    int tier = Integer.parseInt(m1.group(1));
                    weaponArmorBaseByTier.put(tier, PriceQuote.of(Currency.WCOIN, Long.parseLong(m1.group(2))));
                } else if (m2.find()) {
                    int tier = Integer.parseInt(m2.group(1));
                    weaponArmorBaseByTier.put(tier, PriceQuote.of(Currency.WCRAFT, Long.parseLong(m2.group(2))));
                }
                continue;
            }

            if ("STAFF".equals(section)) {
                if (m1.find()) {
                    int tier = Integer.parseInt(m1.group(1));
                    staffBaseByTier.put(tier, PriceQuote.of(Currency.WCOIN, Long.parseLong(m1.group(2))));
                } else if (m2.find()) {
                    int tier = Integer.parseInt(m2.group(1));
                    staffBaseByTier.put(tier, PriceQuote.of(Currency.WCRAFT, Long.parseLong(m2.group(2))));
                }
                continue;
            }

            // Multipliers (global)
            // e.g. "REFINADO = x2.5 valor del recurso base"
            String low = line.toLowerCase(Locale.ROOT);
            if (low.contains("refinado") && low.contains("x")) {
                Double x = extractX(low);
                if (x != null && x > 0) refinedMul = x;
            } else if (low.contains("componente") && low.contains("x")) {
                Double x = extractX(low);
                if (x != null && x > 0) componentMul = x;
            }
        }

        // fallback tier table if annex doesn't enumerate all (keeps runtime deterministic)
        if (rawWCoinByTier.isEmpty()) {
            rawWCoinByTier.put(1, 2L);
            rawWCoinByTier.put(2, 4L);
            rawWCoinByTier.put(3, 8L);
            rawWCoinByTier.put(4, 15L);
            rawWCoinByTier.put(5, 30L);
            rawWCoinByTier.put(6, 60L);
            rawWCoinByTier.put(7, 120L);
            rawWCoinByTier.put(8, 250L);
        }
}

    private Double extractX(String lineLower) {
        // find first occurrence of x<decimal>
        Pattern px = Pattern.compile("x\\s*([0-9]+(?:\\.[0-9]+)?)");
        Matcher m = px.matcher(lineLower);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public PriceQuote quoteNpcSell(String itemId, int tier, int quality) {
        if (itemId == null || itemId.isBlank()) return PriceQuote.of(Currency.WCOIN, 0);

        String id = itemId.toUpperCase(Locale.ROOT);

        // Currency / premium items are not sold to NPC through this path
        if (id.startsWith("IT_CUR")) return PriceQuote.of(Currency.WCOIN, 0);

        if (id.startsWith("IT_WEAP") || id.startsWith("IT_ARM")) {
            PriceQuote base = weaponArmorBaseByTier.getOrDefault(tier, PriceQuote.of(Currency.WCOIN, 0));
            return applyQuality(base, quality);
        }
        if (id.contains("STAFF") || id.startsWith("IT_WEAP_STAFF") || id.startsWith("IT_STAFF")) {
            PriceQuote base = staffBaseByTier.getOrDefault(tier, PriceQuote.of(Currency.WCOIN, 0));
            return applyQuality(base, quality);
        }

        // Materials/components derived from raw tier base (WCoin only, until endgame tiers)
        long raw = rawWCoinByTier.getOrDefault(tier, 0L);
        if (id.startsWith("IT_RES")) {
            return PriceQuote.of(Currency.WCOIN, raw);
        }
        if (id.startsWith("IT_MAT")) {
            return PriceQuote.of(Currency.WCOIN, Math.max(0L, Math.round(raw * refinedMul)));
        }
        if (id.startsWith("IT_COMP")) {
            long refined = Math.round(raw * refinedMul);
            return PriceQuote.of(Currency.WCOIN, Math.max(0L, Math.round(refined * componentMul)));
        }

        // Default: no NPC price known
        return PriceQuote.of(Currency.WCOIN, 0);
    }

    public PriceQuote quoteStationUseFee(int tier) {
        // from ANNEX_ECONOMY_SINKS_v1 and ANNEX_ECONOMY_PRICES_BASE_v1 reference
        if (tier <= 0) return PriceQuote.of(Currency.WCOIN, 0);
        if (tier <= 2) return PriceQuote.of(Currency.WCOIN, 1);
        if (tier <= 4) return PriceQuote.of(Currency.WCOIN, 2);
        if (tier <= 6) return PriceQuote.of(Currency.WCOIN, 3);
        return PriceQuote.of(Currency.WCRAFT, 1);
    }

    private PriceQuote applyQuality(PriceQuote base, int quality) {
        if (base == null) return PriceQuote.of(Currency.WCOIN, 0);
        int q = Math.max(0, quality);
        if (q <= 0) return base;
        double mul = qualityMul.getOrDefault(q, 1.0);
        long amt = (long) Math.round(base.amount() * mul);
        return PriceQuote.of(base.currency(), Math.max(0L, amt));
    }
}