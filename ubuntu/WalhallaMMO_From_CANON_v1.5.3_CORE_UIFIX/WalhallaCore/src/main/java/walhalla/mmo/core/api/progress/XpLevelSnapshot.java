package walhalla.mmo.core.api.progress;

/**
 * Immutable computed snapshot for a total-XP value.
 */
public record XpLevelSnapshot(
        int level,
        long xpIntoLevel,
        long xpForNextLevel
) {}
