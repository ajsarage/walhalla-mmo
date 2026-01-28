package walhalla.mmo.professions;

import java.util.Set;

import org.bukkit.Material;

public record GatherNode(
        String nodeId,
        Set<Material> blocks,
        String professionId,
        String resourceItemId,
        String resourceName,
        Material resourceMaterial,
        int dropMin,
        int dropMax,
        long xp,
        long sellValue
) {}
