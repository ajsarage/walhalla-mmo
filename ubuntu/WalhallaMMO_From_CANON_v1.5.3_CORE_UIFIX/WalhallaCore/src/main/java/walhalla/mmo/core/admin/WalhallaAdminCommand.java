package walhalla.mmo.core.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import walhalla.mmo.core.onboarding.BranchRegistry;
import walhalla.mmo.core.progress.PlayerData;
import walhalla.mmo.core.progress.PlayerProgressService;
import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.canon.CanonDataService;
import walhalla.mmo.core.canon.CanonFile;

/**
 * OP/Admin commands. Players should never need commands.
 * Permission: walhalla.admin (default OP).
 *
 * Usage:
 *  /walhalla state <player> [UNINITIALIZED|CHOOSING_BRANCHES|ACTIVE]
 *  /walhalla branches <player> list
 *  /walhalla branches <player> set <ID,ID,ID,ID>
 *  /walhalla reset <player>
 *
 *  /walhalla xp <player> show
 *  /walhalla xp <player> add global <amount>
 *  /walhalla xp <player> add branch <branchId> <amount>
 *  /walhalla xp <player> add affinity <affinityId> <amount>
 *  /walhalla xp <player> add profession <professionId> <amount>
 *  /walhalla spells reload
        s.sendMessage(ChatColor.YELLOW + "/walhalla combat status");
        s.sendMessage(ChatColor.YELLOW + "/walhalla combat reload");
        s.sendMessage(ChatColor.YELLOW + "/walhalla combat kit <player> set <KIT_ID>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla combat kit <player> show");
 *
 *  /walhalla canon status
 *  /walhalla canon reload
 */
public class WalhallaAdminCommand implements CommandExecutor, TabCompleter {

    private final PlayerProgressService progress;

    public WalhallaAdminCommand(PlayerProgressService progress) {
        this.progress = progress;
    }

    private boolean noPerm(CommandSender s) {
        if (s.isOp() || s.hasPermission("walhalla.admin")) return false;
        s.sendMessage(ChatColor.RED + "No permission.");
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (noPerm(sender)) return true;

        if (args.length < 1) {
            help(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "state" -> handleState(sender, args);
            case "branches" -> handleBranches(sender, args);
            case "reset" -> handleReset(sender, args);
            case "xp" -> handleXp(sender, args);
            case "money" -> handleMoney(sender, args);
            case "spells" -> handleSpells(sender, args);
            case "mage" -> handleMage(sender, args);
            case "combat" -> handleCombat(sender, args);
            case "canon" -> handleCanons(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "Walhalla Admin");
        s.sendMessage(ChatColor.YELLOW + "/walhalla state <player> [UNINITIALIZED|CHOOSING_BRANCHES|ACTIVE]");
        s.sendMessage(ChatColor.YELLOW + "/walhalla branches <player> list");
        s.sendMessage(ChatColor.YELLOW + "/walhalla branches <player> set <ID,ID,ID,ID>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla reset <player>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla xp <player> show");
        s.sendMessage(ChatColor.YELLOW + "/walhalla xp <player> add global <amount>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla xp <player> add branch <branchId> <amount>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla xp <player> add affinity <affinityId> <amount>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla xp <player> add profession <professionId> <amount>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla spells reload");
        s.sendMessage(ChatColor.YELLOW + "/walhalla mage reload");
        s.sendMessage(ChatColor.YELLOW + "/walhalla mage set <player> <SET_ID>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla mage show <player>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla combat status");
        s.sendMessage(ChatColor.YELLOW + "/walhalla combat reload");
        s.sendMessage(ChatColor.YELLOW + "/walhalla combat kit <player> set <KIT_ID>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla combat kit <player> show");
        s.sendMessage(ChatColor.YELLOW + "/walhalla money <player> show");
        s.sendMessage(ChatColor.YELLOW + "/walhalla money <player> add <amount>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla money <player> set <amount>");
        s.sendMessage(ChatColor.YELLOW + "/walhalla canon status");
        s.sendMessage(ChatColor.YELLOW + "/walhalla canon reload");
    }


    private void handleCombat(CommandSender s, String[] args) {
        // Canon: combat admin tooling is optional by phase; do not invent behaviors here.
        if (args.length < 2) {
            s.sendMessage(ChatColor.YELLOW + "Usage:");
            s.sendMessage(ChatColor.YELLOW + " - /walhalla combat status");
            s.sendMessage(ChatColor.YELLOW + " - /walhalla combat reload");
            s.sendMessage(ChatColor.YELLOW + " - /walhalla combat kit <player> set <KIT_ID>");
            s.sendMessage(ChatColor.YELLOW + " - /walhalla combat kit <player> show");
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        org.bukkit.plugin.Plugin pl = Bukkit.getPluginManager().getPlugin("WalhallaCombat");
        boolean enabled = (pl != null && pl.isEnabled());

        switch (sub) {
            case "status" -> {
                if (!enabled) {
                    s.sendMessage(ChatColor.RED + "WalhallaCombat not enabled.");
                } else {
                    s.sendMessage(ChatColor.GREEN + "WalhallaCombat enabled: " + pl.getDescription().getVersion());
                }
                s.sendMessage(ChatColor.GRAY + "(Combat admin actions are phase-gated; this core build does not execute combat mutations.)");
            }
            case "reload", "kit" -> {
                s.sendMessage(ChatColor.YELLOW + "NOT_IMPLEMENTED in this core phase. Combat admin actions are provided by WalhallaCombat when available.");
            }
            default -> {
                s.sendMessage(ChatColor.RED + "Unknown combat subcommand: " + sub);
                s.sendMessage(ChatColor.YELLOW + "Try: /walhalla combat status");
            }
        }
    }

    private void handleCanons(CommandSender s, String[] args) {
        if (args.length < 2) {
            s.sendMessage(ChatColor.YELLOW + "Usage:");
            s.sendMessage(ChatColor.YELLOW + " - /walhalla canon status");
            s.sendMessage(ChatColor.YELLOW + " - /walhalla canon reload");
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "status" -> {
                CoreAPI.getCanonService().ifPresentOrElse(c -> {
                    s.sendMessage(ChatColor.GREEN + "Canon loaded.");
                    s.sendMessage(ChatColor.GRAY + "Dir: " + c.canonDir().getPath());
                    s.sendMessage(ChatColor.GRAY + "Files loaded: " + c.loadedFileCount());
                    s.sendMessage(ChatColor.GRAY + "SHA256 entries: " + c.sha256Map().size());
                }, () -> {
                    s.sendMessage(ChatColor.RED + "Canon service not available (Core not initialized).");
                });
            }
            case "reload" -> {
                CoreAPI.getCanonService().ifPresentOrElse(c -> {
                    try {
                        c.reloadOrFail();
                        s.sendMessage(ChatColor.GREEN + "OK. Canon reloaded.");
                    } catch (Exception ex) {
                        s.sendMessage(ChatColor.RED + "Canon reload failed: " + ex.getMessage());
                    }
                }, () -> s.sendMessage(ChatColor.RED + "Canon service not available (Core not initialized)."));
            }
            default -> {
                s.sendMessage(ChatColor.RED + "Unknown canon subcommand: " + sub);
                s.sendMessage(ChatColor.YELLOW + "Try: /walhalla canon status");
            }
        }
    }

private Player requireOnlinePlayer(CommandSender s, String playerName) {
        Player p = Bukkit.getPlayerExact(playerName);
        if (p == null) {
            s.sendMessage(ChatColor.RED + "Player not online: " + playerName);
            return null;
        }
        return p;
    }

    private PlayerData requirePlayerData(CommandSender s, String playerName) {
        Player p = requireOnlinePlayer(s, playerName);
        if (p == null) return null;
        return progress.getOrLoad(p.getUniqueId());
    }

    private void handleState(CommandSender s, String[] args) {
        if (args.length < 2) { help(s); return; }
        PlayerData d = requirePlayerData(s, args[1]);
        if (d == null) return;

        if (args.length == 2) {
            s.sendMessage(ChatColor.GRAY + "State: " + ChatColor.AQUA + d.getLifecycleState().name());
            return;
        }

        try {
            PlayerData.PlayerLifecycleState st = PlayerData.PlayerLifecycleState.valueOf(args[2].toUpperCase());
            d.setLifecycleState(st);
            progress.saveNow(d);
            s.sendMessage(ChatColor.GREEN + "OK. State set to " + st.name());
        } catch (IllegalArgumentException e) {
            s.sendMessage(ChatColor.RED + "Invalid state.");
        }
    }

    private void handleBranches(CommandSender s, String[] args) {
        if (args.length < 3) { help(s); return; }
        PlayerData d = requirePlayerData(s, args[1]);
        if (d == null) return;

        if ("list".equalsIgnoreCase(args[2])) {
            s.sendMessage(ChatColor.GRAY + "Chosen (" + d.getChosenBranches().size() + "): " + ChatColor.AQUA + d.getChosenBranches());
            return;
        }

        if ("set".equalsIgnoreCase(args[2])) {
            if (args.length < 4) {
                s.sendMessage(ChatColor.RED + "Missing list. Example: GUERRERO,MAGO,MINERO,HERRERO");
                return;
            }
            String[] parts = args[3].split(",");
            List<String> set = new ArrayList<>();
            for (String p : parts) {
                String id = p.trim();
                if (!BranchRegistry.isValidBranchId(id)) {
                    s.sendMessage(ChatColor.RED + "Invalid branch ID: " + id);
                    return;
                }
                set.add(id);
            }
            d.setChosenBranches(set);
            progress.saveNow(d);
            s.sendMessage(ChatColor.GREEN + "OK. branches=" + d.getChosenBranches());
            return;
        }

        help(s);
    }

    private void handleReset(CommandSender s, String[] args) {
        if (args.length < 2) { help(s); return; }
        PlayerData d = requirePlayerData(s, args[1]);
        if (d == null) return;

        d.setLifecycleState(PlayerData.PlayerLifecycleState.CHOOSING_BRANCHES);
        d.setChosenBranches(List.of());
        progress.saveNow(d);
        s.sendMessage(ChatColor.GREEN + "OK. Reset onboarding.");
    }

    private void handleXp(CommandSender s, String[] args) {
        if (args.length < 3) { help(s); return; }
        Player p = requireOnlinePlayer(s, args[1]);
        if (p == null) return;

        PlayerData d = progress.getOrLoad(p.getUniqueId());

        String sub = args[2].toLowerCase(Locale.ROOT);
        if ("show".equals(sub)) {
            s.sendMessage(ChatColor.GRAY + "ACTIVE=" + ChatColor.AQUA + d.isActive());
            s.sendMessage(ChatColor.GRAY + "GlobalXP=" + ChatColor.AQUA + d.getGlobalXpTotal());
            s.sendMessage(ChatColor.GRAY + "RPGPoints=" + ChatColor.AQUA + d.getRpgPointsAvailable() + ChatColor.GRAY +
                    " (earned=" + d.getRpgPointsEarned() + ", spent=" + d.getRpgPointsSpent() + ")");
            s.sendMessage(ChatColor.GRAY + "BranchXP keys=" + ChatColor.AQUA + d.getBranchXpTotal().keySet());
            s.sendMessage(ChatColor.GRAY + "AffinityXP keys=" + ChatColor.AQUA + d.getAffinityXpTotal().keySet());
            s.sendMessage(ChatColor.GRAY + "ProfessionXP keys=" + ChatColor.AQUA + d.getProfessionXpTotal().keySet());
            return;
        }

        if ("add".equals(sub)) {
            if (args.length < 5) { help(s); return; }
            String type = args[3].toLowerCase(Locale.ROOT);
            try {
                if ("global".equals(type)) {
                    long amt = Long.parseLong(args[4]);
                    boolean ok = progress.addGlobalXp(p.getUniqueId(), amt);
                    s.sendMessage(ok ? ChatColor.GREEN + "OK." : ChatColor.RED + "Rejected (player not ACTIVE or amount invalid).");
                    return;
                }
                if ("branch".equals(type)) {
                    if (args.length < 6) { help(s); return; }
                    String id = args[4];
                    long amt = Long.parseLong(args[5]);
                    boolean ok = progress.addBranchXp(p.getUniqueId(), id, amt);
                    s.sendMessage(ok ? ChatColor.GREEN + "OK." : ChatColor.RED + "Rejected (player not ACTIVE or amount invalid).");
                    return;
                }
                if ("affinity".equals(type)) {
                    if (args.length < 6) { help(s); return; }
                    String id = args[4];
                    long amt = Long.parseLong(args[5]);
                    boolean ok = progress.addAffinityXp(p.getUniqueId(), id, amt);
                    s.sendMessage(ok ? ChatColor.GREEN + "OK." : ChatColor.RED + "Rejected (player not ACTIVE or amount invalid).");
                    return;
                }
                if ("profession".equals(type)) {
                    if (args.length < 6) { help(s); return; }
                    String id = args[4];
                    long amt = Long.parseLong(args[5]);
                    boolean ok = progress.addProfessionXp(p.getUniqueId(), id, amt);
                    s.sendMessage(ok ? ChatColor.GREEN + "OK." : ChatColor.RED + "Rejected (player not ACTIVE or amount invalid).");
                    return;
                }

                s.sendMessage(ChatColor.RED + "Unknown xp type: " + type);
                return;

            } catch (NumberFormatException nfe) {
                s.sendMessage(ChatColor.RED + "Invalid amount.");
                return;
            }
        }

        help(s);
    }

private void handleSpells(CommandSender s, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("reload")) {
            s.sendMessage(ChatColor.YELLOW + "Usage: /walhalla spells reload");
            return;
        }
        org.bukkit.plugin.Plugin pl = Bukkit.getPluginManager().getPlugin("WalhallaSpells");
        if (pl == null) {
            s.sendMessage(ChatColor.RED + "WalhallaSpells not enabled.");
            return;
        }
        try {
            Object catalog = pl.getClass().getMethod("getCatalog").invoke(pl);
            if (catalog != null) {
                catalog.getClass().getMethod("reload").invoke(catalog);
                s.sendMessage(ChatColor.GREEN + "OK. Spells reloaded.");
            } else {
                s.sendMessage(ChatColor.RED + "Catalog not available.");
            }
        } catch (Exception ex) {
            s.sendMessage(ChatColor.RED + "Failed to reload spells: " + ex.getClass().getSimpleName());
        }
    }

private void handleMage(CommandSender s, String[] args) {
        org.bukkit.plugin.Plugin pl = Bukkit.getPluginManager().getPlugin("WalhallaSpells");
        if (pl == null) {
            s.sendMessage(ChatColor.RED + "WalhallaSpells not enabled.");
            return;
        }
        if (args.length < 2) {
            s.sendMessage(ChatColor.YELLOW + "Usage:");
            s.sendMessage(ChatColor.YELLOW + " - /walhalla mage reload");
            s.sendMessage(ChatColor.YELLOW + " - /walhalla mage set <player> <SET_ID>");
            s.sendMessage(ChatColor.YELLOW + " - /walhalla mage show <player>");
            return;
        }

        String sub = args[1].toLowerCase();
        try {
            switch (sub) {
                case "reload" -> {
                    pl.getClass().getMethod("reloadMageSets").invoke(pl);
                    s.sendMessage(ChatColor.GREEN + "OK. Mage sets reloaded.");
                }
                case "show" -> {
                    if (args.length < 3) { s.sendMessage(ChatColor.YELLOW + "Usage: /walhalla mage show <player>"); return; }
                    Player p = requireOnlinePlayer(s, args[2]);
                    if (p == null) return;
                    Object opt = pl.getClass().getMethod("getMageSet", java.util.UUID.class).invoke(pl, p.getUniqueId());
                    String val = String.valueOf(opt);
                    s.sendMessage(ChatColor.AQUA + "MageSet(" + p.getName() + "): " + val);
                }
                case "set" -> {
                    if (args.length < 4) { s.sendMessage(ChatColor.YELLOW + "Usage: /walhalla mage set <player> <SET_ID>"); return; }
                    Player p = requireOnlinePlayer(s, args[2]);
                    if (p == null) return;
                    boolean ok = (boolean) pl.getClass().getMethod("setMageSet", java.util.UUID.class, String.class).invoke(pl, p.getUniqueId(), args[3]);
                    if (ok) s.sendMessage(ChatColor.GREEN + "OK. Mage set applied.");
                    else s.sendMessage(ChatColor.RED + "Invalid SET_ID (or mage sets not loaded).");
                }
                default -> s.sendMessage(ChatColor.RED + "Unknown subcommand: " + sub);
            }
        } catch (Exception ex) {
            s.sendMessage(ChatColor.RED + "Mage command failed: " + ex.getClass().getSimpleName());
        }
    }

    private void handleMoney(CommandSender s, String[] args) {
        if (args.length < 3) { help(s); return; }
        Player p = requireOnlinePlayer(s, args[1]);
        if (p == null) return;

        var eco = Bukkit.getServicesManager().load(walhalla.mmo.core.api.economy.EconomyBridge.class);
        if (eco == null) {
            s.sendMessage(ChatColor.RED + "Economy service not available.");
            return;
        }

        String sub = args[2].toLowerCase(Locale.ROOT);

        walhalla.mmo.core.api.economy.Currency currency = walhalla.mmo.core.api.economy.Currency.WCOIN;
        int amtIndex = 3;

        // Optional currency token
        if (args.length >= 5) {
            String curTok = args[3].toLowerCase(Locale.ROOT);
            if ("wcraft".equals(curTok) || "wc".equals(curTok)) currency = walhalla.mmo.core.api.economy.Currency.WCRAFT;
            else currency = walhalla.mmo.core.api.economy.Currency.WCOIN;
            amtIndex = 4;
        }

        if ("show".equals(sub)) {
            long wcoin = eco.getBalance(p.getUniqueId(), walhalla.mmo.core.api.economy.Currency.WCOIN);
            long wcraft = eco.getBalance(p.getUniqueId(), walhalla.mmo.core.api.economy.Currency.WCRAFT);
            s.sendMessage(ChatColor.GRAY + "WCoin=" + ChatColor.AQUA + wcoin + ChatColor.GRAY + " | WCraft=" + ChatColor.LIGHT_PURPLE + wcraft);
            return;
        }

        if (args.length <= amtIndex) { help(s); return; }

        try {
            long amt = Long.parseLong(args[amtIndex]);
            if (amt < 0) { s.sendMessage(ChatColor.RED + "Amount must be >= 0"); return; }

            if ("add".equals(sub)) {
                eco.deposit(p.getUniqueId(), currency, amt, "ADMIN_ADD", "ADMIN_ADD:" + System.currentTimeMillis());
                s.sendMessage(ChatColor.GREEN + "OK. " + currency + "=" + eco.getBalance(p.getUniqueId(), currency));
                return;
            }
            if ("set".equals(sub)) {
                long cur = eco.getBalance(p.getUniqueId(), currency);
                if (amt > cur) eco.deposit(p.getUniqueId(), currency, amt - cur, "ADMIN_SET", "ADMIN_SET:" + System.currentTimeMillis());
                else if (amt < cur) eco.withdraw(p.getUniqueId(), currency, cur - amt, "ADMIN_SET", "ADMIN_SET:" + System.currentTimeMillis());
                s.sendMessage(ChatColor.GREEN + "OK. " + currency + "=" + eco.getBalance(p.getUniqueId(), currency));
                return;
            }
        } catch (NumberFormatException ex) {
            s.sendMessage(ChatColor.RED + "Invalid amount.");
            return;
        }

        help(s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (noPerm(sender)) return List.of();
        if (args.length == 1) return List.of("state", "branches", "reset", "xp", "spells", "money");
        if (args.length == 2 && (args[0].equalsIgnoreCase("state") || args[0].equalsIgnoreCase("branches") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("xp"))) {
            return null; // let Bukkit provide player names
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("state")) return List.of("UNINITIALIZED","CHOOSING_BRANCHES","ACTIVE");
        if (args.length == 3 && args[0].equalsIgnoreCase("branches")) return List.of("list","set");
        if (args.length == 3 && args[0].equalsIgnoreCase("xp")) return List.of("show","add");
        if (args.length == 3 && args[0].equalsIgnoreCase("money")) return List.of("show","add","set");
        if (args.length == 4 && args[0].equalsIgnoreCase("xp") && args[2].equalsIgnoreCase("add")) {
            return List.of("global","branch","affinity","profession");
        }
        return List.of();
    }
}
