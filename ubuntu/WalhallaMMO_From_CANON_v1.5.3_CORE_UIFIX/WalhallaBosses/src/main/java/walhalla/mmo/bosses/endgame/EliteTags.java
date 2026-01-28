package walhalla.mmo.bosses.endgame;

import org.bukkit.entity.Entity;

public final class EliteTags {

    private EliteTags() {}

    public static boolean isElite(Entity e) {
        return e != null && e.getScoreboardTags().contains("WH_ELITE");
    }
}
