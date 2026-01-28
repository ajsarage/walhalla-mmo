package walhalla.mmo.core.api.progress;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configurable XP curves (bootstrap defaults).
 *
 * Canon does not define numeric balance; values are configurable.
 */
public final class XpCurve {

    public record Curve(long base, long perLevel) {
        public Curve {
            base = Math.max(1L, base);
            perLevel = Math.max(0L, perLevel);
        }

        /**
         * XP required to go from level -> level+1 (level >= 1).
         */
        public long requiredForNext(int level) {
            int l = Math.max(1, level);
            // Linear bootstrap curve:
            // req(L) = base + (L-1)*perLevel
            long add = (long) (l - 1) * perLevel;
            long req;
            try {
                req = Math.addExact(base, add);
            } catch (ArithmeticException ex) {
                req = Long.MAX_VALUE;
            }
            return Math.max(1L, req);
        }

        /**
         * Total XP required to reach a given level (i.e. sum req(1..level-1)).
         * Level 1 requires 0 total xp.
         */
        public long totalToReachLevel(int level) {
            int L = Math.max(1, level);
            long n = (long) (L - 1);
            // sum = n*base + perLevel * n*(n-1)/2
            long sum1, sum2, sum;
            try {
                sum1 = Math.multiplyExact(n, base);
            } catch (ArithmeticException ex) { sum1 = Long.MAX_VALUE; }
            try {
                long nn1 = Math.multiplyExact(n, Math.max(0L, n - 1));
                sum2 = Math.multiplyExact(perLevel, nn1 / 2L);
            } catch (ArithmeticException ex) { sum2 = Long.MAX_VALUE; }
            try {
                sum = Math.addExact(sum1, sum2);
            } catch (ArithmeticException ex) { sum = Long.MAX_VALUE; }
            return Math.max(0L, sum);
        }

        /**
         * Snapshot for a total XP value.
         */
        public XpLevelSnapshot snapshot(long totalXp) {
            long t = Math.max(0L, totalXp);
            // Find level by incrementally advancing (safe, but bounded by maxLevel)
            int level = 1;
            long xpInto = t;
            long req = requiredForNext(level);

            // Limit to avoid pathological loops if totalXp is huge.
            int maxLevel = 10_000;

            while (xpInto >= req && level < maxLevel) {
                xpInto -= req;
                level++;
                req = requiredForNext(level);
            }
            return new XpLevelSnapshot(level, xpInto, req);
        }
    }

    private final Curve global;
    private final Curve branch;
    private final Curve affinity;
    private final Curve profession;
    private final int pointsPerGlobalLevel;

    private XpCurve(Curve global, Curve branch, Curve affinity, Curve profession, int pointsPerGlobalLevel) {
        this.global = global;
        this.branch = branch;
        this.affinity = affinity;
        this.profession = profession;
        this.pointsPerGlobalLevel = Math.max(0, pointsPerGlobalLevel);
    }

    public static XpCurve fromConfig(FileConfiguration cfg) {
        long gBase = cfg.getLong("xp.curves.global.base", 100);
        long gPer = cfg.getLong("xp.curves.global.perLevel", 50);

        long bBase = cfg.getLong("xp.curves.branch.base", 80);
        long bPer = cfg.getLong("xp.curves.branch.perLevel", 40);

        long aBase = cfg.getLong("xp.curves.affinity.base", 60);
        long aPer = cfg.getLong("xp.curves.affinity.perLevel", 30);

        long pBase = cfg.getLong("xp.curves.profession.base", 70);
        long pPer = cfg.getLong("xp.curves.profession.perLevel", 35);

        int pts = cfg.getInt("xp.points.rpg.perGlobalLevel", 1);

        return new XpCurve(new Curve(gBase, gPer), new Curve(bBase, bPer), new Curve(aBase, aPer), new Curve(pBase, pPer), pts);
    }

    public Curve global() { return global; }
    public Curve branch() { return branch; }
    public Curve affinity() { return affinity; }
    public Curve profession() { return profession; }

    public int pointsPerGlobalLevel() { return pointsPerGlobalLevel; }
}
