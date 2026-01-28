package walhalla.mmo.professions;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.progress.ProgressMutationBridge;
import walhalla.mmo.core.progress.PlayerData;

public final class GatheringListener implements Listener {

    private final Plugin plugin;
    private final GatheringRegistry registry;
    private final ProgressMutationBridge progress;
    private final ItemFactory items;

    public GatheringListener(Plugin plugin, GatheringRegistry registry, ProgressMutationBridge progress) {
        this.plugin = plugin;
        this.registry = registry;
        this.progress = progress;
        this.items = new ItemFactory(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) return;
        if (CoreAPI.getLifecycleState(p.getUniqueId()) != PlayerData.PlayerLifecycleState.ACTIVE) return;

        Block b = e.getBlock();
        registry.resolveBlock(b.getType()).ifPresent(entry -> {
            // Crops: only if mature
            if (b.getBlockData() instanceof Ageable age) {
                if (age.getAge() < age.getMaximumAge()) return;
            }

            e.setDropItems(false);
            e.setExpToDrop(0);

            int amount = Math.max(1, entry.baseAmount());
            ItemStack drop = items.createResource(entry.resourceItemId(), amount);
            p.getInventory().addItem(drop);

            // Phase 8: XP source values are not defined in annex table. Use a minimal deterministic gain
            // proportional to the canonical base amount (implementation detail; can be rebalanced later).
            if (progress != null) {
                long xp = Math.max(0L, (long) amount);
                if (xp > 0) progress.addProfessionXp(p.getUniqueId(), entry.professionId(), xp);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        if (killer.getGameMode() == GameMode.CREATIVE) return;
        if (CoreAPI.getLifecycleState(killer.getUniqueId()) != PlayerData.PlayerLifecycleState.ACTIVE) return;

        registry.resolveEntityDrop(e.getEntityType()).ifPresent(entry -> {
            int amount = Math.max(1, entry.baseAmount());
            ItemStack drop = items.createResource(entry.resourceItemId(), amount);
            e.getDrops().add(drop);
            if (progress != null) {
                long xp = Math.max(0L, (long) amount);
                if (xp > 0) progress.addProfessionXp(killer.getUniqueId(), entry.professionId(), xp);
            }
        });
    }
}
