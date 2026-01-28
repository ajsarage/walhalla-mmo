package walhalla.mmo.spells.mage;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.canon.CanonFile;

/**
 * Parses ANNEX_MAGE_WARFRAME_SETS_v1.txt from the canon service.
 *
 * Format (simple V1):
 *  SET <SET_ID>
 *  ELEMENT <ELEMENT_ID>
 *  PRIMARY <SPELL_ID>
 *  SECONDARY <SPELL_ID>
 *  SPECIAL <SPELL_ID>
 */
public final class MageSetCatalog {

    private static final Pattern LINE = Pattern.compile("^\s*(SET|ELEMENT|PRIMARY|SECONDARY|SPECIAL)\s+(.+?)\s*$", Pattern.CASE_INSENSITIVE);

    private final Map<String, MageSetDefinition> sets = new LinkedHashMap<>();

    public void reload() {
        sets.clear();
        String raw = CoreAPI.getCanonService()
                .flatMap(c -> c.getRaw(CanonFile.ANNEX_MAGE_SETS))
                .orElse("");

        parse(raw);
    }

    public Collection<MageSetDefinition> all() { return Collections.unmodifiableCollection(sets.values()); }

    public Optional<MageSetDefinition> get(String setId) {
        if (setId == null) return Optional.empty();
        return Optional.ofNullable(sets.get(setId.trim().toUpperCase(Locale.ROOT)));
    }

    public List<String> getAllSetIds() { return new ArrayList<>(sets.keySet()); }

    private void parse(String raw) {
        if (raw == null || raw.isBlank()) return;

        String curSet = null;
        String element = null;
        String p = null, s = null, sp = null;

        String[] lines = raw.split("\\R");
        for (String line : lines) {
            String l = line.strip();
            if (l.isEmpty()) continue;
            if (l.startsWith("#")) continue;
            if (l.startsWith("----")) continue;

            Matcher m = LINE.matcher(l);
            if (!m.matches()) continue;

            String key = m.group(1).toUpperCase(Locale.ROOT);
            String val = m.group(2).trim();

            switch (key) {
                case "SET" -> {
                    // flush previous
                    flush(curSet, element, p, s, sp);
                    curSet = val.toUpperCase(Locale.ROOT);
                    element = null; p = null; s = null; sp = null;
                }
                case "ELEMENT" -> element = val.toUpperCase(Locale.ROOT);
                case "PRIMARY" -> p = val.toUpperCase(Locale.ROOT);
                case "SECONDARY" -> s = val.toUpperCase(Locale.ROOT);
                case "SPECIAL" -> sp = val.toUpperCase(Locale.ROOT);
            }
        }
        flush(curSet, element, p, s, sp);
    }

    private void flush(String setId, String element, String primary, String secondary, String special) {
        if (setId == null) return;
        if (primary == null || secondary == null || special == null) return;
        String el = element == null ? "UNKNOWN" : element;
        sets.put(setId, new MageSetDefinition(setId, el, primary, secondary, special));
    }
}
