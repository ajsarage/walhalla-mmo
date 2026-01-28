package walhalla.mmo.zones.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import walhalla.mmo.zones.service.ZoneManager;

/** Keeps player -> zone mapping up to date. */
public class ZoneTrackerListener implements Listener {

    private final ZoneManager zones;

    public ZoneTrackerListener(ZoneManager zones) {
        this.zones = zones;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        zones.updatePlayerZone(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        // Cheap filter: only recompute on block change.
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }
        Player p = e.getPlayer();
        String before = zones.getCurrentZoneId(p.getUniqueId());
        String now = zones.updatePlayerZone(p);
        // Future: fire zone change event for UI/telemetry.
        // Intentionally no chat spam in Phase 13.
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Mapping will be cleared lazily; no persistence here.
    }
}
