package walhalla.mmo.core.api.progress;

/**
 * CONTRACT_COREAPI_PROGRESS_READONLY_v1 — WeaponAffinityProgressView (read-only).
 */
public record WeaponAffinityProgressView(
        String weaponType,
        int level,
        long totalXp,
        long xpIntoLevel,
        long xpForNextLevel
) {}
