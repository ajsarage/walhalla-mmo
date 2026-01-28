package walhalla.mmo.combat;

import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import walhalla.mmo.core.api.combat.DamageType;

/**
 * Lightweight combat feedback:
 * - actionbar damage dealt
 * - optional status messages
 */
public class CombatFeedback {

    private final WalhallaCombatPlugin plugin;

    public CombatFeedback(WalhallaCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendHitFeedback(Player attacker, LivingEntity target, double dmg, double hp, double maxHp, DamageType type) {
        if (attacker == null) return;
        String tName = target.getName();
        int pct = maxHp <= 0 ? 0 : (int)Math.round((hp / maxHp) * 100.0);
        String typeLabel = type.name().toLowerCase(Locale.ROOT);

        Component msg = Component.text("⟡ ", NamedTextColor.GRAY)
                .append(Component.text(String.format(Locale.ROOT, "%.1f", dmg), NamedTextColor.GOLD))
                .append(Component.text(" dmg ", NamedTextColor.GRAY))
                .append(Component.text(typeLabel, NamedTextColor.AQUA))
                .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                .append(Component.text(tName, NamedTextColor.WHITE))
                .append(Component.text(" (" + pct + "%)", NamedTextColor.GREEN));

        attacker.sendActionBar(msg);
    }

    public void sendStatusFeedback(Player attacker, LivingEntity target, String statusId) {
        if (attacker == null || statusId == null) return;
        attacker.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "Status" + ChatColor.DARK_GRAY + "] "
                + ChatColor.GRAY + target.getName() + " ← " + ChatColor.YELLOW + statusId.toUpperCase(Locale.ROOT));
    }
}
