package walhalla.mmo.core.api.progress;

import java.util.Map;

/**
 * Read-only progress snapshot for UI.
 */
public record ProgressView(
        int globalLevel,
        long globalXp,
        long globalXpToNext,
        Map<String, Integer> branchLevels,
        Map<String, Long> branchXp
) {}
