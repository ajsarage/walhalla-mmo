package walhalla.mmo.zones;

import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.zones.ZoneService;
import walhalla.mmo.zones.listeners.ZoneTrackerListener;
import walhalla.mmo.zones.service.ZoneManager;

public class WalhallaZonesPlugin extends JavaPlugin {

    private ZoneManager zones;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        zones = new ZoneManager(this);
        zones.loadAll();

        // Register service for other modules (CoreAPI reads it via ServicesManager)
        getServer().getServicesManager().register(ZoneService.class, zones, this, org.bukkit.plugin.ServicePriority.Normal);

        // Track players
        getServer().getPluginManager().registerEvents(new ZoneTrackerListener(zones), this);

        // OP-only admin commands
        if (getCommand("whzones") != null) {
            getCommand("whzones").setExecutor(new ZoneAdminCommand(zones));
        }

        getLogger().info("WalhallaZones enabled. Zones=" + zones.getAllZoneIds().size());
    }

    @Override
    public void onDisable() {
        getLogger().info("WalhallaZones disabled.");
    }
}
