package walhalla.mmo.professions;

import walhalla.mmo.professions.GatheringRegistry.Action;
import walhalla.mmo.professions.GatheringRegistry.Entry;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal TXT parsing helpers for Phase 8.
 *
 * These parsers are intentionally strict about the canonical labels
 * (Acción/Profesión/Recurso/Rareza/Cantidad Base) but tolerant about
 * spacing and separators.
 */
final class CanonParsing {

    private CanonParsing() {}

    static List<Entry> parseGatheringTable(String txt) {
        if (txt == null) return List.of();
        List<Entry> out = new ArrayList<>();

        Action currentAction = null;
        String currentProfession = null;
        Integer currentTier = null;

        String resource = null;
        String rarity = null;
        Integer amount = null;

        // Metal expansion support
        List<String> metalSet = List.of("COPPER", "IRON", "GOLD", "NETHERITE");

        String[] lines = txt.split("\r?\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("====")) continue;

            if (line.toLowerCase(Locale.ROOT).startsWith("acción:")) {
                currentAction = Action.fromCanon(line.substring(line.indexOf(':') + 1));
                // reset
                currentTier = null;
                continue;
            }
            if (line.toLowerCase(Locale.ROOT).startsWith("profesión:")) {
                currentProfession = line.substring(line.indexOf(':') + 1).trim();
                continue;
            }

            Matcher t = Pattern.compile("^T(\\d+)$", Pattern.CASE_INSENSITIVE).matcher(line);
            if (t.find()) {
                // flush previous tier block if complete
                currentTier = Integer.parseInt(t.group(1));
                resource = null;
                rarity = null;
                amount = null;
                continue;
            }

            if (line.toLowerCase(Locale.ROOT).startsWith("recurso:")) {
                resource = line.substring(line.indexOf(':') + 1).trim();
                continue;
            }
            if (line.toLowerCase(Locale.ROOT).startsWith("rareza:")) {
                rarity = line.substring(line.indexOf(':') + 1).trim().toUpperCase(Locale.ROOT);
                continue;
            }
            if (line.toLowerCase(Locale.ROOT).startsWith("cantidad base:")) {
                try {
                    amount = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                } catch (Exception ignored) {
                    amount = null;
                }
            }

            // If we have a full entry, commit it. This may happen after Cantidad Base
            if (currentAction != null && currentProfession != null && currentTier != null
                    && resource != null && rarity != null && amount != null) {

                // Expand placeholders like IT_RES_METAL_ORE_<METAL>_T1
                if (resource.contains("<METAL>")) {
                    for (String m : metalSet) {
                        String id = resource.replace("<METAL>", m);
                        out.add(new Entry(currentAction, currentProfession, currentTier, id, rarity, amount));
                    }
                } else {
                    out.add(new Entry(currentAction, currentProfession, currentTier, resource, rarity, amount));
                }

                // prevent accidental duplicates for same block
                resource = null;
                rarity = null;
                amount = null;
            }
        }

        return out;
    }
}
