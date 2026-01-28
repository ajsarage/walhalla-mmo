package walhalla.mmo.combat.kits;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the canonical annex format:
 *  --------------------------------
 *  5.x KIT: NAME
 *  --------------------------------
 *  Ataque Básico: ...
 *  - Coste: ...
 *  - Cooldown: ...
 * (same for secundario/especial)
 *
 * The canon uses qualitative levels (Bajo/Medio/Alto/Muy bajo/etc). Numeric resolution is
 * performed by CombatKitRuntime using configurable mappings.
 */
public final class CanonKitParser {

    private static final Pattern KIT_HEADER = Pattern.compile("^\\s*-{3,}\\s*$\\R\\s*5\\.[0-9]+\\s+KIT:\\s*(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern ATTACK_LINE = Pattern.compile("^\\s*Ataque\\s+(B[aá]sico|Secundario|Especial)\\s*:\\s*(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern COST_LINE = Pattern.compile("^\\s*-\\s*Coste\\s*:\\s*(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern CD_LINE = Pattern.compile("^\\s*-\\s*Cooldown\\s*:\\s*(.+?)\\s*$", Pattern.MULTILINE);


    private CanonKitParser() {}

    public static KitParseResult parse(String branchId, String raw) {
        if (raw == null) return new KitParseResult(List.of(), List.of("Missing annex text for " + branchId));

        List<String> warnings = new ArrayList<>();
        List<CombatKit> kits = new ArrayList<>();

        // Split into sections by KIT headers using manual scanning for stability.
        Matcher m = KIT_HEADER.matcher(raw);
        List<Integer> starts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        while (m.find()) {
            starts.add(m.start());
            names.add(m.group(1).trim());
        }
        for (int i = 0; i < starts.size(); i++) {
            int s = starts.get(i);
            int e = (i + 1 < starts.size()) ? starts.get(i + 1) : raw.length();
            String section = raw.substring(s, e);

            String kitName = names.get(i);
            Map<String, Ability> abilities = parseAbilities(section);

            Ability basic = abilities.getOrDefault("BASICO", new Ability("BASICO", "BAJO", "MUY_BAJO"));
            Ability secondary = abilities.getOrDefault("SECUNDARIO", new Ability("SECUNDARIO", "MEDIO", "MEDIO"));
            Ability special = abilities.getOrDefault("ESPECIAL", new Ability("ESPECIAL", "ALTO", "ALTO"));

            String kitId = branchId.toUpperCase(Locale.ROOT) + "_" + toId(kitName);
            kits.add(new CombatKit(kitId, branchId.toUpperCase(Locale.ROOT), kitName, basic, secondary, special));

            if (!abilities.containsKey("BASICO")) warnings.add("Kit " + kitId + " missing basic attack block, using defaults.");
            if (!abilities.containsKey("SECUNDARIO")) warnings.add("Kit " + kitId + " missing secondary attack block, using defaults.");
            if (!abilities.containsKey("ESPECIAL")) warnings.add("Kit " + kitId + " missing special attack block, using defaults.");
        }

        if (kits.isEmpty()) {
            warnings.add("No kits parsed for branch " + branchId + ". Check annex formatting.");
        }

        return new KitParseResult(Collections.unmodifiableList(kits), Collections.unmodifiableList(warnings));
    }

    private static Map<String, Ability> parseAbilities(String section) {
        Map<String, Ability> out = new HashMap<>();

        Matcher attackM = ATTACK_LINE.matcher(section);
        List<Integer> aStarts = new ArrayList<>();
        List<String> aTypes = new ArrayList<>();
        List<String> aNames = new ArrayList<>();
        while (attackM.find()) {
            aStarts.add(attackM.start());
            String t = normalizeKey(attackM.group(1));
            aTypes.add(t);
            aNames.add(attackM.group(2).trim());
        }

        for (int i = 0; i < aStarts.size(); i++) {
            int s = aStarts.get(i);
            int e = (i + 1 < aStarts.size()) ? aStarts.get(i + 1) : section.length();
            String block = section.substring(s, e);

            String cost = findFirst(COST_LINE, block).orElse("MEDIO");
            String cd = findFirst(CD_LINE, block).orElse("MEDIO");

            out.put(aTypes.get(i), new Ability(aNames.get(i), cost, cd));
        }

        return out;
    }

    private static Optional<String> findFirst(Pattern p, String text) {
        Matcher m = p.matcher(text);
        if (m.find()) return Optional.ofNullable(m.group(1)).map(String::trim);
        return Optional.empty();
    }

    private static String normalizeKey(String in) {
        String s = in == null ? "" : in.trim().toUpperCase(Locale.ROOT);
        s = stripAccents(s);
        if (s.contains("BAS")) return "BASICO";
        if (s.contains("SEC")) return "SECUNDARIO";
        if (s.contains("ESP")) return "ESPECIAL";
        return s;
    }

    private static String toId(String display) {
        String s = stripAccents(display).toUpperCase(Locale.ROOT);
        s = s.replaceAll("[^A-Z0-9]+", "_");
        s = s.replaceAll("^_+|_+$", "");
        return s;
    }

    private static String stripAccents(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
