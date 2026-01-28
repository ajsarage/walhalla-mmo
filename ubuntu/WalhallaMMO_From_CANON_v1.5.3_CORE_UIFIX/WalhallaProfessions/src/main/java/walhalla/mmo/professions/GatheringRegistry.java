package walhalla.mmo.professions;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.canon.CanonFile;

import java.util.*;

/**
 * Phase 8: Data-driven gathering.
 *
 * Canon source of truth:
 * - ANNEX_GATHERING_RESOURCE_TABLE_v1.txt (action/profession/resource/tier/rarity/base amount)
 *
 * Note: Minecraft block/entity -> canonical action/tier mapping is an implementation detail.
 * This registry provides a conservative default mapping that can be iterated later without
 * changing canon data.
 */
public final class GatheringRegistry {

    public enum Action {
        MINING, LOGGING, FARMING, HERBALISM, FISHING, HUNTING;

        public static Action fromCanon(String s) {
            if (s == null) return null;
            try {
                return Action.valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public record Entry(Action action, String professionId, int tier, String resourceItemId, String rarity, int baseAmount) {}

    private final JavaPlugin plugin;
    private final Map<Action, Map<Integer, List<Entry>>> byActionTier = new EnumMap<>(Action.class);

    public GatheringRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        byActionTier.clear();
        for (Action a : Action.values()) byActionTier.put(a, new HashMap<>());

        String txt = CoreAPI.getCanonService()
                .flatMap(c -> c.getRaw(CanonFile.ANNEX_GATHERING_TABLE))
                .orElse("");

        if (txt.isBlank()) {
            plugin.getLogger().severe("Canon gathering annex is missing/empty: " + CanonFile.ANNEX_GATHERING_TABLE.fileName());
            return;
        }

        List<Entry> parsed = CanonParsing.parseGatheringTable(txt);
        Set<String> canonItems = CoreAPI.getCanonItemIds();
        int accepted = 0;

        for (Entry e : parsed) {
            if (!canonItems.contains(e.resourceItemId())) {
                plugin.getLogger().severe("Gathering entry references unknown itemId: " + e.resourceItemId() + " (action=" + e.action() + ", tier=" + e.tier() + ")");
                continue;
            }
            byActionTier.computeIfAbsent(e.action(), k -> new HashMap<>())
                    .computeIfAbsent(e.tier(), k -> new ArrayList<>())
                    .add(e);
            accepted++;
        }

        plugin.getLogger().info("GatheringRegistry loaded entries=" + accepted);
    }

    public int size() {
        int total = 0;
        for (Map<Integer, List<Entry>> tierMap : byActionTier.values()) {
            for (List<Entry> entries : tierMap.values()) {
                total += entries.size();
            }
        }
        return total;
    }

    public Optional<Entry> resolveBlock(Material blockType) {
        if (blockType == null) return Optional.empty();

        // LOGGING
        Integer logTier = tierForLog(blockType);
        if (logTier != null) return resolve(Action.LOGGING, logTier);

        // FARMING (fibers)
        Integer farmTier = tierForFarming(blockType);
        if (farmTier != null) return resolve(Action.FARMING, farmTier);

        // HERBALISM (flowers)
        Integer herbTier = tierForHerb(blockType);
        if (herbTier != null) return resolve(Action.HERBALISM, herbTier);

        // MINING (rocks + ores)
        Integer miningTier = tierForMining(blockType);
        if (miningTier != null) return resolve(Action.MINING, miningTier);

        return Optional.empty();
    }

    public Optional<Entry> resolveEntityDrop(EntityType type) {
        if (type == null) return Optional.empty();

        // Herbalism reactives (implementation detail derived from annex tier semantics)
        Integer herbTier = switch (type) {
            case BLAZE -> 5;
            case GHAST -> 6;
            case ENDERMAN -> 7;
            case ENDER_DRAGON -> 8;
            default -> null;
        };
        if (herbTier != null) return resolve(Action.HERBALISM, herbTier);

        // Hunting is a future phase (requires explicit mob tables). Keep empty for now.
        return Optional.empty();
    }

    private Optional<Entry> resolve(Action action, int tier) {
        List<Entry> list = byActionTier.getOrDefault(action, Map.of()).getOrDefault(tier, List.of());
        if (list.isEmpty()) return Optional.empty();
        // Canon can define multiple outputs per action/tier. For Phase 8, pick first deterministically.
        return Optional.of(list.get(0));
    }

    private static Integer tierForLog(Material m) {
        String n = m.name();
        if (!(n.endsWith("_LOG") || n.endsWith("_WOOD") || n.contains("STRIPPED_"))) return null;
        // Normalize stripped
        n = n.replace("STRIPPED_", "");
        if (n.startsWith("OAK_")) return 1;
        if (n.startsWith("BIRCH_")) return 2;
        if (n.startsWith("SPRUCE_")) return 3;
        if (n.startsWith("JUNGLE_")) return 4;
        if (n.startsWith("ACACIA_")) return 5;
        if (n.startsWith("DARK_OAK_")) return 6;
        if (n.startsWith("MANGROVE_")) return 7;
        if (n.startsWith("CHERRY_")) return 8;
        return null;
    }

    private static Integer tierForMining(Material m) {
        // Rocks
        return switch (m) {
            case STONE, COBBLESTONE -> 1;
            case ANDESITE -> 2;
            case DIORITE -> 3;
            case GRANITE -> 4;
            case DEEPSLATE, COBBLED_DEEPSLATE -> 5;
            case BASALT -> 6;
            case TUFF -> 7;
            case OBSIDIAN -> 8;
            default -> {
                // Ores -> treat as tier1 metal ore gathering for now
                if (m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS) yield 1;
                yield null;
            }
        };
    }

    private static Integer tierForFarming(Material m) {
        return switch (m) {
            case WHEAT, HAY_BLOCK -> 1;
            case SUGAR_CANE -> 2;
            case BAMBOO -> 3;
            case VINE -> 4;
            case NETHER_WART, NETHER_SPROUTS -> 5;
            case WARPED_FUNGUS, WARPED_ROOTS -> 6;
            case CRIMSON_FUNGUS, CRIMSON_ROOTS -> 7;
            case CHORUS_PLANT, CHORUS_FLOWER -> 8;
            default -> null;
        };
    }

    private static Integer tierForHerb(Material m) {
        return switch (m) {
            case DANDELION -> 1;
            case POPPY -> 2;
            case BLUE_ORCHID -> 3;
            case ALLIUM -> 4;
            default -> null;
        };
    }
}
