package walhalla.mmo.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.zones.DeathPenalty;
import walhalla.mmo.core.api.zones.ZoneRulesView;
import walhalla.mmo.core.api.zones.ZoneType;

/**
 * Phase 13 (Endgame): death penalties scale with zone risk (ANNEX_GAMEPLAY_LOOP_AND_ENDGAME_v1 + ANNEX_ECONOMY_SINKS_v1).
 *
 * Guarantees preserved:
 * - No uncontrolled item drops by default.
 * - Penalty is never "free" in risky zones: we apply durability damage (repair sink).
 */
public class DeathRespawnListener implements Listener {

    private final WalhallaCombatPlugin plugin;
    private final RespawnProtectionService respawnProtection;

    public DeathRespawnListener(WalhallaCombatPlugin plugin, RespawnProtectionService respawnProtection) {
        this.plugin = plugin;
        this.respawnProtection = respawnProtection;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        String zid = CoreAPI.getCurrentZoneId(p.getUniqueId()).orElse(null);
        ZoneRulesView rules = CoreAPI.getZoneRules(zid);

        // Avoid uncontrolled drops in production.
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.setDeathMessage(null);

        // Apply a canon-aligned sink in risky zones: gear durability damage (repair cost).
        if (rules != null && rules.deathPenalty() != null) {
            if (rules.deathPenalty() == DeathPenalty.XP_AND_GEAR_DURABILITY) {
                double pct = switch (rules.zoneType()) {
                    case BLACK -> 0.20;      // severe
                    case DANGEROUS -> 0.10;  // significant
                    default -> 0.0;
                };
                if (pct > 0.0) {
                    applyDurabilityPenalty(p, pct);
                }
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        String zid = CoreAPI.getCurrentZoneId(p.getUniqueId()).orElse(null);
        ZoneRulesView rules = CoreAPI.getZoneRules(zid);

        respawnProtection.apply(p.getUniqueId(), rules.respawnProtectionDurationMs());

        if (e.getRespawnLocation() == null || e.getRespawnLocation().getWorld() == null) {
            e.setRespawnLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }

    private void applyDurabilityPenalty(Player p, double pct) {
        // Apply to armor + main hand + offhand. If an item is not damageable, ignore.
        for (ItemStack it : p.getInventory().getArmorContents()) {
            damageItem(it, pct);
        }
        damageItem(p.getInventory().getItemInMainHand(), pct);
        damageItem(p.getInventory().getItemInOffHand(), pct);
    }

    private void damageItem(ItemStack it, double pct) {
        if (it == null) return;
        if (it.getType().getMaxDurability() <= 0) return;

        var meta = it.getItemMeta();
        if (!(meta instanceof Damageable dmg)) return;

        int max = it.getType().getMaxDurability();
        int add = Math.max(1, (int) Math.round(max * pct));
        dmg.setDamage(Math.min(max - 1, dmg.getDamage() + add));
        it.setItemMeta(dmg);
    }
}
