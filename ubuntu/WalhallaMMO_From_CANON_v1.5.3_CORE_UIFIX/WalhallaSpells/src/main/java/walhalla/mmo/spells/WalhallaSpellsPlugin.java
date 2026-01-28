package walhalla.mmo.spells;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.spells.SpellCatalogBridge;
import walhalla.mmo.spells.catalog.YmlSpellCatalog;
import walhalla.mmo.spells.engine.*;
import walhalla.mmo.spells.input.SpellInputListener;
import walhalla.mmo.spells.mage.MageSetCatalog;
import walhalla.mmo.spells.mage.MageSetService;

import java.util.Optional;
import java.util.UUID;

/**
 * WalhallaSpells:
 * - Still uses YML definitions for concrete spells.
 * - Phase 10: adds Mage "Warframe-style" Sets loaded from canon annex.
 */
public class WalhallaSpellsPlugin extends JavaPlugin {

    private YmlSpellCatalog catalog;
    private CooldownTracker cooldowns;
    private EquipService equips;
    private SpellExecutionEngine engine;

    private MageSetCatalog mageSets;
    private MageSetService mageSetService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.catalog = new YmlSpellCatalog(this);
        this.catalog.reload();

        this.cooldowns = new CooldownTracker();
        this.equips = new EquipService();
        this.engine = new SpellExecutionEngine(catalog, cooldowns, equips);

        // Phase 10: Mage sets (canon-driven)
        this.mageSets = new MageSetCatalog();
        this.mageSets.reload();

        this.mageSetService = new MageSetService(this, mageSets, equips);
        this.mageSetService.load();

        // Expose catalog to UI via ServicesManager (UI never reads YML directly).
        Bukkit.getServicesManager().register(SpellCatalogBridge.class, catalog, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(walhalla.mmo.core.api.spells.SpellLoadoutBridge.class, new SpellLoadoutBridgeImpl(equips), this, ServicePriority.Normal);

        // Input + default equip
        Bukkit.getPluginManager().registerEvents(new EquipBootstrapListener(catalog, equips, mageSets, mageSetService), this);
        Bukkit.getPluginManager().registerEvents(new SpellInputListener(engine), this);

        getLogger().info("WalhallaSpells enabled (Phase 10). Loaded " + catalog.getAllSpellIds().size() + " spells. Mage sets: " + mageSets.getAllSetIds().size());
    }

    @Override
    public void onDisable() {
        try {
            if (mageSetService != null) mageSetService.save();
        } catch (Exception ignored) {}
        Bukkit.getServicesManager().unregisterAll(this);
        getLogger().info("WalhallaSpells disabled.");
    }

    public YmlSpellCatalog getCatalog() { return catalog; }
    public EquipService getEquips() { return equips; }

    // -------- Phase 10 admin bridge (called via Core /walhalla mage ...) --------

    public void reloadMageSets() {
        if (mageSets != null) mageSets.reload();
    }

    public java.util.List<String> getMageSetIds() {
        return mageSets == null ? java.util.List.of() : mageSets.getAllSetIds();
    }

    public boolean setMageSet(UUID playerId, String setId) {
        if (mageSetService == null) return false;
        return mageSetService.setPlayerSet(playerId, setId);
    }

    public Optional<String> getMageSet(UUID playerId) {
        if (mageSetService == null) return Optional.empty();
        return mageSetService.getPlayerSetId(playerId);
    }
}
