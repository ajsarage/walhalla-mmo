package walhalla.mmo.rpgmenu;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.spells.SpellCatalogBridge;
import walhalla.mmo.core.api.spells.SpellCatalogEntry;
import walhalla.mmo.core.api.spells.SpellLoadoutBridge;
import walhalla.mmo.core.api.professions.ProfessionsUiBridge;
import walhalla.mmo.core.api.market.MarketUiBridge;
import walhalla.mmo.core.progress.PlayerData;

/**
 * Player-facing RPG menu (no commands).
 * Phase 3: minimal Abilities submenu to equip spells into slots.
 */
public class RpgMenuController implements Listener {

    private static final String TITLE_MAIN = ChatColor.DARK_GRAY + "RPG MENU";
    private static final String TITLE_ABIL = ChatColor.DARK_GRAY + "Habilidades";

    private final JavaPlugin plugin;
    private final Set<UUID> viewingMain = new HashSet<>();
    private final Set<UUID> viewingAbilities = new HashSet<>();

    public RpgMenuController(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (CoreAPI.getLifecycleState(p.getUniqueId()) != PlayerData.PlayerLifecycleState.ACTIVE) return;

        switch (e.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {}
            default -> { return; }
        }

        ItemStack it = p.getInventory().getItemInMainHand();
        if (it == null || it.getType() != Material.NETHER_STAR) return;
        ItemMeta meta = it.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        if (!ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("RPG MENU")) return;

        e.setCancelled(true);
        openMain(p);
    }

    private void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, TITLE_MAIN);
        inv.setItem(11, button(Material.BOOK, ChatColor.AQUA + "Profesiones", List.of(ChatColor.GRAY + "Crafteo, farmeo, ventas")));
        inv.setItem(13, button(Material.ENCHANTED_BOOK, ChatColor.AQUA + "Habilidades", List.of(ChatColor.GRAY + "Gestiona tus slots")));
        inv.setItem(15, button(Material.EMERALD, ChatColor.AQUA + "Mercado", List.of(ChatColor.GRAY + "Subasta y trade")));

        inv.setItem(26, button(Material.BARRIER, ChatColor.RED + "Cerrar", List.of()));
        viewingMain.add(p.getUniqueId());
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openAbilities(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, TITLE_ABIL);

        SpellLoadoutBridge loadout = Bukkit.getServicesManager().load(SpellLoadoutBridge.class);
        SpellCatalogBridge catalog = Bukkit.getServicesManager().load(SpellCatalogBridge.class);

        String prim = loadout != null ? loadout.getEquipped(p.getUniqueId(), "PRIMARY").orElse(null) : null;
        String sec = loadout != null ? loadout.getEquipped(p.getUniqueId(), "SECONDARY").orElse(null) : null;
        String spec = loadout != null ? loadout.getEquipped(p.getUniqueId(), "SPECIAL").orElse(null) : null;

        inv.setItem(11, slotButton("PRIMARY", prim, catalog));
        inv.setItem(13, slotButton("SECONDARY", sec, catalog));
        inv.setItem(15, slotButton("SPECIAL", spec, catalog));

        inv.setItem(26, button(Material.ARROW, ChatColor.YELLOW + "Volver", List.of(ChatColor.GRAY + "Regresar al menú principal")));

        viewingAbilities.add(p.getUniqueId());
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private ItemStack slotButton(String slot, String spellId, SpellCatalogBridge catalog) {
        String title = ChatColor.AQUA + slot;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Clic para cambiar");

        String nameLine = ChatColor.YELLOW + "(vacío)";
        if (spellId != null && catalog != null) {
            SpellCatalogEntry e = catalog.getSpell(spellId);
            if (e != null) {
                nameLine = ChatColor.GREEN + e.displayName();
                lore.add(ChatColor.DARK_GRAY + "ID: " + spellId);
            } else {
                nameLine = ChatColor.RED + spellId;
            }
        }
        lore.add(nameLine);

        return button(Material.PAPER, title, lore);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String title = e.getView().getTitle();
        if (title.equals(TITLE_MAIN) && viewingMain.contains(p.getUniqueId())) {
            e.setCancelled(true);
            if (e.getRawSlot() == 11) {
                ProfessionsUiBridge ui = Bukkit.getServicesManager().load(ProfessionsUiBridge.class);
                if (ui == null) {
                    p.sendMessage(ChatColor.RED + "Profesiones no disponibles.");
                    return;
                }
                ui.openProfessionsMenu(p);
            } else if (e.getRawSlot() == 13) {
                openAbilities(p);
            } else if (e.getRawSlot() == 15) {
                MarketUiBridge market = Bukkit.getServicesManager().load(MarketUiBridge.class);
                if (market == null) {
                    p.sendMessage(ChatColor.RED + "Mercado no disponible.");
                    return;
                }
                market.openMarketMenu(p);
            } else if (e.getRawSlot() == 26) {
                p.closeInventory();
            }
            return;
        }

        if (title.equals(TITLE_ABIL) && viewingAbilities.contains(p.getUniqueId())) {
            e.setCancelled(true);

            if (e.getRawSlot() == 26) {
                openMain(p);
                return;
            }

            String slot = switch (e.getRawSlot()) {
                case 11 -> "PRIMARY";
                case 13 -> "SECONDARY";
                case 15 -> "SPECIAL";
                default -> null;
            };
            if (slot == null) return;

            cycleSpell(p, slot);
            openAbilities(p);
        }
    }

    private void cycleSpell(Player p, String slotId) {
        SpellLoadoutBridge loadout = Bukkit.getServicesManager().load(SpellLoadoutBridge.class);
        SpellCatalogBridge catalog = Bukkit.getServicesManager().load(SpellCatalogBridge.class);
        if (loadout == null || catalog == null) {
            p.sendMessage(ChatColor.RED + "Habilidades no disponibles.");
            return;
        }

        List<String> allowedBranches = CoreAPI.getChosenBranches(p.getUniqueId()).stream()
                .map(s -> s.toUpperCase(Locale.ROOT)).toList();

        List<String> candidates = new ArrayList<>();
        for (String id : catalog.getAllSpellIds()) {
            SpellCatalogEntry entry = catalog.getSpell(id);
            if (entry != null) {
                String element = entry.element() != null ? entry.element().toUpperCase(Locale.ROOT) : "GENERIC";
                // element/branchId aligned in our starter templates
                if (allowedBranches.contains(element)) candidates.add(id);
            }
        }
        candidates.sort(String::compareToIgnoreCase);

        if (candidates.isEmpty()) {
            p.sendMessage(ChatColor.RED + "No hay habilidades para tus ramas.");
            return;
        }

        String current = loadout.getEquipped(p.getUniqueId(), slotId).orElse(null);
        int idx = current == null ? -1 : candidates.indexOf(current);
        int next = (idx + 1) % candidates.size();
        loadout.setEquipped(p.getUniqueId(), slotId, candidates.get(next));
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.4f);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        viewingMain.remove(p.getUniqueId());
        viewingAbilities.remove(p.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if ((title.equals(TITLE_MAIN) && viewingMain.contains(p.getUniqueId())) ||
            (title.equals(TITLE_ABIL) && viewingAbilities.contains(p.getUniqueId()))) {
            e.setCancelled(true);
        }
    }

    private ItemStack button(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            im.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) im.setLore(lore);
            it.setItemMeta(im);
        }
        return it;
    }
}
