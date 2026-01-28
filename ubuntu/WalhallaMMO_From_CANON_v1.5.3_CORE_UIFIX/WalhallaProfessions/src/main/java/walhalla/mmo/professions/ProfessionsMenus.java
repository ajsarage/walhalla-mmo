package walhalla.mmo.professions;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.economy.EconomyBridge;
import walhalla.mmo.core.api.professions.ProfessionsUiBridge;
import walhalla.mmo.core.api.progress.ProfessionProgressView;
import walhalla.mmo.core.api.progress.ProgressMutationBridge;

public final class ProfessionsMenus implements Listener, ProfessionsUiBridge {

    private final JavaPlugin plugin;
    private final GatheringRegistry gathering;
    private final RecipeRegistry recipes;
    private final ProgressMutationBridge progress;
    private final ItemFactory items;

    public ProfessionsMenus(JavaPlugin plugin, GatheringRegistry gathering, RecipeRegistry recipes, ProgressMutationBridge progress) {
        this.plugin = plugin;
        this.gathering = gathering;
        this.recipes = recipes;
        this.progress = progress;
        this.items = new ItemFactory(plugin);
    }

    @Override
    public void openProfessionsMenu(Player player) {
        if (player == null) return;
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuHolder.Type.PROFESSIONS), 54, ChatColor.DARK_AQUA + "Profesiones");

        inv.setItem(10, button(Material.CRAFTING_TABLE, "Crafteo", List.of("Abre el catálogo de recetas")));
        inv.setItem(12, button(Material.EMERALD, "Vender", List.of("Vende recursos por WCoin (NPC)")));

        EconomyBridge eco = Bukkit.getServicesManager().load(EconomyBridge.class);
        long bal = eco == null ? 0L : eco.getBalance(player.getUniqueId());
        inv.setItem(14, button(Material.GOLD_NUGGET, "Monedero", List.of("WCoin: " + bal)));

        List<ProfessionProgressView> profs = CoreAPI.getAllProfessionProgress(player.getUniqueId());
        profs.sort(Comparator.comparing(ProfessionProgressView::professionId));

        int slot = 27;
        for (ProfessionProgressView p : profs) {
            if (slot >= inv.getSize()) break;
            inv.setItem(slot++, button(Material.BOOK, p.professionId(), List.of(
                    "Nivel: " + p.level(),
                    "XP: " + p.totalXp(),
                    "Siguiente: " + (p.xpForNextLevel() - p.xpIntoLevel())
            )));
        }

        player.openInventory(inv);
    }

    @Override
    public void openCraftingMenu(Player player) {
        if (player == null) return;
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuHolder.Type.CRAFTING), 54, ChatColor.DARK_GREEN + "Crafteo");

        int slot = 0;
        for (CraftRecipe r : recipes.all()) {
            if (slot >= inv.getSize()) break;
            List<String> lore = new ArrayList<>();
            if (r.requiredProfessionId() != null && !r.requiredProfessionId().isBlank()) {
                lore.add("Requiere: " + r.requiredProfessionId() + " L" + Math.max(0, r.requiredProfessionLevel()));
            }
            if (r.moneyCost() > 0) lore.add("Coste: " + r.moneyCost() + " WCoin");
            lore.add(" ");
            lore.add("Ingredientes:");
            for (Map.Entry<String, Integer> e : r.ingredients().entrySet()) {
                lore.add("- " + e.getKey() + " x" + e.getValue());
            }
            lore.add(" ");
            lore.add("Click para fabricar");

            ItemStack icon = button(r.displayMaterial(), r.displayName(), lore);
            // tag with recipe id in display name line is enough for now
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot++, icon);
        }

        inv.setItem(53, button(Material.ARROW, "Volver", List.of("Regresa a Profesiones")));
        player.openInventory(inv);
    }

    private void openSellMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuHolder.Type.SELL), 27, ChatColor.GOLD + "Vender");
        inv.setItem(11, button(Material.EMERALD_BLOCK, "Vender todo", List.of("Vende todos los ítems con precio NPC", "(según ANNEX_ECONOMY_PRICES_BASE_v1)")));
        inv.setItem(15, button(Material.ARROW, "Volver", List.of("Regresa a Profesiones")));
        player.openInventory(inv);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getInventory().getHolder() instanceof MenuHolder h)) return;

        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0) return;

        switch (h.type()) {
            case PROFESSIONS -> handleProfessionsClick(p, slot);
            case CRAFTING -> handleCraftingClick(p, slot, e.getCurrentItem());
            case SELL -> handleSellClick(p, slot);
        }
    }

    private void handleProfessionsClick(Player p, int slot) {
        if (slot == 10) {
            openCraftingMenu(p);
            return;
        }
        if (slot == 12) {
            openSellMenu(p);
            return;
        }
        // wallet is display-only
    }

    private void handleCraftingClick(Player p, int slot, ItemStack clicked) {
        if (slot == 53) {
            openProfessionsMenu(p);
            return;
        }
        if (clicked == null) return;
        String title = ChatColor.stripColor(clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "");
        if (title == null || title.isBlank()) return;

        CraftRecipe target = null;
        for (CraftRecipe r : recipes.all()) {
            if (r.displayName().equalsIgnoreCase(title)) { target = r; break; }
        }
        if (target == null) return;

        // Profession gating
        if (target.requiredProfessionId() != null && !target.requiredProfessionId().isBlank()) {
            var viewOpt = CoreAPI.getProfessionProgress(p.getUniqueId(), target.requiredProfessionId());
            int lvl = viewOpt.map(ProfessionProgressView::level).orElse(1);
            if (lvl < Math.max(0, target.requiredProfessionLevel())) {
                p.sendMessage(ChatColor.RED + "Nivel insuficiente: " + target.requiredProfessionId() + " (L" + lvl + ")");
                return;
            }
        }

        // Money + fees (Phase 12: atomicity)
        EconomyBridge eco = Bukkit.getServicesManager().load(EconomyBridge.class);
        if (eco == null) {
            p.sendMessage(ChatColor.RED + "Economía no disponible.");
            return;
        }

        // First ensure ingredients exist (so we never charge if the action can't execute)
        if (!hasIngredients(p, target.ingredients())) {
            p.sendMessage(ChatColor.RED + "Faltan ingredientes.");
            return;
        }

        // Station use fee derived from canon sinks (based on output tier)
        int outTier = parseTierFromId(target.outputItemId());
        var stationFee = CoreAPI.quoteStationUseFee(outTier);

        // Total cost (recipe cost is WCoin per canon recipes; station fee can be WCoin or WCraft at T7+)
        String txBase = "CRAFT:" + target.recipeId() + ":" + System.currentTimeMillis();
        if (target.moneyCost() > 0) {
            if (!eco.withdraw(p.getUniqueId(), walhalla.mmo.core.api.economy.Currency.WCOIN, target.moneyCost(), "CRAFT_COST:" + target.recipeId(), txBase + ":COST")) {
                p.sendMessage(ChatColor.RED + "No tienes WCoin suficiente.");
                return;
            }
        }
        if (stationFee.amount() > 0) {
            if (!eco.withdraw(p.getUniqueId(), stationFee.currency(), stationFee.amount(), "STATION_FEE:" + target.recipeId(), txBase + ":FEE")) {
                // refund recipe cost if fee fails
                if (target.moneyCost() > 0) {
                    eco.deposit(p.getUniqueId(), walhalla.mmo.core.api.economy.Currency.WCOIN, target.moneyCost(), "REFUND_CRAFT_COST:" + target.recipeId(), txBase + ":REFUND_COST");
                }
                p.sendMessage(ChatColor.RED + "No puedes pagar la tarifa de estación (" + stationFee.amount() + " " + stationFee.currency() + ").");
                return;
            }
        }

        // Consume ingredients (best-effort atomicity; if it fails, refund)
        if (!consumeIngredientsSafe(p, target.ingredients())) {
            if (target.moneyCost() > 0) eco.deposit(p.getUniqueId(), walhalla.mmo.core.api.economy.Currency.WCOIN, target.moneyCost(), "REFUND_CRAFT_COST:" + target.recipeId(), txBase + ":RB_COST");
            if (stationFee.amount() > 0) eco.deposit(p.getUniqueId(), stationFee.currency(), stationFee.amount(), "REFUND_STATION_FEE:" + target.recipeId(), txBase + ":RB_FEE");
            p.sendMessage(ChatColor.RED + "No se pudo consumir ingredientes (inventario cambió).");
            return;
        }

        if (!hasIngredients(p, target.ingredients())) {
            p.sendMessage(ChatColor.RED + "Faltan ingredientes.");
            return;
        }
        

        ItemStack out = items.createResource(target.outputItemId(), target.outputName(), target.outputMaterial(), target.outputAmount());
        p.getInventory().addItem(out);
        p.sendMessage(ChatColor.GREEN + "Fabricado: " + target.outputName());

        if (progress != null && target.craftXp() > 0 && target.requiredProfessionId() != null && !target.requiredProfessionId().isBlank()) {
            progress.addProfessionXp(p.getUniqueId(), target.requiredProfessionId(), target.craftXp());
        }
    }

    
    private int parseTierFromId(String itemId) {
        if (itemId == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("_T(\\d+)$").matcher(itemId);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return 0;
    }

    private int parseQualityFromItem(ItemStack it) {
        if (it == null || it.getItemMeta() == null) return 1;
        var meta = it.getItemMeta();
        var key = new org.bukkit.NamespacedKey(plugin, "wh_quality");
        String q = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
        if (q == null) return 1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Q(\\d+)").matcher(q.toUpperCase(Locale.ROOT));
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return 1;
    }

    private boolean consumeIngredientsSafe(Player p, Map<String, Integer> req) {
        if (p == null) return false;
        if (req == null || req.isEmpty()) return true;
        if (!hasIngredients(p, req)) return false;

        // perform consumption
        consumeIngredients(p, req);

        // verify nothing is still missing (defensive)
        return !hasIngredients(p, req);
    }

private boolean hasIngredients(Player p, Map<String, Integer> req) {
        Map<String, Integer> need = new HashMap<>();
        for (var e : req.entrySet()) need.put(e.getKey(), Math.max(1, e.getValue()));

        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null) continue;
            String id = ItemFactory.getItemId(plugin, it);
            if (id == null) continue;
            if (!need.containsKey(id)) continue;
            int left = need.get(id);
            int take = Math.min(left, it.getAmount());
            left -= take;
            if (left <= 0) need.remove(id);
            else need.put(id, left);
            if (need.isEmpty()) return true;
        }
        return need.isEmpty();
    }

    private void consumeIngredients(Player p, Map<String, Integer> req) {
        Map<String, Integer> need = new HashMap<>();
        for (var e : req.entrySet()) need.put(e.getKey(), Math.max(1, e.getValue()));

        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            String id = ItemFactory.getItemId(plugin, it);
            if (id == null) continue;
            if (!need.containsKey(id)) continue;

            int left = need.get(id);
            int take = Math.min(left, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) contents[i] = null;

            left -= take;
            if (left <= 0) need.remove(id);
            else need.put(id, left);

            if (need.isEmpty()) break;
        }
        p.getInventory().setContents(contents);
    }

    private void handleSellClick(Player p, int slot) {
        if (slot == 15) {
            openProfessionsMenu(p);
            return;
        }
        if (slot != 11) return;

        EconomyBridge eco = Bukkit.getServicesManager().load(EconomyBridge.class);
        if (eco == null) {
            p.sendMessage(ChatColor.RED + "Economía no disponible.");
            return;
        }

        long totalWCoin = 0L;
        long totalWCraft = 0L;


        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            String id = ItemFactory.getItemId(plugin, it);
            if (id == null) continue;

            int tier = parseTierFromId(id);
            int q = parseQualityFromItem(it);
            var quote = CoreAPI.quoteNpcSell(id, tier, q);
            if (quote.amount() <= 0) continue;
            long add = quote.amount() * (long) it.getAmount();
            if (quote.currency() == walhalla.mmo.core.api.economy.Currency.WCRAFT) totalWCraft = safeAdd(totalWCraft, add);
            else totalWCoin = safeAdd(totalWCoin, add);
            contents[i] = null;
            contents[i] = null;
        }
        p.getInventory().setContents(contents);

        if (totalWCoin > 0) {
            eco.deposit(p.getUniqueId(), walhalla.mmo.core.api.economy.Currency.WCOIN, totalWCoin, "NPC_SELL_ALL", "SELL_ALL:" + System.currentTimeMillis());
        }
        if (totalWCraft > 0) {
            eco.deposit(p.getUniqueId(), walhalla.mmo.core.api.economy.Currency.WCRAFT, totalWCraft, "NPC_SELL_ALL", "SELL_ALL:" + System.currentTimeMillis() + ":WC");
        }
        if (totalWCoin > 0 || totalWCraft > 0) {
            p.sendMessage(ChatColor.GREEN + "Vendido por: " + totalWCoin + " WCoin" + (totalWCraft > 0 ? (" + " + totalWCraft + " WCraft") : ""));
        } else {
            p.sendMessage(ChatColor.GRAY + "No tienes ítems vendibles.");
        }

        // refresh
        openProfessionsMenu(p);
    }

    private long safeAdd(long a, long b) {
        try { return Math.addExact(a, b); } catch (ArithmeticException ex) { return Long.MAX_VALUE; }
    }

    private ItemStack button(Material mat, String name, List<String> lorePlain) {
        ItemStack it = new ItemStack(mat == null ? Material.PAPER : mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + name);
            List<String> lore = new ArrayList<>();
            for (String s : lorePlain) lore.add(ChatColor.GRAY + s);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // no-op placeholder
    }
}