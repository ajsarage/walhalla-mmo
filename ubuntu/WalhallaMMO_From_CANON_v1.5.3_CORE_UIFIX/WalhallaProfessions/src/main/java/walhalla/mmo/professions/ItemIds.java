package walhalla.mmo.professions;

import org.bukkit.NamespacedKey;

public final class ItemIds {
    public static final String KEY_ITEM_ID = "wh_item_id";
    public static NamespacedKey namespacedKey(org.bukkit.plugin.Plugin plugin) {
        return new NamespacedKey(plugin, KEY_ITEM_ID);
    }
    private ItemIds() {}
}
