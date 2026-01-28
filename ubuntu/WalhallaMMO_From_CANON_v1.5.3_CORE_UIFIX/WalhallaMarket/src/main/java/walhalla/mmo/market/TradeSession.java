package walhalla.mmo.market;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.economy.Currency;
import walhalla.mmo.core.api.economy.EconomyBridge;

import java.time.Instant;
import java.util.*;

/**
 * Direct player-to-player trade UI (secondary channel; auction house is primary).
 *
 * Atomicity:
 * - We only finalize when both confirm.
 * - Currency is withdrawn from each side, then deposited to the other.
 * - If any withdraw fails, nothing executes.
 */
public final class TradeSession {

    private static final String TITLE = ChatColor.DARK_GRAY + "Trade";

    private final JavaPlugin plugin;
    private final EconomyBridge econ;
    private final Player a;
    private final Player b;
    private final Runnable onEnd;

    private final Inventory invA;
    private final Inventory invB;

    // Offers (items are stored in the open inventories)
    private long offerA_Wcoin = 0;
    private long offerA_Wcraft = 0;
    private long offerB_Wcoin = 0;
    private long offerB_Wcraft = 0;

    private boolean confirmA = false;
    private boolean confirmB = false;

    private long lastInteraction = Instant.now().toEpochMilli();

    public TradeSession(JavaPlugin plugin, EconomyBridge econ, Player a, Player b, Runnable onEnd) {
        this.plugin = plugin;
        this.econ = econ;
        this.a = a;
        this.b = b;
        this.onEnd = onEnd;

        this.invA = Bukkit.createInventory(a, 54, TITLE + " | " + b.getName());
        this.invB = Bukkit.createInventory(b, 54, TITLE + " | " + a.getName());

        layout(invA);
        layout(invB);
    }

    public boolean isTradeTitle(String title) {
        return title != null && ChatColor.stripColor(title).startsWith("Trade");
    }

    public void open() {
        refresh();
        a.openInventory(invA);
        b.openInventory(invB);
        a.sendMessage(ChatColor.AQUA + "Trade iniciado con " + b.getName() + ". Coloca ítems y confirma.");
        b.sendMessage(ChatColor.AQUA + "Trade iniciado con " + a.getName() + ". Coloca ítems y confirma.");
    }

    private void layout(Inventory inv) {
        // separator column (slots 4,13,22,31,40,49)
        int[] sep = {4,13,22,31,40,49};
        for (int s : sep) inv.setItem(s, glass());

        // confirm + cancel buttons
        inv.setItem(45, button(Material.GREEN_CONCRETE, ChatColor.GREEN + "Confirmar", List.of(ChatColor.GRAY + "Ambos deben confirmar")));
        inv.setItem(53, button(Material.RED_CONCRETE, ChatColor.RED + "Cancelar", List.of(ChatColor.GRAY + "Cierra el trade")));

        // currency controls
        inv.setItem(46, moneyButton(Material.GOLD_INGOT, ChatColor.GOLD + "WCoin", 0));
        inv.setItem(47, moneyButton(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "WCraft", 0));
        inv.setItem(51, button(Material.GREEN_CONCRETE, ChatColor.GREEN + "+10", List.of()));
        inv.setItem(52, button(Material.RED_CONCRETE, ChatColor.RED + "-10", List.of()));
    }

    private ItemStack glass() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta im = it.getItemMeta();
        if (im != null) { im.setDisplayName(" "); it.setItemMeta(im); }
        return it;
    }

    private ItemStack moneyButton(Material mat, String name, long amount) {
        return button(mat, name, List.of(ChatColor.GRAY + "Oferta: " + ChatColor.WHITE + amount,
                ChatColor.DARK_GRAY + "Clic para seleccionar", ChatColor.DARK_GRAY + "Usa +10/-10"));
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

    private void refresh() {
        // mirror offers for each side
        invA.setItem(46, moneyButton(Material.GOLD_INGOT, ChatColor.GOLD + "WCoin", offerA_Wcoin));
        invA.setItem(47, moneyButton(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "WCraft", offerA_Wcraft));

        invB.setItem(46, moneyButton(Material.GOLD_INGOT, ChatColor.GOLD + "WCoin", offerB_Wcoin));
        invB.setItem(47, moneyButton(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "WCraft", offerB_Wcraft));

        invA.setItem(50, button(Material.PAPER, ChatColor.YELLOW + "Estado",
                List.of(statusLine(confirmA, a.getName()), statusLine(confirmB, b.getName()))));
        invB.setItem(50, button(Material.PAPER, ChatColor.YELLOW + "Estado",
                List.of(statusLine(confirmB, b.getName()), statusLine(confirmA, a.getName()))));
    }

    private String statusLine(boolean ok, String who) {
        return (ok ? ChatColor.GREEN : ChatColor.RED) + (ok ? "✔ " : "✖ ") + ChatColor.GRAY + who + " confirmado";
    }

    private boolean isOfferSlot(int raw) {
        // left side (0-3,9-12,18-21,27-30,36-39) + right side (5-8,14-17,23-26,32-35,41-44)
        if (raw >= 0 && raw <= 44) {
            int col = raw % 9;
            return col != 4;
        }
        return false;
    }

    private boolean isLeft(int raw) {
        int col = raw % 9;
        return col < 4;
    }

    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        boolean isA = p.getUniqueId().equals(a.getUniqueId());
        boolean isB = p.getUniqueId().equals(b.getUniqueId());
        if (!isA && !isB) return;

        InventoryView view = e.getView();
        if (!isTradeTitle(view.getTitle())) return;

        int raw = e.getRawSlot();

        // allow placing items only in your own offer area
        if (isOfferSlot(raw)) {
            if ((isA && isLeft(raw)) || (isB && isLeft(raw))) {
                // left side is always "your offer" per viewer, so allow
                e.setCancelled(false);
                // any edit resets confirmations
                confirmA = false;
                confirmB = false;
                lastInteraction = Instant.now().toEpochMilli();
                Bukkit.getScheduler().runTask(plugin, this::refresh);
                return;
            } else {
                e.setCancelled(true);
                return;
            }
        }

        e.setCancelled(true);

        // currency selection
        if (raw == 46) {
            setSelected(p, Currency.WCOIN);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
            return;
        }
        if (raw == 47) {
            setSelected(p, Currency.WCRAFT);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
            return;
        }

        if (raw == 51 || raw == 52) {
            long delta = raw == 51 ? 10 : -10;
            Currency sel = getSelected(p);
            if (sel == Currency.WCRAFT) {
                if (isA) offerA_Wcraft = Math.max(0, offerA_Wcraft + delta);
                else offerB_Wcraft = Math.max(0, offerB_Wcraft + delta);
            } else {
                if (isA) offerA_Wcoin = Math.max(0, offerA_Wcoin + delta);
                else offerB_Wcoin = Math.max(0, offerB_Wcoin + delta);
            }
            confirmA = false;
            confirmB = false;
            refresh();
            return;
        }

        if (raw == 45) {
            if (isA) confirmA = true;
            if (isB) confirmB = true;
            refresh();
            if (confirmA && confirmB) finalizeTrade();
            return;
        }

        if (raw == 53) {
            end("Trade cancelado.");
            return;
        }
    }

    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        boolean isA = p.getUniqueId().equals(a.getUniqueId());
        boolean isB = p.getUniqueId().equals(b.getUniqueId());
        if (!isA && !isB) return;
        if (!isTradeTitle(e.getView().getTitle())) return;

        // cancel drags that touch forbidden slots
        for (int raw : e.getRawSlots()) {
            if (isOfferSlot(raw)) {
                if (raw % 9 == 4) { e.setCancelled(true); return; }
                // allow only in own offer side (left for each viewer)
                if (!(raw % 9 < 4)) { e.setCancelled(true); return; }
            } else if (raw < 54) {
                e.setCancelled(true);
                return;
            }
        }
        confirmA = false;
        confirmB = false;
        Bukkit.getScheduler().runTask(plugin, this::refresh);
    }

    public void onClose(InventoryCloseEvent e) {
        // if either closes, cancel and return items
        end("Trade cancelado.");
    }

    private final Map<UUID, Currency> selected = new HashMap<>();

    private void setSelected(Player p, Currency c) { selected.put(p.getUniqueId(), c); }
    private Currency getSelected(Player p) { return selected.getOrDefault(p.getUniqueId(), Currency.WCOIN); }

    private void finalizeTrade() {
        String tx = "P2P:" + a.getUniqueId() + ":" + b.getUniqueId() + ":" + Instant.now().toEpochMilli();

        // Withdraw from A
        if (offerA_Wcoin > 0 && !econ.withdraw(a.getUniqueId(), Currency.WCOIN, offerA_Wcoin, "P2P_TRADE", tx + ":A:WCOIN")) {
            a.sendMessage(ChatColor.RED + "No tienes WCoin suficiente.");
            b.sendMessage(ChatColor.RED + "Trade falló: A sin fondos.");
            confirmA = false; confirmB = false; refresh();
            return;
        }
        if (offerA_Wcraft > 0 && !econ.withdraw(a.getUniqueId(), Currency.WCRAFT, offerA_Wcraft, "P2P_TRADE", tx + ":A:WCRAFT")) {
            if (offerA_Wcoin > 0) econ.deposit(a.getUniqueId(), Currency.WCOIN, offerA_Wcoin, "P2P_REFUND", tx + ":A:REF");
            a.sendMessage(ChatColor.RED + "No tienes WCraft suficiente.");
            b.sendMessage(ChatColor.RED + "Trade falló: A sin fondos.");
            confirmA = false; confirmB = false; refresh();
            return;
        }

        // Withdraw from B
        if (offerB_Wcoin > 0 && !econ.withdraw(b.getUniqueId(), Currency.WCOIN, offerB_Wcoin, "P2P_TRADE", tx + ":B:WCOIN")) {
            // refund A
            if (offerA_Wcoin > 0) econ.deposit(a.getUniqueId(), Currency.WCOIN, offerA_Wcoin, "P2P_REFUND", tx + ":A:R1");
            if (offerA_Wcraft > 0) econ.deposit(a.getUniqueId(), Currency.WCRAFT, offerA_Wcraft, "P2P_REFUND", tx + ":A:R2");
            a.sendMessage(ChatColor.RED + "Trade falló: B sin WCoin.");
            b.sendMessage(ChatColor.RED + "No tienes WCoin suficiente.");
            confirmA = false; confirmB = false; refresh();
            return;
        }
        if (offerB_Wcraft > 0 && !econ.withdraw(b.getUniqueId(), Currency.WCRAFT, offerB_Wcraft, "P2P_TRADE", tx + ":B:WCRAFT")) {
            // refund A and B wcoin
            if (offerA_Wcoin > 0) econ.deposit(a.getUniqueId(), Currency.WCOIN, offerA_Wcoin, "P2P_REFUND", tx + ":A:R3");
            if (offerA_Wcraft > 0) econ.deposit(a.getUniqueId(), Currency.WCRAFT, offerA_Wcraft, "P2P_REFUND", tx + ":A:R4");
            if (offerB_Wcoin > 0) econ.deposit(b.getUniqueId(), Currency.WCOIN, offerB_Wcoin, "P2P_REFUND", tx + ":B:R1");
            a.sendMessage(ChatColor.RED + "Trade falló: B sin WCraft.");
            b.sendMessage(ChatColor.RED + "No tienes WCraft suficiente.");
            confirmA = false; confirmB = false; refresh();
            return;
        }

        // Collect offered items from each UI (left side)
        List<ItemStack> itemsA = collectOffer(invA);
        List<ItemStack> itemsB = collectOffer(invB);

        // Clear offer slots
        clearOffer(invA);
        clearOffer(invB);

        // Deposit currencies to other side
        if (offerA_Wcoin > 0) econ.deposit(b.getUniqueId(), Currency.WCOIN, offerA_Wcoin, "P2P_TRADE_IN", tx + ":IN:B:WCOIN");
        if (offerA_Wcraft > 0) econ.deposit(b.getUniqueId(), Currency.WCRAFT, offerA_Wcraft, "P2P_TRADE_IN", tx + ":IN:B:WCRAFT");
        if (offerB_Wcoin > 0) econ.deposit(a.getUniqueId(), Currency.WCOIN, offerB_Wcoin, "P2P_TRADE_IN", tx + ":IN:A:WCOIN");
        if (offerB_Wcraft > 0) econ.deposit(a.getUniqueId(), Currency.WCRAFT, offerB_Wcraft, "P2P_TRADE_IN", tx + ":IN:A:WCRAFT");

        // Give items to each side
        giveAll(a, itemsB);
        giveAll(b, itemsA);

        a.playSound(a.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        b.playSound(b.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

        end(ChatColor.GREEN + "Trade completado.");
    }

    private List<ItemStack> collectOffer(Inventory inv) {
        List<ItemStack> out = new ArrayList<>();
        for (int raw = 0; raw <= 44; raw++) {
            if (!isOfferSlot(raw)) continue;
            if (!(raw % 9 < 4)) continue; // left side only
            ItemStack it = inv.getItem(raw);
            if (it != null && it.getType() != Material.AIR) out.add(it.clone());
        }
        return out;
    }

    private void clearOffer(Inventory inv) {
        for (int raw = 0; raw <= 44; raw++) {
            if (!isOfferSlot(raw)) continue;
            if (!(raw % 9 < 4)) continue;
            inv.setItem(raw, null);
        }
    }

    private void giveAll(Player p, List<ItemStack> items) {
        for (ItemStack it : items) {
            HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(it);
            leftover.values().forEach(li -> p.getWorld().dropItemNaturally(p.getLocation(), li));
        }
    }

    private void end(String msg) {
        try {
            // Return any offered items to owners (from each UI left side)
            returnToOwner(a, invA);
            returnToOwner(b, invB);
        } catch (Exception ignored) {}

        if (a.isOnline()) a.closeInventory();
        if (b.isOnline()) b.closeInventory();

        if (a.isOnline()) a.sendMessage(msg);
        if (b.isOnline()) b.sendMessage(msg);

        onEnd.run();
    }

    private void returnToOwner(Player owner, Inventory inv) {
        List<ItemStack> items = collectOffer(inv);
        clearOffer(inv);
        giveAll(owner, items);
    }
}
