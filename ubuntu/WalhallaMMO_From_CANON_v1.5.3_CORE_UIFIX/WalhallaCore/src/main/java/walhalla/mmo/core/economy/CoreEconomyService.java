package walhalla.mmo.core.economy;

import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.economy.Currency;
import walhalla.mmo.core.api.economy.EconomyBridge;
import walhalla.mmo.core.progress.PlayerData;
import walhalla.mmo.core.progress.PlayerProgressService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Phase 12: Economy authority implemented in Core.
 *
 * Guarantees:
 * - Atomic wallet mutation (per-player synchronized)
 * - "If not charged -> action must not execute" is supported by withdraw() returning false
 * - Transaction audit (append-only log)
 * - Optional idempotency via txId
 */
public final class CoreEconomyService implements EconomyBridge {

    private final JavaPlugin plugin;
    private final PlayerProgressService progress;
    private final File ledgerFile;

    // Best-effort idempotency (kept in memory, not a full DB)
    private final Map<UUID, LinkedHashSet<String>> recentTxIds = new HashMap<>();

    public CoreEconomyService(JavaPlugin plugin, PlayerProgressService progress) {
        this.plugin = plugin;
        this.progress = progress;
        this.ledgerFile = new File(plugin.getDataFolder(), "economy_ledger.log");
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
    }

    @Override
    public long getBalance(UUID playerId, Currency currency) {
        if (playerId == null || currency == null) return 0L;
        PlayerData data = progress.getOrLoad(playerId);
        synchronized (data) {
            return currency == Currency.WCRAFT ? data.getWcraft() : data.getWcoin();
        }
    }

    @Override
    public void deposit(UUID playerId, Currency currency, long amount, String reason, String txId) {
        if (playerId == null || currency == null) return;
        if (amount <= 0) return;

        PlayerData data = progress.getOrLoad(playerId);
        synchronized (data) {
            if (isDuplicate(playerId, txId)) {
                audit(playerId, currency, "DUP_DEPOSIT", amount, reason, txId, getBal(data, currency), getBal(data, currency));
                return;
            }

            long before = getBal(data, currency);
            long after;
            try {
                after = Math.addExact(before, amount);
            } catch (ArithmeticException ex) {
                after = Long.MAX_VALUE;
            }
            setBal(data, currency, after);

            markTx(playerId, txId);
            progress.saveNow(data);
            audit(playerId, currency, "DEPOSIT", amount, reason, txId, before, after);
        }
    }

    @Override
    public boolean withdraw(UUID playerId, Currency currency, long amount, String reason, String txId) {
        if (playerId == null || currency == null) return false;
        if (amount <= 0) return false;

        PlayerData data = progress.getOrLoad(playerId);
        synchronized (data) {
            if (isDuplicate(playerId, txId)) {
                audit(playerId, currency, "DUP_WITHDRAW", amount, reason, txId, getBal(data, currency), getBal(data, currency));
                return true;
            }

            long before = getBal(data, currency);
            if (before < amount) {
                audit(playerId, currency, "DECLINED", amount, reason, txId, before, before);
                return false;
            }
            long after = before - amount;
            setBal(data, currency, after);

            markTx(playerId, txId);
            progress.saveNow(data);
            audit(playerId, currency, "WITHDRAW", amount, reason, txId, before, after);
            return true;
        }
    }

    private long getBal(PlayerData data, Currency c) {
        return c == Currency.WCRAFT ? data.getWcraft() : data.getWcoin();
    }

    private void setBal(PlayerData data, Currency c, long v) {
        if (c == Currency.WCRAFT) data.setWcraft(v);
        else data.setWcoin(v);
    }

    private boolean isDuplicate(UUID playerId, String txId) {
        if (txId == null || txId.isBlank()) return false;
        LinkedHashSet<String> set = recentTxIds.get(playerId);
        return set != null && set.contains(txId);
    }

    private void markTx(UUID playerId, String txId) {
        if (txId == null || txId.isBlank()) return;
        LinkedHashSet<String> set = recentTxIds.computeIfAbsent(playerId, k -> new LinkedHashSet<>());
        set.add(txId);
        // cap memory
        while (set.size() > 256) {
            Iterator<String> it = set.iterator();
            if (it.hasNext()) { it.next(); it.remove(); }
            else break;
        }
    }

    private void audit(UUID playerId, Currency currency, String kind, long amount, String reason, String txId, long before, long after) {
        // append-only, one JSON line per tx (human + machine readable)
        String safeReason = reason == null ? "" : reason.replace("\n", " ").replace("\r", " ");
        String safeTx = txId == null ? "" : txId;
        String line = "{"
                + "\"ts\":\"" + Instant.now().toString() + "\","
                + "\"player\":\"" + playerId + "\","
                + "\"kind\":\"" + kind + "\","
                + "\"currency\":\"" + currency + "\","
                + "\"amount\":" + amount + ","
                + "\"before\":" + before + ","
                + "\"after\":" + after + ","
                + "\"reason\":\"" + escapeJson(safeReason) + "\","
                + "\"txId\":\"" + escapeJson(safeTx) + "\""
                + "}";

        try (FileOutputStream fos = new FileOutputStream(ledgerFile, true);
             OutputStreamWriter w = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(w)) {
            bw.write(line);
            bw.newLine();
        } catch (IOException ex) {
            plugin.getLogger().warning("Economy audit write failed: " + ex.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\t", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
