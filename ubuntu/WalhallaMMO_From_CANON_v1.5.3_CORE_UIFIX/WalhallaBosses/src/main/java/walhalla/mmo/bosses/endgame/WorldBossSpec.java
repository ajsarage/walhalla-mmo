package walhalla.mmo.bosses.endgame;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

public record WorldBossSpec(
        String id,
        EntityType entityType,
        String worldName,
        double x, double y, double z,
        int respawnMinutes,
        boolean announce
) {
    public static WorldBossSpec fromSection(String id, ConfigurationSection sec) {
        String typeStr = sec.getString("entityType", "WITHER");
        EntityType type;
        try { type = EntityType.valueOf(typeStr.toUpperCase()); }
        catch (Exception e) { type = EntityType.WITHER; }
        String world = sec.getString("world", "world");
        double x = sec.getDouble("x", 0);
        double y = sec.getDouble("y", 80);
        double z = sec.getDouble("z", 0);
        int respawn = sec.getInt("respawnMinutes", 60);
        boolean announce = sec.getBoolean("announce", true);
        return new WorldBossSpec(id, type, world, x, y, z, respawn, announce);
    }

    public Location toLocation(World w) {
        return new Location(w, x, y, z);
    }
}
