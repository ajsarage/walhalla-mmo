package walhalla.mmo.spells.engine;

import java.util.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.progress.PlayerData;
import walhalla.mmo.spells.catalog.YmlSpellCatalog;
import walhalla.mmo.spells.mage.MageSetCatalog;
import walhalla.mmo.spells.mage.MageSetService;
import walhalla.mmo.spells.model.SpellDefinition;
import walhalla.mmo.spells.model.SpellSlot;

/**
 * Ensures players have something equipped from day 1.
 * Phase 10: if player has MAGO, ensure a mage set is selected and applied to equips.
 */
public class EquipBootstrapListener implements Listener {

    private final YmlSpellCatalog catalog;
    private final EquipService equips;
    private final MageSetCatalog mageSets;
    private final MageSetService mageSetService;

    public EquipBootstrapListener(YmlSpellCatalog catalog, EquipService equips, MageSetCatalog mageSets, MageSetService mageSetService) {
        this.catalog = catalog;
        this.equips = equips;
        this.mageSets = mageSets;
        this.mageSetService = mageSetService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (CoreAPI.getLifecycleState(id) != PlayerData.PlayerLifecycleState.ACTIVE) return;

        List<String> branches = CoreAPI.getChosenBranches(id);
        boolean isMage = branches.stream().anyMatch(b -> b.equalsIgnoreCase("MAGO"));

        // Mage: ensure set is selected and applied (overrides equipped slots).
        if (isMage && mageSetService != null && mageSets != null) {
            mageSetService.ensureDefault(id);
            mageSetService.getPlayerSetId(id).flatMap(mageSets::get).ifPresent(def -> mageSetService.applyToEquips(id, def));
            return;
        }

        // Non-mage: if nothing equipped at all, auto-equip defaults.
        if (!equips.getAll(id).isEmpty()) return;

        // Priority order: then the rest.
        List<String> priority = new ArrayList<>();
        for (String b : branches) {
            String up = b.toUpperCase(Locale.ROOT);
            if (!priority.contains(up) && (up.equals("GUERRERO") || up.equals("TANQUE") || up.equals("CAZADOR"))) {
                priority.add(up);
            }
        }

        // Equip per slot: pick first spell matching any priority branch.
        for (SpellSlot slot : List.of(SpellSlot.PRIMARY, SpellSlot.SECONDARY, SpellSlot.SPECIAL)) {
            SpellDefinition pick = pickFirst(priority, slot);
            if (pick != null) equips.setEquipped(id, slot, pick.id());
        }
    }

    private SpellDefinition pickFirst(List<String> branches, SpellSlot slot) {
        for (String b : branches) {
            for (SpellDefinition d : catalog.getAllDefinitions()) {
                if (d.slot() == slot && d.branchId().equalsIgnoreCase(b)) {
                    return d;
                }
            }
        }
        // fallback: any definition for that slot
        for (SpellDefinition d : catalog.getAllDefinitions()) {
            if (d.slot() == slot) return d;
        }
        return null;
    }
}
