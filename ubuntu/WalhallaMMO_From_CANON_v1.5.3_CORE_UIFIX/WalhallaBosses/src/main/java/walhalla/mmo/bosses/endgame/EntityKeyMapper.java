package walhalla.mmo.bosses.endgame;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/**
 * Maps Bukkit entity types to canonical keys used in ANNEX_ENTITY_DROPS_AND_LOOT_TABLES.
 */
public final class EntityKeyMapper {

    private EntityKeyMapper() {}

    public static String toCanonKey(LivingEntity e) {
        if (e == null) return null;
        EntityType t = e.getType();
        if (t == null) return null;

        // Groups in annex:
        // "ZOMBIE / HUSK / DROWNED / ZOMBIE_VILLAGER"
        return switch (t) {
            case ZOMBIE, HUSK, DROWNED, ZOMBIE_VILLAGER -> "ZOMBIE";
            case SKELETON, STRAY, WITHER_SKELETON -> "SKELETON";
            case SPIDER, CAVE_SPIDER -> "SPIDER";
            case WITHER -> "WITHER";
            case ENDER_DRAGON -> "ENDER_DRAGON";
            default -> t.name(); // many match directly (COW, PIG, CHICKEN, SHEEP, ENDERMAN, BLAZE, ...)
        };
    }
}
