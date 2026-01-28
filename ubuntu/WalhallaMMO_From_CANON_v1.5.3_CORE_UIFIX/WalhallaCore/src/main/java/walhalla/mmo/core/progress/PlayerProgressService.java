package walhalla.mmo.core.progress;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.progress.*;
import walhalla.mmo.core.canon.CanonXpCaps;

/**
 * Owns in-memory PlayerData for online players.
 *
 * Canon notes:
 * - XP mutates only if lifecycleState == ACTIVE (CONTRACT_XP_PERSISTENCE_v1, XP-1)
 * - Persistence is deferred and snapshot-based (XP-2/XP-3/XP-4), handled by store + autosave.
 */
public class PlayerProgressService implements Listener {

    private final JavaPlugin plugin;
    private final PlayerProgressStore store;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    private final XpCurve curve;

    private CanonXpCaps caps = CanonXpCaps.defaults();

    public PlayerProgressService(JavaPlugin plugin, PlayerProgressStore store) {
        this.plugin = plugin;
        this.store = store;
        this.curve = XpCurve.fromConfig(plugin.getConfig());
    }
    /**
     * Phase 7: apply binding canon caps to XP progression.
     * If caps are missing, defaults keep levels effectively unbounded.
     */
    public void applyCanonCaps(CanonXpCaps caps) {
        if (caps != null) this.caps = caps;
    }

    private long capTotalForMaxLevel(XpCurve.Curve c, int maxLevel) {
        int ml = Math.max(1, maxLevel);
        // Total required to reach maxLevel (level 1 => 0)
        return c.totalToReachLevel(ml);
    }

    private long clampTotal(XpCurve.Curve c, long total, int maxLevel) {
        long t = Math.max(0L, total);
        long cap = capTotalForMaxLevel(c, maxLevel);
        return Math.min(t, cap);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        cache.put(id, store.load(id));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        PlayerData data = cache.remove(id);
        if (data != null) store.save(data);
    }

    public PlayerData getOrLoad(UUID id) {
        return cache.computeIfAbsent(id, store::load);
    }

    public void saveNow(PlayerData data) {
        if (data == null) return;
        store.save(data);
        cache.put(data.getPlayerId(), data);
    }

    public boolean isActive(UUID id) {
        return getOrLoad(id).isActive();
    }

    // =========================================================
    // XP mutation (Core-only) — guarded by ACTIVE state (XP-1)
    // =========================================================

    public boolean addGlobalXp(UUID id, long amount) {
        if (amount <= 0) return false;
        PlayerData d = getOrLoad(id);
        if (!d.isActive()) return false;
        long nt = safeAdd(d.getGlobalXpTotal(), amount);
        nt = clampTotal(curve.global(), nt, caps.globalMaxLevel());
        d.setGlobalXpTotal(nt);
        // RPG points: configurable per global level (earned as levels are reached)
        syncRpgPointsFromGlobalLevel(d);
        return true;
    }

    public boolean addBranchXp(UUID id, String branchId, long amount) {
        if (amount <= 0) return false;
        if (branchId == null || branchId.isBlank()) return false;
        PlayerData d = getOrLoad(id);
        if (!d.isActive()) return false;
        String key = branchId.trim();
        d.setBranchXpTotal(key, safeAdd(d.getBranchXpTotal(key), amount));
        return true;
    }

    public boolean addSubBranchXp(UUID id, String subBranchKey, long amount) {
        if (amount <= 0) return false;
        if (subBranchKey == null || subBranchKey.isBlank()) return false;
        PlayerData d = getOrLoad(id);
        if (!d.isActive()) return false;
        String key = subBranchKey.trim();
        d.setSubBranchXpTotal(key, safeAdd(d.getSubBranchXpTotal(key), amount));
        return true;
    }

    public boolean addAffinityXp(UUID id, String affinityId, long amount) {
        if (amount <= 0) return false;
        if (affinityId == null || affinityId.isBlank()) return false;
        PlayerData d = getOrLoad(id);
        if (!d.isActive()) return false;
        String key = affinityId.trim();
        d.setAffinityXpTotal(key, safeAdd(d.getAffinityXpTotal(key), amount));
        return true;
    }

    public boolean addProfessionXp(UUID id, String professionId, long amount) {
        if (amount <= 0) return false;
        if (professionId == null || professionId.isBlank()) return false;
        PlayerData d = getOrLoad(id);
        if (!d.isActive()) return false;
        String key = professionId.trim();
        d.setProfessionXpTotal(key, safeAdd(d.getProfessionXpTotal(key), amount));
        return true;
    }

    private long safeAdd(long a, long b) {
        long r;
        try {
            r = Math.addExact(a, b);
        } catch (ArithmeticException ex) {
            r = Long.MAX_VALUE;
        }
        return Math.max(0L, r);
    }

    
    /**
     * Read-only: chosen branches selected during onboarding.
     * CONTRACT: CONTRACT_COREAPI_PROGRESS_READONLY_v1
     */
    public List<String> getChosenBranches(UUID playerId) {
        PlayerData d = getOrLoad(playerId);
        // Preserve canonical onboarding order (LinkedHashSet in PlayerData).
        return new ArrayList<>(d.getChosenBranches());
    }

    /**
     * Read-only: lifecycle state for onboarding/XP gating.
     * CONTRACT: CONTRACT_COREAPI_PROGRESS_READONLY_v1
     */
    public PlayerData.PlayerLifecycleState getLifecycleState(UUID playerId) {
        PlayerData d = getOrLoad(playerId);
        return d.getLifecycleState();
    }

// =========================================================
    // Read-only views (CoreAPI uses these)
    // =========================================================

    public PlayerProgressView getPlayerProgress(UUID id) {
        PlayerData d = getOrLoad(id);
        XpLevelSnapshot snap = curve.global().snapshot(d.getGlobalXpTotal());
        return new PlayerProgressView(
                snap.level(),
                d.getGlobalXpTotal(),
                snap.xpIntoLevel(),
                snap.xpForNextLevel()
        );
    }

    public List<BranchProgressView> getAllBranchProgress(UUID id) {
        PlayerData d = getOrLoad(id);
        List<BranchProgressView> out = new ArrayList<>();
        for (String branchId : d.getChosenBranches()) {
            long total = d.getBranchXpTotal(branchId);
            XpLevelSnapshot snap = curve.branch().snapshot(total);
            out.add(new BranchProgressView(
                    branchId,
                    snap.level(),
                    total,
                    snap.xpIntoLevel(),
                    snap.xpForNextLevel(),
                    true
            ));
        }
        // Also expose stored branch XP for branches not in chosen set (admin/test), marked inactive.
        for (Map.Entry<String, Long> e : d.getBranchXpTotal().entrySet()) {
            if (d.getChosenBranches().contains(e.getKey())) continue;
            long total = e.getValue() == null ? 0L : e.getValue();
            XpLevelSnapshot snap = curve.branch().snapshot(total);
            out.add(new BranchProgressView(
                    e.getKey(),
                    snap.level(),
                    total,
                    snap.xpIntoLevel(),
                    snap.xpForNextLevel(),
                    false
            ));
        }
        return out;
    }

    public BranchProgressView getBranchProgress(UUID id, String branchId) {
        PlayerData d = getOrLoad(id);
        long total = d.getBranchXpTotal(branchId);
        XpLevelSnapshot snap = curve.branch().snapshot(total);
        boolean active = d.getChosenBranches().contains(branchId);
        return new BranchProgressView(branchId, snap.level(), total, snap.xpIntoLevel(), snap.xpForNextLevel(), active);
    }

    public List<WeaponAffinityProgressView> getWeaponAffinities(UUID id) {
        PlayerData d = getOrLoad(id);
        List<WeaponAffinityProgressView> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : d.getAffinityXpTotal().entrySet()) {
            long total = e.getValue() == null ? 0L : e.getValue();
            XpLevelSnapshot snap = curve.affinity().snapshot(total);
            out.add(new WeaponAffinityProgressView(e.getKey(), snap.level(), total, snap.xpIntoLevel(), snap.xpForNextLevel()));
        }
        return out;
    }



    public AvailablePointsView getAvailableRpgPoints(UUID id) {
        PlayerData d = getOrLoad(id);
        return new AvailablePointsView(d.getRpgPointsAvailable());
    }

    public SubBranchProgressView getSubBranchProgress(UUID id, String subBranchId) {
        // Canon: NOT_IMPLEMENTED (CONTRACT_COREAPI_PROGRESS_READONLY_v1)
        return SubBranchProgressView.notImplemented(subBranchId);
    }

    // =========================================================
    // RPG points earning (bootstrap rule)
    // =========================================================
    private void syncRpgPointsFromGlobalLevel(PlayerData d) {
        XpLevelSnapshot snap = curve.global().snapshot(d.getGlobalXpTotal());
        int lvl = snap.level();
        int perLevel = Math.max(0, curve.pointsPerGlobalLevel());
        // Earned points is deterministic from level; spending is separate.
        int earned = Math.max(0, (lvl - 1) * perLevel);
        d.setRpgPointsEarned(earned);
    }

    public void saveAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = cache.get(p.getUniqueId());
            if (d != null) store.save(d);
        }
    }

    // =========================================================
// Professions (read-only views)
    // =========================================================

    public List<ProfessionProgressView> getAllProfessionProgress(UUID id) {
        PlayerData d = getOrLoad(id);
        List<ProfessionProgressView> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : d.getProfessionXpTotal().entrySet()) {
            String professionId = e.getKey();
            long total = e.getValue() == null ? 0L : e.getValue();
            XpLevelSnapshot snap = curve.profession().snapshot(total);
            out.add(new ProfessionProgressView(
                    professionId,
                    snap.level(),
                    total,
                    snap.xpIntoLevel(),
                    snap.xpForNextLevel()
            ));
        }
        return out;
    }

    public ProfessionProgressView getProfessionProgress(UUID id, String professionId) {
        String key = professionId == null ? "" : professionId.trim();
        PlayerData d = getOrLoad(id);
        long total = d.getProfessionXpTotal(key);
        XpLevelSnapshot snap = curve.profession().snapshot(total);
        return new ProfessionProgressView(
                key,
                snap.level(),
                total,
                snap.xpIntoLevel(),
                snap.xpForNextLevel()
        );
    }

}
