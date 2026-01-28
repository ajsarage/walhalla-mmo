package walhalla.mmo.bosses.endgame;

import java.util.*;

/**
 * Parser for ANNEX_ENTITY_DROPS_AND_LOOT_TABLES_v1(.1) format.
 * Supports blocks like:
 *   COW (Vaca)
 *   Drops:
 *   - IT_RES_... | COMMON | CantBase 2
 *
 * Also supports grouped names like:
 *   ZOMBIE / HUSK / DROWNED / ZOMBIE_VILLAGER
 */
public final class EntityLootParser {

    private EntityLootParser() {}

    public static Map<String, List<DropEntry>> parse(String txt) {
        Map<String, List<DropEntry>> out = new LinkedHashMap<>();
        if (txt == null) return out;

        String currentKeyLine = null;
        List<String> currentKeys = List.of();
        boolean inDrops = false;

        for (String raw : txt.split("\r?\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("====") || line.startsWith("---") || line.startsWith("###")) continue;

            // Entity header: UPPERCASE token(s), may include slashes
            // Example: "COW (Vaca)" or "ZOMBIE / HUSK / DROWNED / ZOMBIE_VILLAGER"
            if (looksLikeEntityHeader(line)) {
                currentKeyLine = line;
                currentKeys = normalizeEntityKeys(line);
                for (String k : currentKeys) out.putIfAbsent(k, new ArrayList<>());
                inDrops = false;
                continue;
            }

            if (line.equalsIgnoreCase("Drops:") || line.equalsIgnoreCase("DROPS:")) {
                inDrops = true;
                continue;
            }

            if (!inDrops) continue;
            if (!line.startsWith("-")) continue;

            DropEntry de = parseDropLine(line);
            if (de == null) continue;

            for (String k : currentKeys) {
                out.computeIfAbsent(k, kk -> new ArrayList<>()).add(de);
            }
        }

        // Freeze inner lists
        Map<String, List<DropEntry>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<DropEntry>> e : out.entrySet()) {
            frozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return frozen;
    }

    private static boolean looksLikeEntityHeader(String line) {
        // ignore numbered sections
        if (Character.isDigit(line.charAt(0))) return false;
        if (line.startsWith("-")) return false;
        if (line.contains("|")) return false;
        if (line.equalsIgnoreCase("Drops:")) return false;

        // Must contain at least one uppercase letter and no ":".
        if (line.contains(":")) return false;

        // Take only first token part before "("
        String main = line.split("\\(")[0].trim();

        // Accept "ZOMBIE / HUSK" style
        return main.length() >= 3 && main.equals(main.toUpperCase());
    }

    private static List<String> normalizeEntityKeys(String line) {
        String main = line.split("\\(")[0].trim();
        String[] parts = main.split("/");
        List<String> keys = new ArrayList<>();
        for (String p : parts) {
            String k = p.trim().replace(" ", "_");
            if (!k.isBlank()) keys.add(k);
        }
        return keys;
    }

    private static DropEntry parseDropLine(String line) {
        // "- ITEMID | RAREZA | CantBase N"
        String content = line.substring(1).trim();
        String[] seg = content.split("\\|");
        if (seg.length < 3) return null;
        String itemId = seg[0].trim();
        String rarity = seg[1].trim();
        String third = seg[2].trim();

        int base = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("CantBase\\s*(\\d+)").matcher(third.replace(",", " "));
        if (m.find()) {
            try { base = Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return new DropEntry(itemId, rarity, base);
    }
}
