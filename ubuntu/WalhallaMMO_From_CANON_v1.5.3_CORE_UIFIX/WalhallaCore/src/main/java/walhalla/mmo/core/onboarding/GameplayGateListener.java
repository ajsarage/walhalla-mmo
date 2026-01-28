package walhalla.mmo.core.onboarding;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import walhalla.mmo.core.progress.PlayerProgressService;

/**
 * Enforces: players do not use commands, and gameplay is blocked until ACTIVE.
 */
public class GameplayGateListener implements Listener {

    private final PlayerProgressService progress;

    public GameplayGateListener(PlayerProgressService progress) {
        this.progress = progress;
    }

    // Players never type commands. Only OP/admin may use commands.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (p.isOp()) return;
        e.setCancelled(true);
        // No spam: keep message minimal
        p.sendActionBar(ChatColor.DARK_RED + "Comandos desactivados. Usa los menús.");
    }

    // Block movement while not ACTIVE (hard gate).
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (progress.isActive(p.getUniqueId())) return;
        if (e.getFrom().getWorld() == null || e.getTo() == null) return;
        if (!e.getFrom().toVector().equals(e.getTo().toVector()) || e.getFrom().getYaw() != e.getTo().getYaw() || e.getFrom().getPitch() != e.getTo().getPitch()) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (progress.isActive(p.getUniqueId())) return;
        // allow looking around; but cancel any interaction
        if (e.getAction() != Action.PHYSICAL) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (!progress.isActive(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(PlayerAttemptPickupItemEvent e) {
        if (!progress.isActive(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            if (!progress.isActive(p.getUniqueId())) e.setCancelled(true);
        }
        if (e.getEntity() instanceof Player p) {
            if (!progress.isActive(p.getUniqueId())) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!progress.isActive(p.getUniqueId())) {
            e.setCancelled(true);
            p.sendActionBar(ChatColor.GRAY + "Completa tu selección inicial en el menú.");
        }
    }
}
