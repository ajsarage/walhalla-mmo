package walhalla.mmo.core.progress;

import java.util.*;

/**
 * Canon: PlayerData is owned by Core, persisted by Core.
 *
 * Phase 2 (Progression/XP):
 * - XP mutation is allowed ONLY when lifecycleState == ACTIVE. (CONTRACT_XP_PERSISTENCE_v1, XP-1)
 * - Persistence is deferred and performed via full snapshots (handled by PlayerProgressStore/Service).
 *
 * NOTE: The canon does not provide balance numbers here. Curves are configurable in config.yml.
 */
public class PlayerData {

    public enum PlayerLifecycleState {
        UNINITIALIZED,
        CHOOSING_BRANCHES,
        ACTIVE
    }

    private final UUID playerId;

    // Lifecycle
    private PlayerLifecycleState lifecycleState = PlayerLifecycleState.UNINITIALIZED;

    // Initial branches chosen at onboarding (exactly 4 required by canon).
    private final LinkedHashSet<String> chosenBranches = new LinkedHashSet<>();

    // =========================
    // XP / Levels (Core-owned)
    // =========================

    // Global profile XP (total, not "into level")
    private long globalXpTotal = 0L;

    // Branch XP (per branchId) - total
    private final Map<String, Long> branchXpTotal = new HashMap<>();

    // Sub-branch XP is NOT_IMPLEMENTED for progression rules in canon,
    // but XP type exists in persistence contract; we store totals for future.
    // Key format: "<branchId>:<subBranchId>" (string id canonicalization happens elsewhere).
    private final Map<String, Long> subBranchXpTotal = new HashMap<>();

    // Weapon/Staff affinity XP (per weaponType or affinityId) - total
    private final Map<String, Long> affinityXpTotal = new HashMap<>();

    // Profession XP (per professionId) - total
    private final Map<String, Long> professionXpTotal = new HashMap<>();

    // RPG points (available = earned - spent). Spending is not implemented in Phase 2, but the values exist.
    private int rpgPointsEarned = 0;
    private int rpgPointsSpent = 0;

    // Economy (Phase 12)
    private long wcoin = 0L;
    private long wcraft = 0L;


    public PlayerData(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() { return playerId; }

    // -------------------------
    // Lifecycle
    // -------------------------
    public PlayerLifecycleState getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(PlayerLifecycleState state) {
        this.lifecycleState = (state == null) ? PlayerLifecycleState.UNINITIALIZED : state;
    }

    public boolean isActive() {
        return lifecycleState == PlayerLifecycleState.ACTIVE;
    }

    // -------------------------
    // Onboarding branches
    // -------------------------
    public Set<String> getChosenBranches() { return chosenBranches; }

    public void setChosenBranches(Collection<String> branches) {
        chosenBranches.clear();
        if (branches != null) {
            for (String b : branches) {
                if (b != null && !b.isBlank()) chosenBranches.add(b.trim());
            }
        }
    }

    // -------------------------
    // XP totals (read/write by Core services only)
    // -------------------------

    public long getGlobalXpTotal() { return globalXpTotal; }
    public void setGlobalXpTotal(long v) { this.globalXpTotal = Math.max(0L, v); }

    public Map<String, Long> getBranchXpTotal() { return branchXpTotal; }
    public long getBranchXpTotal(String branchId) { return branchXpTotal.getOrDefault(branchId, 0L); }
    public void setBranchXpTotal(String branchId, long total) {
        if (branchId == null || branchId.isBlank()) return;
        branchXpTotal.put(branchId.trim(), Math.max(0L, total));
    }

    public Map<String, Long> getSubBranchXpTotal() { return subBranchXpTotal; }
    public long getSubBranchXpTotal(String key) { return subBranchXpTotal.getOrDefault(key, 0L); }
    public void setSubBranchXpTotal(String key, long total) {
        if (key == null || key.isBlank()) return;
        subBranchXpTotal.put(key.trim(), Math.max(0L, total));
    }

    public Map<String, Long> getAffinityXpTotal() { return affinityXpTotal; }
    public long getAffinityXpTotal(String affinityId) { return affinityXpTotal.getOrDefault(affinityId, 0L); }
    public void setAffinityXpTotal(String affinityId, long total) {
        if (affinityId == null || affinityId.isBlank()) return;
        affinityXpTotal.put(affinityId.trim(), Math.max(0L, total));
    }

    public Map<String, Long> getProfessionXpTotal() { return professionXpTotal; }
    public long getProfessionXpTotal(String professionId) { return professionXpTotal.getOrDefault(professionId, 0L); }
    public void setProfessionXpTotal(String professionId, long total) {
        if (professionId == null || professionId.isBlank()) return;
        professionXpTotal.put(professionId.trim(), Math.max(0L, total));
    }

    // -------------------------
    // RPG points
    // -------------------------
    public int getRpgPointsEarned() { return rpgPointsEarned; }
    public void setRpgPointsEarned(int v) { this.rpgPointsEarned = Math.max(0, v); }

    public int getRpgPointsSpent() { return rpgPointsSpent; }
    public void setRpgPointsSpent(int v) { this.rpgPointsSpent = Math.max(0, v); }

    public int getRpgPointsAvailable() {
        int a = rpgPointsEarned - rpgPointsSpent;
        return Math.max(0, a);
    }

    public long getWcoin() { return Math.max(0L, wcoin); }
    public void setWcoin(long v) { this.wcoin = Math.max(0L, v); }

    public long getWcraft() { return Math.max(0L, wcraft); }
    public void setWcraft(long v) { this.wcraft = Math.max(0L, v); }
}

