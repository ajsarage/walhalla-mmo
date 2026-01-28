package walhalla.mmo.core.canon;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Loads canonical data annexes (TXT) and exposes them to the rest of the runtime.
 *
 * Phase 7 rules:
 * - Core is data-driven: if required annexes are missing or unreadable => HARD FAIL (server must not start).
 * - Core never invents tables; it only consumes canon data.
 */
public final class CanonDataService {

    public static final String CANON_FOLDER = "canon";

    private final JavaPlugin plugin;
    private final File canonDir;

    private final Map<CanonFile, String> rawTexts = new EnumMap<>(CanonFile.class);
    private final Map<CanonFile, String> sha256 = new EnumMap<>(CanonFile.class);
    private CanonTextIndex index = new CanonTextIndex(Set.of(), Set.of());
    private CanonXpCaps xpCaps = CanonXpCaps.defaults();

    public CanonDataService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.canonDir = new File(plugin.getDataFolder(), CANON_FOLDER);
    }

    public void ensureExtracted() throws IOException {
        if (!canonDir.exists() && !canonDir.mkdirs()) {
            throw new IOException("Failed to create canon dir: " + canonDir.getAbsolutePath());
        }

        for (CanonFile f : CanonFile.values()) {
            File out = new File(canonDir, f.fileName());
            if (out.exists()) continue;

            try (InputStream in = plugin.getResource(f.resourcePath())) {
                if (in == null) {
                    if (f.required()) {
                        throw new IOException("Missing required canon resource inside jar: " + f.resourcePath());
                    }
                    continue;
                }
                try (OutputStream os = new FileOutputStream(out)) {
                    in.transferTo(os);
                }
            }
        }
    }

    public void loadOrFail() {
        try {
            ensureExtracted();
            rawTexts.clear();
            sha256.clear();

            // Load required first
            for (CanonFile f : CanonFile.values()) {
                File file = new File(canonDir, f.fileName());
                if (!file.exists()) {
                    if (f.required()) throw new IOException("Required canon file missing on disk: " + file.getName());
                    continue;
                }
                String text = readUtf8(file);
                rawTexts.put(f, text);
                sha256.put(f, hashSha256(text));
            }

            // Build index from all loaded texts
            index = CanonTextIndex.buildFromAllText(rawTexts.values());

            // Parse caps (best-effort, but caps are binding when found)
            xpCaps = parseXpCaps(rawTexts.get(CanonFile.ANNEX_XP_CURVES)).orElse(CanonXpCaps.defaults());
        } catch (Exception ex) {
            plugin.getLogger().severe("CANON LOAD FAILED: " + ex.getMessage());
            throw new RuntimeException("Canon load failed", ex);
        }
    }

    public void reloadOrFail() {
        loadOrFail();
    }

    public File canonDir() { return canonDir; }

    public CanonTextIndex index() { return index; }

    public CanonXpCaps xpCaps() { return xpCaps; }

    public Optional<String> getRaw(CanonFile file) {
        return Optional.ofNullable(rawTexts.get(file));
    }

    public Map<CanonFile, String> sha256Map() {
        return Collections.unmodifiableMap(sha256);
    }

    public int loadedFileCount() {
        return rawTexts.size();
    }

    private static String readUtf8(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String hashSha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            return "sha256_error";
        }
    }

    private static Optional<CanonXpCaps> parseXpCaps(String xpText) {
        if (xpText == null || xpText.isBlank()) return Optional.empty();

        Integer global = findCap(xpText, "NIVEL MÁXIMO:\\s*-\\s*(\\d+)");
        Integer branch = findCap(xpText, "NIVEL MÁXIMO DE RAMA:\\s*-\\s*(\\d+)");
        Integer sub = findCap(xpText, "NIVEL MÁXIMO DE SUBRAMA:\\s*-\\s*(\\d+)");
        Integer affinity = findCap(xpText, "NIVEL MÁXIMO DE AFINIDAD:\\s*-\\s*(\\d+)");
        Integer prof = findCap(xpText, "NIVEL MÁXIMO DE PROFESIÓN:\\s*-\\s*(\\d+)");

        if (global == null && branch == null && sub == null && affinity == null && prof == null) return Optional.empty();

        return Optional.of(new CanonXpCaps(
                global != null ? global : 10_000,
                branch != null ? branch : 10_000,
                sub != null ? sub : 10_000,
                affinity != null ? affinity : 10_000,
                prof != null ? prof : 10_000
        ));
    }

    private static Integer findCap(String text, String regex) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return null;
    }
}
