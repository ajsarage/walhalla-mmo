package walhalla.mmo.professions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class CustomItemFactory {

    private final Plugin plugin;
    private final NamespacedKey keyItemId;

    public CustomItemFactory(Plugin plugin) {
        this.plugin = plugin;
        this.keyItemId = ItemIds.namespacedKey(plugin);
    }

    public ItemStack createResource(String itemId, Material material, int amount, String displayName, List<String> lore) {
        if (amount <= 0) amount = 1;
        if (material == null) material = Material.PAPER;
        ItemStack it = new ItemStack(material, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RESET + displayName);
            List<String> l = new ArrayList<>();
            if (lore != null) l.addAll(lore);
            l.add(ChatColor.DARK_GRAY + "ID: " + itemId);
            meta.setLore(l);
            meta.getPersistentDataContainer().set(keyItemId, PersistentDataType.STRING, itemId);
            it.setItemMeta(meta);
        }
        return it;
    }

    public String getItemId(ItemStack it) {
        if (it == null) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(keyItemId, PersistentDataType.STRING);
    }
}
