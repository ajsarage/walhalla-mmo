package walhalla.mmo.core.api.professions;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays; // Added import

public interface ProfessionsUiBridge {
    void openProfessionsMenu(Player player);
    void openCraftingMenu(Player player);

    default Inventory createMenu(String title, int size) {
        return Bukkit.createInventory(null, size, title);
    }

    default ItemStack createMenuItem(String name, String... lore) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
