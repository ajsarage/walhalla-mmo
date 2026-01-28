package walhalla.mmo.market;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class WalhallaMarketAdminCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MarketStore store;

    public WalhallaMarketAdminCommand(JavaPlugin plugin, MarketStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("walhalla.admin")) {
            sender.sendMessage(ChatColor.RED + "Sin permiso.");
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "help";
        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                store.load();
                sender.sendMessage(ChatColor.GREEN + "WalhallaMarket recargado (config + listings).");
                return true;
            }
            case "inspect" -> {
                int active = 0;
                for (MarketListing l : store.all()) if (l.status == MarketListing.Status.ACTIVE) active++;
                sender.sendMessage(ChatColor.YELLOW + "Listados totales: " + store.all().size() + " | activos: " + active);
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.GRAY + "/walhalla-market reload");
                sender.sendMessage(ChatColor.GRAY + "/walhalla-market inspect");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("walhalla.admin")) return out;
        if (args.length == 1) {
            out.add("reload");
            out.add("inspect");
        }
        return out;
    }
}
