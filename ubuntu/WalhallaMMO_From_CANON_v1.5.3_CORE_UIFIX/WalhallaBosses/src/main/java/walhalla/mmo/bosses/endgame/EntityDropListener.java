package walhalla.mmo.bosses.endgame;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.zones.ZoneRulesView;
import walhalla.mmo.core.api.zones.ZoneType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Overrides vanilla drops with canonical entity drop tables.
 * Applies risk/reward multipliers by ZoneType (from WalhallaZones zones_multipliers.yml).
 */
public final class EntityDropListener implements Listener {

    private final JavaPlugin plugin;
    private final EndgameConfig config;
    private final EndgameLootRegistry registry;
    private final BossItemFactory items;
    private final ZoneMultipliers multipliers;

    public EntityDropListener(JavaPlugin plugin, EndgameConfig config, EndgameLootRegistry registry) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.items = new BossItemFactory(plugin);
        this.multipliers = ZoneMultipliers.loadFromZonesDataFolder(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        if (!config.overrideVanillaDrops) return;
        if (!registry.isLoaded()) return;

        LivingEntity dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;

        String entityKey = EntityKeyMapper.toCanonKey(dead);
        if (entityKey == null) return;

        List<DropEntry> drops = registry.getDropsFor(entityKey);
        if (drops.isEmpty()) return;

        // Clear vanilla drops/exp to enforce canon-controlled economy
        e.getDrops().clear();
        e.setDroppedExp(0);

        ZoneType zoneType = ZoneType.SAFE;
        var zoneIdOpt = CoreAPI.getCurrentZoneId(killer.getUniqueId());
        if (zoneIdOpt.isPresent()) {
            var zoneRulesOpt = CoreAPI.getZoneRules(zoneIdOpt.get());
            if (zoneRulesOpt != null) {
                zoneType = zoneRulesOpt.zoneType();
            }
        }

        double lootMult = multipliers.lootMultiplier(zoneType);

        // Elite bonus rolls if tagged
        double extraRolls = 0.0;
        if (EliteTags.isElite(dead)) {
            extraRolls = switch (zoneType) {
                case BLACK -> config.eliteExtraLootRollsBlack;
                case DANGEROUS -> config.eliteExtraLootRollsDangerous;
                case CONTESTED -> config.eliteExtraLootRollsContested;
                default -> 0.0;
            };
        }

        int rolls = Math.max(1, (int) Math.round(lootMult + extraRolls));
        Location loc = dead.getLocation();

        for (int r = 0; r < rolls; r++) {
            for (DropEntry de : drops) {
                if (de.baseAmount() <= 0) continue; // canon explicitly says not a kill-drop
                loc.getWorld().dropItemNaturally(loc, items.createResource(de.itemId(), de.baseAmount()));
            }
        }
    }
}
