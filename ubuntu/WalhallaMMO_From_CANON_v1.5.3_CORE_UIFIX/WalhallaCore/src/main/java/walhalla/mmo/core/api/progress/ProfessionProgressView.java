package walhalla.mmo.core.api.progress;

/**
 * Read-only profession progress snapshot.
 */
public record ProfessionProgressView(
        String professionId,
        int level,
        long totalXp,
        long xpIntoLevel,
        long xpForNextLevel
) {}
