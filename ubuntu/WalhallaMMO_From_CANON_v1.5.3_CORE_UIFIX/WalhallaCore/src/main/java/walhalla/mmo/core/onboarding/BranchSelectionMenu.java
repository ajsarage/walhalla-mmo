package walhalla.mmo.core.onboarding;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.progress.PlayerData;
import walhalla.mmo.core.progress.PlayerProgressService;

/**
 * Player-facing onboarding UI.
 * Players never type commands: everything happens via menu.
 */
public class BranchSelectionMenu implements Listener {

    private static final String TITLE = ChatColor.DARK_GRAY + "Selección inicial (elige 4)";

    private final JavaPlugin plugin;
    private final PlayerProgressService progress;

    // Track open menus to identify our inventory
    private final Set<UUID> viewing = new HashSet<>();

    public BranchSelectionMenu(JavaPlugin plugin, PlayerProgressService progress) {
        this.plugin = plugin;
        this.progress = progress;
    }

    public void open(Player p) {
        PlayerData data = progress.getOrLoad(p.getUniqueId());

        Inventory inv = Bukkit.createInventory(p, 54, TITLE);

        // Layout:
        // 0..8 header
        // combat 10..16
        // professions 28..34 etc
        // confirm at 49, info at 4
        inv.setItem(4, infoItem(data));

        int slot = 10;
        for (String br : BranchRegistry.combatBranches()) {
            inv.setItem(slot, branchItem(br, data.getChosenBranches().contains(br), true));
            slot++;
        }

        slot = 28;
        for (String pr : BranchRegistry.professionBranches()) {
            inv.setItem(slot, branchItem(pr, data.getChosenBranches().contains(pr), false));
            slot++;
            if (slot == 35) slot = 37; // spacing
        }

        inv.setItem(49, confirmItem(data));
        // Fill borders lightly
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null && (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8)) {
                inv.setItem(i, filler());
            }
        }

        viewing.add(p.getUniqueId());
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private ItemStack filler() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(" ");
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack infoItem(PlayerData data) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.GOLD + "Instrucciones");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "• Selecciona exactamente " + ChatColor.AQUA + "4" + ChatColor.GRAY + " ramas.");
            lore.add(ChatColor.GRAY + "• No puedes jugar hasta confirmar.");
            lore.add(ChatColor.GRAY + "• Selección actual: " + ChatColor.YELLOW + data.getChosenBranches().size() + "/4");
            lore.add(" ");
            lore.add(ChatColor.DARK_GRAY + "Combat: 4 | Oficios: 8");
            m.setLore(lore);
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack branchItem(String id, boolean selected, boolean combat) {
        Material mat;
        if (combat) {
            mat = switch (id) {
                case BranchRegistry.BR_GUERRERO -> Material.IRON_SWORD;
                case BranchRegistry.BR_TANQUE -> Material.SHIELD;
                case BranchRegistry.BR_CAZADOR -> Material.BOW;
                case BranchRegistry.BR_MAGO -> Material.BLAZE_ROD;
                default -> Material.PAPER;
            };
        } else {
            mat = switch (id) {
                case BranchRegistry.PR_MINERO -> Material.IRON_PICKAXE;
                case BranchRegistry.PR_LENADOR -> Material.IRON_AXE;
                case BranchRegistry.PR_AGRICULTOR -> Material.WHEAT;
                case BranchRegistry.PR_COCINERO -> Material.COOKED_BEEF;
                case BranchRegistry.PR_PESCADOR -> Material.FISHING_ROD;
                case BranchRegistry.PR_CARTOGRAFO -> Material.MAP;
                case BranchRegistry.PR_HERRERO -> Material.ANVIL;
                case BranchRegistry.PR_DON_DE_HIERBAS -> Material.FERN;
                default -> Material.PAPER;
            };
        }

        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName((selected ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✖ ") + ChatColor.WHITE + id);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + (combat ? "Rama de combate" : "Oficio"));
            lore.add(" ");
            lore.add(selected ? ChatColor.GREEN + "Seleccionado" : ChatColor.YELLOW + "Click para seleccionar");
            m.setLore(lore);
            m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack confirmItem(PlayerData data) {
        boolean ok = data.getChosenBranches().size() == 4;
        ItemStack it = new ItemStack(ok ? Material.LIME_CONCRETE : Material.RED_CONCRETE);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ok ? ChatColor.GREEN + "CONFIRMAR" : ChatColor.RED + "CONFIRMAR");
            List<String> lore = new ArrayList<>();
            if (ok) {
                lore.add(ChatColor.GRAY + "Click para confirmar y empezar.");
            } else {
                lore.add(ChatColor.GRAY + "Necesitas " + ChatColor.AQUA + "4" + ChatColor.GRAY + " ramas.");
                lore.add(ChatColor.GRAY + "Actual: " + ChatColor.YELLOW + data.getChosenBranches().size() + "/4");
            }
            m.setLore(lore);
            it.setItemMeta(m);
        }
        return it;
    }

    private boolean isOurTitle(InventoryView view) {
    if (view == null) return false;

    String legacyTitle = LegacyComponentSerializer
            .legacySection()
            .serialize(view.title());

    return TITLE.equals(legacyTitle);
}

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!isOurTitle(e.getView())) return;

        e.setCancelled(true);

        PlayerData data = progress.getOrLoad(p.getUniqueId());

        // Only allowed while choosing
        if (data.getLifecycleState() != PlayerData.PlayerLifecycleState.CHOOSING_BRANCHES) {
            p.closeInventory();
            viewing.remove(p.getUniqueId());
            return;
        }

        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Confirm button
        if (slot == 49) {
            if (data.getChosenBranches().size() != 4) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                p.sendTitle(ChatColor.RED + "Elige 4 ramas", ChatColor.GRAY + "Actual: " + data.getChosenBranches().size() + "/4", 5, 30, 10);
                return;
            }

            data.setLifecycleState(PlayerData.PlayerLifecycleState.ACTIVE);
            progress.saveNow(data);
            // Give RPG Menu access item (players never use commands)
            org.bukkit.inventory.ItemStack menuItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_STAR);
            org.bukkit.inventory.meta.ItemMeta mm = menuItem.getItemMeta();
            if (mm != null) {
                mm.setDisplayName(org.bukkit.ChatColor.AQUA + "RPG MENU");
                java.util.List<String> lore = new java.util.ArrayList<>();
                lore.add(org.bukkit.ChatColor.GRAY + "Clic derecho para abrir");
                mm.setLore(lore);
                menuItem.setItemMeta(mm);
            }
            p.getInventory().addItem(menuItem);

            p.closeInventory();
            viewing.remove(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f);
            p.sendTitle(ChatColor.GREEN + "¡Listo!", ChatColor.GRAY + "Bienvenido a Walhalla.", 10, 50, 20);
            return;
        }

        // Determine branchId from item name
        String branchId = extractBranchId(clicked);
        if (branchId == null) return;

        // Toggle selection
        if (data.getChosenBranches().contains(branchId)) {
            data.getChosenBranches().remove(branchId);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f);
        } else {
            if (data.getChosenBranches().size() >= 4) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                p.sendTitle(ChatColor.RED + "Máximo 4", ChatColor.GRAY + "Desselecciona una opción primero.", 5, 25, 10);
                return;
            }
            data.getChosenBranches().add(branchId);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
        }

        // Refresh view
        Bukkit.getScheduler().runTask(plugin, () -> open(p));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (isOurTitle(e.getView())) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!isOurTitle(e.getView())) return;

        PlayerData data = progress.getOrLoad(p.getUniqueId());
        viewing.remove(p.getUniqueId());

        // If still choosing, reopen shortly (player cannot bypass)
        if (data.getLifecycleState() == PlayerData.PlayerLifecycleState.CHOOSING_BRANCHES) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;
                PlayerData d2 = progress.getOrLoad(p.getUniqueId());
                if (d2.getLifecycleState() == PlayerData.PlayerLifecycleState.CHOOSING_BRANCHES) {
                    open(p);
                }
            }, 10L);
        }
    }

    private String extractBranchId(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        if (m == null || !m.hasDisplayName()) return null;
        String raw = ChatColor.stripColor(m.getDisplayName());
        if (raw == null) return null;
        // raw example: "✔ GUERRERO" or "✖ MINERO"
        raw = raw.replace("✔", "").replace("✖", "").trim();
        if (BranchRegistry.isValidBranchId(raw)) return raw;
        return null;
    }
}
