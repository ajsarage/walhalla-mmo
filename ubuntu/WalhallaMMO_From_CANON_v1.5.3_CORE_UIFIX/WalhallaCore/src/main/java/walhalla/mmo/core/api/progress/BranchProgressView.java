package walhalla.mmo.core.api.progress;

/**
 * CONTRACT_COREAPI_PROGRESS_READONLY_v1 — BranchProgressView (read-only).
 */
public record BranchProgressView(
        String branchId,
        int level,
        long totalXp,
        long xpIntoLevel,
        long xpForNextLevel,
        boolean active
) {}
