package walhalla.mmo.core.api.progress;

/**
 * CONTRACT_COREAPI_PROGRESS_READONLY_v1 — PlayerProgressView (read-only).
 */
public record PlayerProgressView(
        int level,
        long totalXp,
        long xpIntoLevel,
        long xpForNextLevel
) {}
