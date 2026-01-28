package walhalla.mmo.core.canon;

/**
 * Canon XP caps parsed from ANNEX_XP_CURVES_ALL_v1.
 * These values are binding: XP cannot increase beyond the total required to reach the cap.
 */
public record CanonXpCaps(
        int globalMaxLevel,
        int branchMaxLevel,
        int subBranchMaxLevel,
        int affinityMaxLevel,
        int professionMaxLevel
) {
    public static CanonXpCaps defaults() {
        // Safe fallbacks if parsing fails; kept high to avoid accidental hard caps.
        return new CanonXpCaps(10_000, 10_000, 10_000, 10_000, 10_000);
    }
}
