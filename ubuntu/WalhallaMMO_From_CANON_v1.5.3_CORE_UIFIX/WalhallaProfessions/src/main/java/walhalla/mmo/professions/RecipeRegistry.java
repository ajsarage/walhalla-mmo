package walhalla.mmo.professions;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.canon.CanonFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 11: Canon-driven crafting & refining recipes.
 *
 * Source of truth:
 * - ANNEX_REFINING_RECIPES_v1.txt
 * - ANNEX_CRAFTING_RECIPES_ALL_v1.txt
 *
 * Rules:
 * - No recipes.yml (players never edit gameplay data).
 * - Only expands placeholders when resulting itemIds exist in canon catalog.
 * - If a recipe references unknown itemIds => skipped with error log (no silent fallbacks).
 */
public final class RecipeRegistry {

    private final JavaPlugin plugin;
    private final List<CraftRecipe> recipes = new ArrayList<>();

    public RecipeRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        recipes.clear();

        String refining = CoreAPI.getCanonService()
                .flatMap(c -> c.getRaw(CanonFile.ANNEX_REFINING_RECIPES))
                .orElse("");
        String crafting = CoreAPI.getCanonService()
                .flatMap(c -> c.getRaw(CanonFile.ANNEX_CRAFTING_RECIPES))
                .orElse("");

        Set<String> canonItems = CoreAPI.getCanonItemIds();
        Set<String> canonStations = CoreAPI.getCanonStationIds();

        if (!refining.isBlank()) {
            recipes.addAll(CanonRecipeParsing.parseRefining(refining, canonItems, canonStations, plugin));
        } else {
            plugin.getLogger().severe("Canon refining annex missing/empty: " + CanonFile.ANNEX_REFINING_RECIPES.fileName());
        }

        if (!crafting.isBlank()) {
            recipes.addAll(CanonRecipeParsing.parseStructuredCrafting(crafting, canonItems, canonStations, plugin));
        } else {
            plugin.getLogger().severe("Canon crafting annex missing/empty: " + CanonFile.ANNEX_CRAFTING_RECIPES.fileName());
        }

        plugin.getLogger().info("RecipeRegistry loaded recipes=" + recipes.size());
    }

    public List<CraftRecipe> all() {
        return Collections.unmodifiableList(recipes);
    }

    public int size() { return recipes.size(); }

    // ------------------------------------------------------------
    // Internal parsing helpers (strict, but tolerant of formatting)
    // ------------------------------------------------------------
    static final class CanonRecipeParsing {

        private static final Pattern SEP = Pattern.compile("^-{10,}\\s*$");
        private static final Pattern COST = Pattern.compile("Coste\\s*:\\s*(\\d+)\\s*WCoin", Pattern.CASE_INSENSITIVE);
        private static final Pattern TIME = Pattern.compile("Tiempo\\s*:\\s*(\\d+)\\s*s", Pattern.CASE_INSENSITIVE);

        private CanonRecipeParsing() {}

        static List<CraftRecipe> parseRefining(String txt, Set<String> canonItems, Set<String> canonStations, JavaPlugin plugin) {
            List<CraftRecipe> out = new ArrayList<>();
            List<String> lines = Arrays.asList(txt.split("\r?\n"));

            String[] currentIdRef = {null};
            String[] stationRef = {null};
            String[] professionRef = {null};
            long[] moneyCostRef = {0};
            int[] timeSecRef = {0};

            List<String> inputs = new ArrayList<>();
            List<String> outputs = new ArrayList<>();
            boolean[] inInputRef = {false};
            boolean[] inOutputRef = {false};

            Runnable flush = () -> {
                if (currentIdRef[0] == null) return;

                List<ExpandedLine> in = expandItemLines(inputs, canonItems);
                List<ExpandedLine> outLines = expandItemLines(outputs, canonItems);

                // build recipes by pairing expanded outputs 1:1; if multiple outputs, create one recipe per output
                for (ExpandedLine o : outLines) {
                    Map<String,Integer> ing = new LinkedHashMap<>();
                    for (ExpandedLine ii : in) {
                        ing.merge(ii.itemId(), ii.amount(), Integer::sum);
                    }
                    if (o.itemId() == null) continue;

                    if (!canonStations.isEmpty() && stationRef[0] != null && !stationRef[0].isBlank() && !canonStations.contains(stationRef[0])) {
                        plugin.getLogger().severe("Refining recipe " + currentIdRef[0] + " references unknown stationId: " + stationRef[0]);
                        continue;
                    }
                    // Ingredients must exist
                    boolean ok = true;
                    for (String iid : ing.keySet()) {
                        if (!canonItems.contains(iid)) { ok = false; plugin.getLogger().severe("Refining recipe " + currentIdRef[0] + " unknown ingredient itemId: " + iid); }
                    }
                    if (!canonItems.contains(o.itemId())) { ok = false; plugin.getLogger().severe("Refining recipe " + currentIdRef[0] + " unknown output itemId: " + o.itemId()); }
                    if (!ok) continue;

                    String rid = currentIdRef[0] + "::" + o.itemId();
                    String disp = "Refinado: " + o.itemId();
                    Material mat = ItemFactory.guessMaterialStatic(o.itemId());
                    out.add(new CraftRecipe(
                            rid,
                            disp,
                            mat,
                            o.itemId(),
                            o.itemId(),
                            mat,
                            o.amount(),
                            ing,
                            professionRef[0],
                            1,
                            0,
                            moneyCostRef[0]
                    ));
                }
            };

            for (int i = 0; i < lines.size(); i++) {
                String raw = lines.get(i);
                String line = raw.trim();
                if (line.isEmpty()) continue;

                if (SEP.matcher(line).matches()) {
                    // next line should be recipe id
                    // flush previous
                    flush.run();
                    // reset
                    currentIdRef[0] = null; stationRef[0] = null; professionRef[0] = null; moneyCostRef[0] = 0; timeSecRef[0] = 0;
                    inputs.clear(); outputs.clear(); inInputRef[0] = false; inOutputRef[0] = false;
                    // try read next non-empty line as id
                    for (int j = i+1; j < lines.size(); j++) {
                        String l2 = lines.get(j).trim();
                        if (l2.isEmpty()) continue;
                        if (l2.startsWith("REC_")) { currentIdRef[0] = l2; }
                        break;
                    }
                    continue;
                }

                if (line.toLowerCase(Locale.ROOT).startsWith("estación:")) {
                    stationRef[0] = line.substring(line.indexOf(':')+1).trim();
                    continue;
                }
                if (line.toLowerCase(Locale.ROOT).startsWith("profesión:")) {
                    professionRef[0] = line.substring(line.indexOf(':')+1).trim();
                    continue;
                }
                if (line.toLowerCase(Locale.ROOT).startsWith("input:")) {
                    inInputRef[0] = true; inOutputRef[0] = false; continue;
                }
                if (line.toLowerCase(Locale.ROOT).startsWith("output:")) {
                    inOutputRef[0] = true; inInputRef[0] = false; continue;
                }
                Matcher mc = COST.matcher(line);
                if (mc.find()) {
                    moneyCostRef[0] = Long.parseLong(mc.group(1));
                    continue;
                }
                Matcher mt = TIME.matcher(line);
                if (mt.find()) {
                    timeSecRef[0] = Integer.parseInt(mt.group(1));
                    continue;
                }

                if (line.startsWith("-")) {
                    String itemLine = line.substring(1).trim();
                    if (inInputRef[0]) inputs.add(itemLine);
                    else if (inOutputRef[0]) outputs.add(itemLine);
                }
            }
            flush.run();
            return out;
        }

        static List<CraftRecipe> parseStructuredCrafting(String txt, Set<String> canonItems, Set<String> canonStations, JavaPlugin plugin) {
            List<CraftRecipe> out = new ArrayList<>();
            List<String> lines = Arrays.asList(txt.split("\r?\n"));

            String[] sectionRef = {null};
            String[] stationRef = {null};
            String[] professionRef = {null};
            long[] moneyCostRef = {0};

            List<String> outputs = new ArrayList<>();
            List<String> inputs = new ArrayList<>();
            boolean inOutputs = false;
            boolean inInputs = false;

            Runnable flush = () -> {
                if (stationRef[0] == null || stationRef[0].isBlank()) return;
                if (outputs.isEmpty() || inputs.isEmpty()) return;

                List<ExpandedLine> outExp = expandItemLines(outputs, canonItems);
                List<ExpandedLine> inExp = expandItemLines(inputs, canonItems);

                for (ExpandedLine o : outExp) {
                    Map<String,Integer> ing = new LinkedHashMap<>();
                    for (ExpandedLine ii : inExp) {
                        ing.merge(ii.itemId(), ii.amount(), Integer::sum);
                    }

                    boolean ok = true;
                    if (!canonStations.isEmpty() && !canonStations.contains(stationRef[0])) {
                        plugin.getLogger().severe("Crafting block references unknown stationId: " + stationRef[0] + " (section=" + sectionRef[0] + ")");
                        ok = false;
                    }
                    for (String iid : ing.keySet()) {
                        if (!canonItems.contains(iid)) { ok = false; plugin.getLogger().severe("Crafting recipe unknown ingredient itemId: " + iid + " (section=" + sectionRef[0] + ")"); }
                    }
                    if (!canonItems.contains(o.itemId())) { ok = false; plugin.getLogger().severe("Crafting recipe unknown output itemId: " + o.itemId() + " (section=" + sectionRef[0] + ")"); }
                    if (!ok) continue;

                    String rid = "CRAFT::" + (sectionRef[0] == null ? "SECTION" : sectionRef[0]) + "::" + o.itemId();
                    String disp = "Craftear: " + o.itemId();
                    Material mat = ItemFactory.guessMaterialStatic(o.itemId());
                    out.add(new CraftRecipe(
                            rid,
                            disp,
                            mat,
                            o.itemId(),
                            o.itemId(),
                            mat,
                            o.amount(),
                            ing,
                            professionRef[0],
                            1,
                            0,
                            moneyCostRef[0]
                    ));
                }
            };

            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty()) continue;

                if (SEP.matcher(line).matches()) {
                    flush.run();
                    // reset for next block
                    sectionRef[0] = null; stationRef[0] = null; professionRef[0] = null; moneyCostRef[0] = 0;
                    outputs.clear(); inputs.clear(); inOutputs = false; inInputs = false;
                    continue;
                }

                // Headers like "6.1 COMPONENTES DE MADERA"
                if (line.matches("^\\d+\\.\\d+\\s+.*$")) {
                    sectionRef[0] = line;
                    continue;
                }
                if (line.toLowerCase(Locale.ROOT).startsWith("estación:")) {
                    stationRef[0] = line.substring(line.indexOf(':')+1).trim();
                    continue;
                }
                if (line.toLowerCase(Locale.ROOT).startsWith("oficio:")) {
                    professionRef[0] = line.substring(line.indexOf(':')+1).trim();
                    continue;
                }
                Matcher mc = COST.matcher(line);
                if (mc.find()) { moneyCostRef[0] = Long.parseLong(mc.group(1)); continue; }

                if (line.toLowerCase(Locale.ROOT).startsWith("outputs:")) { inOutputs = true; inInputs = false; continue; }
                if (line.toLowerCase(Locale.ROOT).startsWith("inputs:")) { inInputs = true; inOutputs = false; 
                    // capture inline inputs after "Inputs:"
                    String rest = line.substring(line.indexOf(':')+1).trim();
                    if (!rest.isBlank()) inputs.add(rest);
                    continue; }

                if (line.startsWith("-")) {
                    String itemLine = line.substring(1).trim();
                    if (inOutputs) outputs.add(itemLine);
                    else if (inInputs) inputs.add(itemLine);
                }
            }
            flush.run();
            return out;
        }

        // Represents a concrete expanded line (no placeholders), with amount.
        private record ExpandedLine(String itemId, int amount) {}

        private static List<ExpandedLine> expandItemLines(List<String> lines, Set<String> canonItems) {
            List<ExpandedLine> out = new ArrayList<>();
            if (lines == null) return out;

            // derive placeholder vocab from canon items
            Set<String> metals = extractMiddle(canonItems, "IT_RES_METAL_ORE_", "_T");
            Set<String> woods = extractMiddle(canonItems, "IT_RES_WOOD_LOG_", "_T");
            Set<String> rocks = extractMiddle(canonItems, "IT_RES_ROCK_", "_T");
            Set<String> gems = extractMiddle(canonItems, "IT_RES_GEM_", "_T");
            Set<String> crystals = extractMiddle(canonItems, "IT_RES_CRYSTAL_", "_T");

            for (String raw : lines) {
                if (raw == null) continue;
                String line = raw.trim();
                if (line.isEmpty()) continue;

                // parse "IT_... xN" suffix
                int amount = 1;
                Matcher mx = Pattern.compile("\\bx(\\d+)\\b", Pattern.CASE_INSENSITIVE).matcher(line);
                if (mx.find()) {
                    amount = Integer.parseInt(mx.group(1));
                    line = line.substring(0, mx.start()).trim();
                }

                // Expand tiers T#
                if (line.contains("T#")) {
                    for (int t = 1; t <= 8; t++) {
                        String tiered = line.replace("T#", "T" + t);
                        out.addAll(expandPlaceholders(tiered, amount, canonItems, metals, woods, rocks, gems, crystals));
                    }
                } else {
                    out.addAll(expandPlaceholders(line, amount, canonItems, metals, woods, rocks, gems, crystals));
                }
            }

            // filter only canon items, dedupe
            Map<String,Integer> best = new LinkedHashMap<>();
            for (ExpandedLine e : out) {
                if (e.itemId() == null) continue;
                if (!canonItems.contains(e.itemId())) continue;
                best.put(e.itemId(), e.amount());
            }
            List<ExpandedLine> finalOut = new ArrayList<>();
            for (var e : best.entrySet()) finalOut.add(new ExpandedLine(e.getKey(), e.getValue()));
            return finalOut;
        }

        private static List<ExpandedLine> expandPlaceholders(String itemId, int amount, Set<String> canonItems,
                                                            Set<String> metals, Set<String> woods, Set<String> rocks,
                                                            Set<String> gems, Set<String> crystals) {
            List<ExpandedLine> out = new ArrayList<>();
            String base = itemId.trim();

            if (base.contains("<METAL>")) {
                for (String v : metals) out.addAll(expandPlaceholders(base.replace("<METAL>", v), amount, canonItems, Set.of(), woods, rocks, gems, crystals));
                return out;
            }
            if (base.contains("<ESPECIE>")) {
                for (String v : woods) out.addAll(expandPlaceholders(base.replace("<ESPECIE>", v), amount, canonItems, metals, Set.of(), rocks, gems, crystals));
                return out;
            }
            if (base.contains("<TIPO>")) {
                // Try multiple vocabularies (rock/gem/crystal). We only keep those that exist in canon.
                for (String v : rocks) out.add(new ExpandedLine(base.replace("<TIPO>", v), amount));
                for (String v : gems) out.add(new ExpandedLine(base.replace("<TIPO>", v), amount));
                for (String v : crystals) out.add(new ExpandedLine(base.replace("<TIPO>", v), amount));
                return out;
            }

            out.add(new ExpandedLine(base, amount));
            return out;
        }

        private static Set<String> extractMiddle(Set<String> ids, String prefix, String suffixContains) {
            Set<String> out = new TreeSet<>();
            if (ids == null) return out;
            for (String id : ids) {
                if (!id.startsWith(prefix)) continue;
                int suf = id.indexOf(suffixContains);
                if (suf <= prefix.length()) continue;
                String mid = id.substring(prefix.length(), suf);
                if (!mid.isBlank()) out.add(mid);
            }
            return out;
        }
    }
}
