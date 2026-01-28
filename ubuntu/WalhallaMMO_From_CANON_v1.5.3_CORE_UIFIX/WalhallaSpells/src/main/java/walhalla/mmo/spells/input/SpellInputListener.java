package walhalla.mmo.spells.input;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.progress.PlayerData;
import walhalla.mmo.spells.engine.SpellExecutionEngine;
import walhalla.mmo.spells.model.SpellSlot;

/**
 * Player input for MAGE spells (Phase 10).
 *
 * Rules:
 * - Players never type commands.
 * - Non-mage combat abilities are handled by WalhallaCombat.
 *
 * Mapping (Mage staff only):
 * - Left-click: PRIMARY
 * - Right-click: SECONDARY
 * - Swap-hand (F): SPECIAL
 */
public class SpellInputListener implements Listener {

    private final SpellExecutionEngine engine;

    public SpellInputListener(SpellExecutionEngine engine) {
        this.engine = engine;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (CoreAPI.getLifecycleState(p.getUniqueId()) != PlayerData.PlayerLifecycleState.ACTIVE) return;
        if (!isMageStaff(p)) return;

        SpellSlot slot;
        switch (e.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> slot = SpellSlot.PRIMARY;
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> slot = SpellSlot.SECONDARY;
            default -> { return; }
        }

        boolean handled = engine.castEquipped(p, slot);
        if (handled) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (CoreAPI.getLifecycleState(p.getUniqueId()) != PlayerData.PlayerLifecycleState.ACTIVE) return;
        if (!isMageStaff(p)) return;

        boolean handled = engine.castEquipped(p, SpellSlot.SPECIAL);
        if (handled) e.setCancelled(true);
    }

    private boolean isMageStaff(Player p) {
        boolean hasMage = CoreAPI.getChosenBranches(p.getUniqueId()).stream()
                .anyMatch(s -> s.equalsIgnoreCase("MAGO"));
        if (!hasMage) return false;

        ItemStack main = p.getInventory().getItemInMainHand();
        Material mat = main != null ? main.getType() : Material.AIR;
        return mat == Material.BLAZE_ROD || mat == Material.STICK;
    }
}
