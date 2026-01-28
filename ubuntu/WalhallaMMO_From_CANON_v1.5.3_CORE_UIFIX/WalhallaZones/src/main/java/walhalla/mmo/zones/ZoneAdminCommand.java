package walhalla.mmo.zones;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.zones.ZoneRulesView;
import walhalla.mmo.zones.service.ZoneManager;

/** OP-only commands for Zones (reload/inspect). */
public class ZoneAdminCommand implements CommandExecutor {

    private final ZoneManager zones;

    public ZoneAdminCommand(ZoneManager zones) {
        this.zones = zones;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("walhalla.admin.zones")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /whzones <reload|where>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                zones.loadAll();
                sender.sendMessage("§aWalhallaZones reloaded.");
                return true;
            }
            case "where" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("§cOnly players.");
                    return true;
                }
                String zid = CoreAPI.getCurrentZoneId(p.getUniqueId()).orElse("(unknown)");
                ZoneRulesView rules = CoreAPI.getZoneRules(zid);
                sender.sendMessage("§bZone: §f" + zid + " §7(" + rules.zoneType() + ")");
                sender.sendMessage("§7PVP=" + rules.pvpEnabled() + " Combat=" + rules.combatAllowed() + " DeathPenalty=" + rules.deathPenalty());
                return true;
            }
            default -> {
                sender.sendMessage("§eUsage: /whzones <reload|where>");
                return true;
            }
        }
    }
}
