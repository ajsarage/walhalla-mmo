package walhalla.mmo.bosses.endgame;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Minimal canonical item wrapper.
 * Uses the same PDC keys as other systems (wh_item_id, wh_tier, wh_quality) where possible.
 * DisplayName defaults to itemId (no invention).
 */
public final class BossItemFactory {

    private final Plugin plugin;
    private final NamespacedKey keyItemId;
    private final NamespacedKey keyTier;
    private final NamespacedKey keyQuality;

    public BossItemFactory(Plugin plugin) {
        this.plugin = plugin;
        this.keyItemId = new NamespacedKey(plugin, "wh_item_id");
        this.keyTier = new NamespacedKey(plugin, "wh_tier");
        this.keyQuality = new NamespacedKey(plugin, "wh_quality");
    }

    public ItemStack createResource(String itemId, int amount) {
        ItemStack it = new ItemStack(guessMaterial(itemId), Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + itemId);
            meta.setLore(List.of(ChatColor.DARK_GRAY + "ID: " + itemId));
            meta.getPersistentDataContainer().set(keyItemId, PersistentDataType.STRING, itemId);
            Integer tier = parseTier(itemId);
            if (tier != null) meta.getPersistentDataContainer().set(keyTier, PersistentDataType.INTEGER, tier);
            meta.getPersistentDataContainer().set(keyQuality, PersistentDataType.INTEGER, 0);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static Integer parseTier(String itemId) {
        if (itemId == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("_T(\\d+)$").matcher(itemId);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return null;
    }

    private static Material guessMaterial(String itemId) {
        if (itemId == null) return Material.PAPER;
        String u = itemId.toUpperCase();
        if (u.contains("LEATHER")) return Material.LEATHER;
        if (u.contains("BEEF") || u.contains("PORK") || u.contains("MUTTON") || u.contains("CHICKEN")) return Material.BEEF;
        if (u.contains("FEATHER")) return Material.FEATHER;
        if (u.contains("BONE")) return Material.BONE;
        if (u.contains("GUNPOWDER")) return Material.GUNPOWDER;
        if (u.contains("ROTTEN")) return Material.ROTTEN_FLESH;
        if (u.contains("STRING")) return Material.STRING;
        if (u.contains("SLIME")) return Material.SLIME_BALL;
        if (u.contains("PEARL")) return Material.ENDER_PEARL;
        if (u.contains("BLAZE")) return Material.BLAZE_ROD;
        if (u.contains("WITHER")) return Material.NETHER_STAR;
        return Material.PAPER;
    }
}
