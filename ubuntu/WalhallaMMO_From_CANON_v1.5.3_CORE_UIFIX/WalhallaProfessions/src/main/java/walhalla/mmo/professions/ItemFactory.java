package walhalla.mmo.professions;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class ItemFactory {

    private final Plugin plugin;

    public ItemFactory(Plugin plugin) {
        this.plugin = plugin;
    }

    private static final String PDC_WH_ITEM_ID = "wh_item_id";
    private static final String PDC_WH_TIER = "wh_tier";
    private static final String PDC_WH_QUALITY = "wh_quality";

    public ItemStack createResource(String itemId, String displayName, Material material, int amount) {
        ItemStack it = new ItemStack(material == null ? guessMaterialStatic(itemId) : material, Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + displayName);
            meta.setLore(List.of(ChatColor.DARK_GRAY + "ID: " + itemId));
            // Backward compatible key
            meta.getPersistentDataContainer().set(ItemIds.namespacedKey(plugin), PersistentDataType.STRING, itemId);
            // Canon keys
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, PDC_WH_ITEM_ID), PersistentDataType.STRING, itemId);
            Integer tier = parseTier(itemId);
            if (tier != null) meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, PDC_WH_TIER), PersistentDataType.INTEGER, tier);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, PDC_WH_QUALITY), PersistentDataType.STRING, "Q1");
            it.setItemMeta(meta);
        }
        return it;
    }

    public ItemStack createResource(String itemId, int amount) {
        return createResource(itemId, itemId, null, amount);
    }

    private static Integer parseTier(String itemId) {
        if (itemId == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("_T(\\d+)$").matcher(itemId);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return null;
    }

    
/**
 * Static access for other registries (recipes, combat kits UI) that need a best-effort material icon.
 * Canon authority is the itemId; material is purely visual.
 */
public static Material guessMaterialStatic(String itemId) {
    return guessMaterial(itemId);
}

private static Material guessMaterial(String itemId) {
        if (itemId == null) return Material.PAPER;
        String id = itemId.toUpperCase(java.util.Locale.ROOT);
        // Timber
        if (id.contains("WOOD_LOG_OAK")) return Material.OAK_LOG;
        if (id.contains("WOOD_LOG_BIRCH")) return Material.BIRCH_LOG;
        if (id.contains("WOOD_LOG_SPRUCE")) return Material.SPRUCE_LOG;
        if (id.contains("WOOD_LOG_JUNGLE")) return Material.JUNGLE_LOG;
        if (id.contains("WOOD_LOG_ACACIA")) return Material.ACACIA_LOG;
        if (id.contains("WOOD_LOG_DARK_OAK")) return Material.DARK_OAK_LOG;
        if (id.contains("WOOD_LOG_MANGROVE")) return Material.MANGROVE_LOG;
        if (id.contains("WOOD_LOG_CHERRY")) return Material.CHERRY_LOG;

        // Rocks
        if (id.contains("ROCK_STONE")) return Material.STONE;
        if (id.contains("ROCK_ANDESITE")) return Material.ANDESITE;
        if (id.contains("ROCK_DIORITE")) return Material.DIORITE;
        if (id.contains("ROCK_GRANITE")) return Material.GRANITE;
        if (id.contains("ROCK_DEEPSLATE")) return Material.DEEPSLATE;
        if (id.contains("ROCK_BASALT")) return Material.BASALT;
        if (id.contains("ROCK_TUFF")) return Material.TUFF;
        if (id.contains("ROCK_OBSIDIAN")) return Material.OBSIDIAN;

        // Fibers
        if (id.contains("FIBER_WHEAT")) return Material.WHEAT;
        if (id.contains("FIBER_SUGARCANE")) return Material.SUGAR_CANE;
        if (id.contains("FIBER_BAMBOO")) return Material.BAMBOO;
        if (id.contains("FIBER_VINE")) return Material.VINE;
        if (id.contains("FIBER_NETHER")) return Material.NETHER_WART;
        if (id.contains("FIBER_WARPED")) return Material.WARPED_FUNGUS;
        if (id.contains("FIBER_CRIMSON")) return Material.CRIMSON_FUNGUS;
        if (id.contains("FIBER_CHORUS")) return Material.CHORUS_PLANT;

        // Herbs
        if (id.contains("HERB_DANDELION")) return Material.DANDELION;
        if (id.contains("HERB_POPPY")) return Material.POPPY;
        if (id.contains("HERB_BLUE_ORCHID")) return Material.BLUE_ORCHID;
        if (id.contains("HERB_ALLIUM")) return Material.ALLIUM;

        // Reactives
        if (id.contains("REACT_BLAZE")) return Material.BLAZE_POWDER;
        if (id.contains("REACT_GHAST")) return Material.GHAST_TEAR;
        if (id.contains("REACT_ENDER")) return Material.ENDER_PEARL;
        if (id.contains("REACT_DRAGON")) return Material.DRAGON_BREATH;

        return Material.PAPER;
    }

    public static String getItemId(Plugin plugin, ItemStack it) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(ItemIds.namespacedKey(plugin), PersistentDataType.STRING);
    }
}
