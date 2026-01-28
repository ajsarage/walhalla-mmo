package walhalla.mmo.professions;

import java.util.Map;

import org.bukkit.Material;

public record CraftRecipe(
        String recipeId,
        String displayName,
        Material displayMaterial,
        String outputItemId,
        String outputName,
        Material outputMaterial,
        int outputAmount,
        Map<String, Integer> ingredients,
        String requiredProfessionId,
        int requiredProfessionLevel,
        long craftXp,
        long moneyCost
) {}
