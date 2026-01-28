package walhalla.mmo.zones.model;

import org.bukkit.Location;

/** Axis-aligned bounds in a Bukkit world. */
public record ZoneBox(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(world)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }
}
