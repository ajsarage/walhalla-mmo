package walhalla.mmo.market;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.economy.Currency;
import walhalla.mmo.core.api.economy.EconomyBridge;
import walhalla.mmo.core.api.market.MarketUiBridge;
import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.progress.PlayerData;

import java.time.Instant;
import java.util.*;

/**
 * Market + Direct trade UI controller.
 *
 * Player access: UI only.
 * Admin access: /walhalla-market (OP only).
 */
public final class MarketController implements Listener, MarketUiBridge {

    private static final String TITLE_MARKET = ChatColor.DARK_GRAY + "Mercado";
    private static final String TITLE_BROWSE = ChatColor.DARK_GRAY + "Mercado: Buscar";
    private static final String TITLE_CREATE = ChatColor.DARK_GRAY + "Mercado: Publicar";
    private static final String TITLE_MY = ChatColor.DARK_GRAY + "Mercado: Mis listados";

    private final JavaPlugin plugin;
    private final MarketStore store;

    // per-player draft listing while in create menu
    private final Map<UUID, Draft> drafts = new HashMap<>();

    // direct trade sessions
    private final Map<UUID, TradeSession> tradeByPlayer = new HashMap<>();

    public MarketController(JavaPlugin plugin, MarketStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override
    public void openMarketMenu(Player player) {
        if (player == null) return;
        if (CoreAPI.getLifecycleState(player.getUniqueId()) != PlayerData.PlayerLifecycleState.ACTIVE) return;

        Inventory inv = Bukkit.createInventory(player, 27, TITLE_MARKET);
        inv.setItem(11, button(Material.CHEST, ChatColor.AQUA + "Buscar", List.of(ChatColor.GRAY + "Explora listados")));
        inv.setItem(13, button(Material.ANVIL, ChatColor.AQUA + "Publicar", List.of(ChatColor.GRAY + "Poner ítems en subasta")));
        inv.setItem(15, button(Material.PAPER, ChatColor.AQUA + "Mis listados", List.of(ChatColor.GRAY + "Retirar / ver estado")));
        inv.setItem(26, button(Material.BARRIER, ChatColor.RED + "Cerrar", List.of()));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    private void openBrowse(Player p, int page) {
        Inventory inv = Bukkit.createInventory(p, 54, TITLE_BROWSE + " #" + page);

        List<MarketListing> list = store.activeListings();
        int perPage = 45;
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, list.size());

        for (int i = start; i < end; i++) {
            MarketListing l = list.get(i);
            ItemStack it = (l.item == null) ? new ItemStack(Material.BARRIER) : l.item.clone();
            it.setAmount(Math.max(1, Math.min(64, l.quantity)));
            ItemMeta im = it.getItemMeta();
            if (im != null) {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "ID: " + l.listingId);
                lore.add(ChatColor.GRAY + "Precio: " + ChatColor.GOLD + l.priceWcoin + " WCoin" +
                        (l.priceWcraft > 0 ? ChatColor.GRAY + " + " + ChatColor.LIGHT_PURPLE + l.priceWcraft + " WCraft" : ""));
                lore.add(ChatColor.YELLOW + "Clic para comprar (1x)");
                im.setLore(lore);
                it.setItemMeta(im);
            }
            inv.setItem(i - start, it);
        }

        inv.setItem(45, button(Material.ARROW, ChatColor.YELLOW + "Volver", List.of(ChatColor.GRAY + "Menú mercado")));
        inv.setItem(53, button(Material.ARROW, ChatColor.YELLOW + "Siguiente", List.of(ChatColor.GRAY + "Página siguiente")));
        p.openInventory(inv);
    }

    private void openMyListings(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54, TITLE_MY);

        List<MarketListing> list = store.activeForSeller(p.getUniqueId());
        int i = 0;
        for (MarketListing l : list) {
            if (i >= 45) break;
            ItemStack it = (l.item == null) ? new ItemStack(Material.BARRIER) : l.item.clone();
            it.setAmount(Math.max(1, Math.min(64, l.quantity)));
            ItemMeta im = it.getItemMeta();
            if (im != null) {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "ID: " + l.listingId);
                lore.add(ChatColor.GRAY + "Precio: " + ChatColor.GOLD + l.priceWcoin + " WCoin" +
                        (l.priceWcraft > 0 ? ChatColor.GRAY + " + " + ChatColor.LIGHT_PURPLE + l.priceWcraft + " WCraft" : ""));
                lore.add(ChatColor.RED + "Shift+Clic para cancelar");
                im.setLore(lore);
                it.setItemMeta(im);
            }
            inv.setItem(i++, it);
        }

        inv.setItem(45, button(Material.ARROW, ChatColor.YELLOW + "Volver", List.of(ChatColor.GRAY + "Menú mercado")));
        p.openInventory(inv);
    }

    private void openCreate(Player p) {
        Draft d = drafts.computeIfAbsent(p.getUniqueId(), id -> new Draft());
        Inventory inv = Bukkit.createInventory(p, 27, TITLE_CREATE);

        inv.setItem(13, d.item == null ? button(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Coloca ítem aquí", List.of(ChatColor.DARK_GRAY + "Arrastra un stack")) : d.item.clone());
        inv.setItem(10, moneyButton(ChatColor.GOLD + "WCoin", d.priceWcoin, Material.GOLD_INGOT));
        inv.setItem(16, moneyButton(ChatColor.LIGHT_PURPLE + "WCraft", d.priceWcraft, Material.AMETHYST_SHARD));

        inv.setItem(19, button(Material.GREEN_CONCRETE, ChatColor.GREEN + "+1", List.of(ChatColor.GRAY + "Añade al último seleccionado")));
        inv.setItem(20, button(Material.GREEN_CONCRETE, ChatColor.GREEN + "+10", List.of()));
        inv.setItem(21, button(Material.GREEN_CONCRETE, ChatColor.GREEN + "+100", List.of()));
        inv.setItem(23, button(Material.RED_CONCRETE, ChatColor.RED + "-1", List.of(ChatColor.GRAY + "Resta al último seleccionado")));
        inv.setItem(24, button(Material.RED_CONCRETE, ChatColor.RED + "-10", List.of()));
        inv.setItem(25, button(Material.RED_CONCRETE, ChatColor.RED + "-100", List.of()));

        inv.setItem(22, button(Material.EMERALD_BLOCK, ChatColor.AQUA + "Publicar", List.of(ChatColor.GRAY + "Cobro -> escrow -> listo")));
        inv.setItem(26, button(Material.ARROW, ChatColor.YELLOW + "Volver", List.of(ChatColor.GRAY + "Menú mercado")));

        p.openInventory(inv);
    }

    private ItemStack moneyButton(String title, long amount, Material mat) {
        return button(mat, title, List.of(ChatColor.GRAY + "Actual: " + ChatColor.WHITE + amount,
                ChatColor.DARK_GRAY + "Selecciona esta moneda y usa + / -"));
    }

    // =====================================================================
    // Events
    // =====================================================================

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();

        if (title.equals(TITLE_MARKET)) {
            e.setCancelled(true);
            if (e.getRawSlot() == 11) openBrowse(p, 1);
            else if (e.getRawSlot() == 13) openCreate(p);
            else if (e.getRawSlot() == 15) openMyListings(p);
            else if (e.getRawSlot() == 26) p.closeInventory();
            return;
        }

        if (title.startsWith(TITLE_BROWSE)) {
            e.setCancelled(true);
            if (e.getRawSlot() == 45) { openMarketMenu(p); return; }
            if (e.getRawSlot() == 53) {
                int page = parsePage(title);
                openBrowse(p, page + 1);
                return;
            }
            if (e.getRawSlot() < 45) {
                ItemStack clicked = e.getCurrentItem();
                if (clicked == null) return;
                String id = extractListingId(clicked);
                if (id == null) return;
                attemptBuy(p, id, 1);
                openBrowse(p, parsePage(title)); // refresh
            }
            return;
        }

        if (title.equals(TITLE_MY)) {
            e.setCancelled(true);
            if (e.getRawSlot() == 45) { openMarketMenu(p); return; }
            if (e.getRawSlot() < 45 && e.isShiftClick()) {
                ItemStack clicked = e.getCurrentItem();
                if (clicked == null) return;
                String id = extractListingId(clicked);
                if (id == null) return;
                cancelListing(p, id);
                openMyListings(p);
            }
            return;
        }

        if (title.equals(TITLE_CREATE)) {
            // allow placing an item in slot 13
            if (e.getRawSlot() == 13) {
                // allow normal item move into slot 13
                e.setCancelled(false);
                return;
            }
            e.setCancelled(true);

            Draft d = drafts.computeIfAbsent(p.getUniqueId(), id -> new Draft());

            if (e.getRawSlot() == 10) { d.lastCurrency = Currency.WCOIN; openCreate(p); return; }
            if (e.getRawSlot() == 16) { d.lastCurrency = Currency.WCRAFT; openCreate(p); return; }

            long delta = 0;
            if (e.getRawSlot() == 19) delta = 1;
            if (e.getRawSlot() == 20) delta = 10;
            if (e.getRawSlot() == 21) delta = 100;
            if (e.getRawSlot() == 23) delta = -1;
            if (e.getRawSlot() == 24) delta = -10;
            if (e.getRawSlot() == 25) delta = -100;

            if (delta != 0) {
                if (d.lastCurrency == Currency.WCRAFT) d.priceWcraft = Math.max(0, d.priceWcraft + delta);
                else d.priceWcoin = Math.max(0, d.priceWcoin + delta);
                openCreate(p);
                return;
            }

            if (e.getRawSlot() == 22) {
                // snapshot item from slot 13
                ItemStack item = e.getInventory().getItem(13);
                d.item = (item == null || item.getType() == Material.AIR) ? null : item.clone();
                if (d.item == null) { p.sendMessage(ChatColor.RED + "Coloca un ítem."); return; }
                if (d.priceWcoin <= 0 && d.priceWcraft <= 0) { p.sendMessage(ChatColor.RED + "Define un precio."); return; }

                if (publishListing(p, d)) {
                    // clear slot 13
                    e.getInventory().setItem(13, null);
                    d.item = null;
                    d.priceWcoin = 0;
                    d.priceWcraft = 0;
                    openMarketMenu(p);
                } else {
                    openCreate(p);
                }
                return;
            }

            if (e.getRawSlot() == 26) {
                openMarketMenu(p);
                return;
            }
        }

        // Trade UI handled by TradeSession class
        TradeSession ts = tradeByPlayer.get(p.getUniqueId());
        if (ts != null && ts.isTradeTitle(title)) {
            ts.onClick(e);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (title.equals(TITLE_MARKET) || title.startsWith(TITLE_BROWSE) || title.equals(TITLE_MY)) {
            e.setCancelled(true);
            return;
        }
        TradeSession ts = tradeByPlayer.get(p.getUniqueId());
        if (ts != null && ts.isTradeTitle(title)) ts.onDrag(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;

        String title = e.getView().getTitle();
        if (title.equals(TITLE_CREATE)) {
            // capture item back to player on close
            Draft d = drafts.computeIfAbsent(p.getUniqueId(), id -> new Draft());
            ItemStack item = e.getInventory().getItem(13);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(item);
                leftover.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
                e.getInventory().setItem(13, null);
            }
            d.item = null;
        }

        TradeSession ts = tradeByPlayer.get(p.getUniqueId());
        if (ts != null && ts.isTradeTitle(title)) {
            ts.onClose(e);
        }
    }

    // Direct trade handshake: SHIFT + right click on player
    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player other)) return;
        Player p = e.getPlayer();

        if (!plugin.getConfig().getBoolean("trade.enabled", true)) return;
        if (!p.isSneaking()) return;

        if (CoreAPI.getLifecycleState(p.getUniqueId()) != PlayerData.PlayerLifecycleState.ACTIVE) return;
        if (CoreAPI.getLifecycleState(other.getUniqueId()) != PlayerData.PlayerLifecycleState.ACTIVE) return;

        double maxDist = plugin.getConfig().getDouble("trade.max_distance", 4.0);
        if (p.getLocation().distance(other.getLocation()) > maxDist) {
            p.sendMessage(ChatColor.RED + "Demasiado lejos para intercambiar.");
            return;
        }

        startTrade(p, other);
    }

    private void startTrade(Player a, Player b) {
        // if either already trading, ignore
        if (tradeByPlayer.containsKey(a.getUniqueId()) || tradeByPlayer.containsKey(b.getUniqueId())) {
            a.sendMessage(ChatColor.RED + "Uno de los jugadores ya está intercambiando.");
            return;
        }

        EconomyBridge econ = Bukkit.getServicesManager().load(EconomyBridge.class);
        if (econ == null) {
            a.sendMessage(ChatColor.RED + "Economía no disponible.");
            return;
        }

        TradeSession ts = new TradeSession(plugin, econ, a, b, () -> {
            tradeByPlayer.remove(a.getUniqueId());
            tradeByPlayer.remove(b.getUniqueId());
        });

        tradeByPlayer.put(a.getUniqueId(), ts);
        tradeByPlayer.put(b.getUniqueId(), ts);

        ts.open();
    }

    // =====================================================================
    // Market actions
    // =====================================================================

    private boolean publishListing(Player p, Draft d) {
        ItemStack item = d.item;
        if (item == null || item.getType() == Material.AIR) return false;

        // listing fee (sink)
        long fee = plugin.getConfig().getLong("market.fees.listing_fee_wcoin_fixed", 0L);
        if (fee > 0) {
            EconomyBridge econ = Bukkit.getServicesManager().load(EconomyBridge.class);
            if (econ == null) { p.sendMessage(ChatColor.RED + "Economía no disponible."); return false; }
            String txFee = "LISTFEE:" + p.getUniqueId() + ":" + Instant.now().toEpochMilli();
            if (!econ.withdraw(p.getUniqueId(), Currency.WCOIN, fee, "MARKET_LISTING_FEE", txFee)) {
                p.sendMessage(ChatColor.RED + "Fondos insuficientes para publicar.");
                return false;
            }
        }

        // remove item from player inventory (escrow by storage)
        int qty = item.getAmount();
        if (!removeExact(p.getInventory(), item, qty)) {
            p.sendMessage(ChatColor.RED + "No tienes ese stack disponible.");
            return false;
        }

        MarketListing l = new MarketListing();
        l.listingId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        l.sellerId = p.getUniqueId();
        l.item = item.clone();
        l.quantity = qty;
        l.priceWcoin = d.priceWcoin;
        l.priceWcraft = d.priceWcraft;
        l.createdAtEpoch = Instant.now().toEpochMilli();

        long mins = plugin.getConfig().getLong("market.listings.default_duration_minutes", 10080L);
        l.expiresAtEpoch = Instant.now().plusSeconds(mins * 60L).toEpochMilli();

        store.put(l);
        store.flush();

        p.sendMessage(ChatColor.GREEN + "Listado publicado.");
        return true;
    }

    private void attemptBuy(Player buyer, String listingId, int quantity) {
        MarketListing l = store.get(listingId).orElse(null);
        if (l == null || l.status != MarketListing.Status.ACTIVE) {
            buyer.sendMessage(ChatColor.RED + "Listado no disponible.");
            return;
        }
        if (quantity <= 0) quantity = 1;
        if (quantity > l.quantity) quantity = l.quantity;

        if (l.sellerId != null && l.sellerId.equals(buyer.getUniqueId())) {
            buyer.sendMessage(ChatColor.RED + "No puedes comprar tu propio listado.");
            return;
        }

        EconomyBridge econ = Bukkit.getServicesManager().load(EconomyBridge.class);
        if (econ == null) { buyer.sendMessage(ChatColor.RED + "Economía no disponible."); return; }

        // price is per listing (we only allow full stack buy in baseline)
        long costWcoin = l.priceWcoin;
        long costWcraft = l.priceWcraft;

        String txBase = "MKBUY:" + listingId + ":" + buyer.getUniqueId() + ":" + Instant.now().toEpochMilli();

        // Atomic withdraw (best-effort): withdraw WCoin then WCraft, refund if needed
        if (costWcoin > 0) {
            if (!econ.withdraw(buyer.getUniqueId(), Currency.WCOIN, costWcoin, "MARKET_BUY", txBase + ":WCOIN")) {
                buyer.sendMessage(ChatColor.RED + "No tienes WCoin suficiente.");
                return;
            }
        }
        if (costWcraft > 0) {
            if (!econ.withdraw(buyer.getUniqueId(), Currency.WCRAFT, costWcraft, "MARKET_BUY", txBase + ":WCRAFT")) {
                // refund wcoin
                if (costWcoin > 0) econ.deposit(buyer.getUniqueId(), Currency.WCOIN, costWcoin, "MARKET_BUY_REFUND", txBase + ":REFUND_WCOIN");
                buyer.sendMessage(ChatColor.RED + "No tienes WCraft suficiente.");
                return;
            }
        }

        // Tax (sink) on successful sale
        double taxWcoinPct = plugin.getConfig().getDouble("market.fees.sale_tax_percent_wcoin", 0.0);
        double taxWcraftPct = plugin.getConfig().getDouble("market.fees.sale_tax_percent_wcraft", 0.0);

        long taxWcoin = (taxWcoinPct <= 0) ? 0 : Math.max(0, Math.round(costWcoin * (taxWcoinPct / 100.0)));
        long taxWcraft = (taxWcraftPct <= 0) ? 0 : Math.max(0, Math.round(costWcraft * (taxWcraftPct / 100.0)));

        long payWcoin = Math.max(0, costWcoin - taxWcoin);
        long payWcraft = Math.max(0, costWcraft - taxWcraft);

        // deliver item to buyer
        ItemStack give = l.item == null ? null : l.item.clone();
        if (give == null) {
            // refund everything
            if (costWcoin > 0) econ.deposit(buyer.getUniqueId(), Currency.WCOIN, costWcoin, "MARKET_REFUND", txBase + ":R1");
            if (costWcraft > 0) econ.deposit(buyer.getUniqueId(), Currency.WCRAFT, costWcraft, "MARKET_REFUND", txBase + ":R2");
            buyer.sendMessage(ChatColor.RED + "Error de item. Reembolso hecho.");
            return;
        }
        give.setAmount(Math.max(1, quantity));

        HashMap<Integer, ItemStack> leftover = buyer.getInventory().addItem(give);
        if (!leftover.isEmpty()) {
            // inventory full -> refund
            if (costWcoin > 0) econ.deposit(buyer.getUniqueId(), Currency.WCOIN, costWcoin, "MARKET_REFUND", txBase + ":R3");
            if (costWcraft > 0) econ.deposit(buyer.getUniqueId(), Currency.WCRAFT, costWcraft, "MARKET_REFUND", txBase + ":R4");
            buyer.sendMessage(ChatColor.RED + "Inventario lleno. Reembolso hecho.");
            return;
        }

        // pay seller (minus tax)
        if (l.sellerId != null) {
            if (payWcoin > 0) econ.deposit(l.sellerId, Currency.WCOIN, payWcoin, "MARKET_SALE", txBase + ":PAY_WCOIN");
            if (payWcraft > 0) econ.deposit(l.sellerId, Currency.WCRAFT, payWcraft, "MARKET_SALE", txBase + ":PAY_WCRAFT");
        }

        l.quantity -= quantity;
        if (l.quantity <= 0) l.status = MarketListing.Status.SOLD;

        store.put(l);
        store.flush();

        buyer.sendMessage(ChatColor.GREEN + "Compra realizada.");
        buyer.playSound(buyer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
    }

    private void cancelListing(Player p, String listingId) {
        MarketListing l = store.get(listingId).orElse(null);
        if (l == null || l.status != MarketListing.Status.ACTIVE) {
            p.sendMessage(ChatColor.RED + "Listado no disponible.");
            return;
        }
        if (l.sellerId == null || !l.sellerId.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "No eres el vendedor.");
            return;
        }

        // return item to seller
        ItemStack give = l.item == null ? null : l.item.clone();
        if (give != null) {
            give.setAmount(Math.max(1, l.quantity));
            HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(give);
            leftover.values().forEach(it -> p.getWorld().dropItemNaturally(p.getLocation(), it));
        }

        l.status = MarketListing.Status.CANCELLED;
        store.put(l);
        store.flush();

        p.sendMessage(ChatColor.YELLOW + "Listado cancelado.");
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private int parsePage(String title) {
        try {
            int idx = title.lastIndexOf('#');
            if (idx < 0) return 1;
            return Integer.parseInt(title.substring(idx + 1).trim());
        } catch (Exception ex) {
            return 1;
        }
    }

    private String extractListingId(ItemStack it) {
        ItemMeta im = it.getItemMeta();
        if (im == null || im.getLore() == null) return null;
        for (String line : im.getLore()) {
            String s = ChatColor.stripColor(line);
            if (s != null && s.startsWith("ID:")) return s.substring(3).trim();
        }
        return null;
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

    private boolean removeExact(PlayerInventory inv, ItemStack template, int amount) {
        if (amount <= 0) return true;
        int toRemove = amount;

        // First pass count
        int count = 0;
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType() == Material.AIR) continue;
            if (!it.isSimilar(template)) continue;
            count += it.getAmount();
        }
        if (count < amount) return false;

        // Second pass remove
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            if (!it.isSimilar(template)) continue;

            int take = Math.min(it.getAmount(), toRemove);
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) inv.setItem(i, null);
            toRemove -= take;
            if (toRemove <= 0) break;
        }
        return true;
    }

    private static final class Draft {
        ItemStack item;
        long priceWcoin;
        long priceWcraft;
        Currency lastCurrency = Currency.WCOIN;
    }
}
