package walhalla.mmo.core.api.professions;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ProfessionsUiBridgeImpl implements ProfessionsUiBridge {

    @Override
    public void openProfessionsMenu(Player player) {
        Inventory menu = createMenu("Professions", 9);
        ItemStack item1 = createMenuItem("Profession 1", "Description of Profession 1");
        ItemStack item2 = createMenuItem("Profession 2", "Description of Profession 2");
        ItemStack item3 = createMenuItem("Profession 3", "Description of Profession 3");

        menu.setItem(0, item1);
        menu.setItem(1, item2);
        menu.setItem(2, item3);

        player.openInventory(menu);
    }

    @Override
    public void openCraftingMenu(Player player) {
        Inventory menu = createMenu("Crafting", 9);
        ItemStack item1 = createMenuItem("Item 1", "Description of Item 1");
        ItemStack item2 = createMenuItem("Item 2", "Description of Item 2");
        ItemStack item3 = createMenuItem("Item 3", "Description of Item 3");

        menu.setItem(0, item1);
        menu.setItem(1, item2);
        menu.setItem(2, item3);

        player.openInventory(menu);
    }

    @Override
    public Inventory createMenu(String title, int size) {
        return Bukkit.createInventory(null, size, title);
    }

    @Override
    public ItemStack createMenuItem(String name, String... lore) {
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
