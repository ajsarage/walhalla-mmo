package walhalla.mmo.combat.kits;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import walhalla.mmo.combat.CombatService;
import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.canon.CanonFile;

/**
 * Phase 9: Non-mage combat kits (data-driven).
 *
 * NOTE: Canon annexes use qualitative labels for coste/cooldown (Bajo/Medio/Alto/etc).
 * Numeric resolution is provided via config mappings in WalhallaCombat/config.yml, which is tunable.
 */
public final class CombatKitRuntime implements Listener {

    private final JavaPlugin plugin;
    private final CombatService combat;
    private final PlayerKitStore store;

    private final Map<String, CombatKit> kitsById = new HashMap<>();
    private final Map<String, List<CombatKit>> kitsByBranch = new HashMap<>();

    private final Map<String, Integer> cooldownTicks = new HashMap<>();
    private final Map<String, Integer> costUnits = new HashMap<>();

    private final Map<UUID, Map<String, Long>> cooldownUntil = new ConcurrentHashMap<>(); // player -> abilityKey -> epochMs

    public CombatKitRuntime(JavaPlugin plugin, CombatService combat) {
        this.plugin = plugin;
        this.combat = combat;
        this.store = new PlayerKitStore(new File(plugin.getDataFolder(), "kits.yml"));
    }

    public void loadMappingsFromConfig() {
        cooldownTicks.clear();
        costUnits.clear();

        var cfg = plugin.getConfig();
        var cdSec = cfg.getConfigurationSection("kitCooldownTicks");
        if (cdSec != null) {
            for (String k : cdSec.getKeys(false)) cooldownTicks.put(norm(k), cdSec.getInt(k));
        }
        var costSec = cfg.getConfigurationSection("kitCostUnits");
        if (costSec != null) {
            for (String k : costSec.getKeys(false)) costUnits.put(norm(k), costSec.getInt(k));
        }

        // Minimal safety defaults if config is missing.
        cooldownTicks.putIfAbsent("MUY_BAJO", 10);
        cooldownTicks.putIfAbsent("BAJO", 30);
        cooldownTicks.putIfAbsent("MEDIO", 60);
        cooldownTicks.putIfAbsent("ALTO", 120);
        cooldownTicks.putIfAbsent("MUY_ALTO", 200);

        costUnits.putIfAbsent("MUY_BAJO", 2);
        costUnits.putIfAbsent("BAJO", 5);
        costUnits.putIfAbsent("MEDIO", 10);
        costUnits.putIfAbsent("ALTO", 20);
        costUnits.putIfAbsent("MUY_ALTO", 30);
    }

    public boolean reloadFromCanon() {
        kitsById.clear();
        kitsByBranch.clear();

        var canonOpt = CoreAPI.getCanonService();
        if (canonOpt.isEmpty()) {
            plugin.getLogger().severe("Canon service not available in CoreAPI.");
            return false;
        }
        var canon = canonOpt.get();

        String w = canon.getRaw(CanonFile.ANNEX_COMBAT_KITS_WARRIOR).orElse(null);
        String t = canon.getRaw(CanonFile.ANNEX_COMBAT_KITS_TANK).orElse(null);
        String h = canon.getRaw(CanonFile.ANNEX_COMBAT_KITS_HUNTER).orElse(null);

        ingest(CanonKitParser.parse("GUERRERO", w));
        ingest(CanonKitParser.parse("TANQUE", t));
        ingest(CanonKitParser.parse("CAZADOR", h));

        try {
            store.load();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load kits.yml: " + e.getMessage());
        }

        plugin.getLogger().info("Loaded combat kits: " + kitsById.size());
        return !kitsById.isEmpty();
    }

    private void ingest(KitParseResult res) {
        for (String warn : res.warnings()) plugin.getLogger().warning("[KitParse] " + warn);
        for (CombatKit k : res.kits()) {
            kitsById.put(k.kitId().toUpperCase(Locale.ROOT), k);
            kitsByBranch.computeIfAbsent(k.branchId().toUpperCase(Locale.ROOT), __ -> new ArrayList<>()).add(k);
        }
    }

    public String statusLine() {
        return "Kits=" + kitsById.size() + " (GUERRERO=" + kitsByBranch.getOrDefault("GUERRERO", List.of()).size()
                + ", TANQUE=" + kitsByBranch.getOrDefault("TANQUE", List.of()).size()
                + ", CAZADOR=" + kitsByBranch.getOrDefault("CAZADOR", List.of()).size() + ")";
    }

    public Set<String> kitIds() { return Collections.unmodifiableSet(kitsById.keySet()); }

    public Optional<String> getPlayerKitId(UUID playerId) {
        return store.getKit(playerId);
    }

    public boolean setPlayerKit(UUID playerId, String kitId) {
        if (kitId == null) return false;
        CombatKit k = kitsById.get(kitId.toUpperCase(Locale.ROOT));
        if (k == null) return false;
        store.setKit(playerId, k.kitId());
        try { store.save(); } catch (IOException ignored) {}
        return true;
    }

    private CombatKit resolveKit(Player p) {
        String branch = combat.inferCombatBranch(p);
        Optional<String> forced = store.getKit(p.getUniqueId());
        if (forced.isPresent()) {
            CombatKit k = kitsById.get(forced.get().toUpperCase(Locale.ROOT));
            if (k != null) return k;
        }
        List<CombatKit> list = kitsByBranch.getOrDefault(branch.toUpperCase(Locale.ROOT), List.of());
        if (!list.isEmpty()) return list.get(0);
        // fallback: any kit
        return kitsById.values().stream().findFirst().orElse(null);
    }

    // --------------------------------------
    // Input handling (no commands for players)
    // --------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        // Right click triggers secondary (non-mage only)
        switch (e.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {}
            default -> { return; }
        }
        if (combat.inferCombatBranch(p).equalsIgnoreCase("MAGO")) return;
        CombatKit kit = resolveKit(p);
        if (kit == null) return;

        e.setCancelled(true);
        castAbility(p, kit, "SECONDARY");
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (combat.inferCombatBranch(p).equalsIgnoreCase("MAGO")) return;

        CombatKit kit = resolveKit(p);
        if (kit == null) return;

        e.setCancelled(true);
        castAbility(p, kit, "SPECIAL");
    }

    private void castAbility(Player p, CombatKit kit, String which) {
        if (!CoreAPI.getPlayerProgress(p.getUniqueId()).isPresent()) return;
        if (CoreAPI.getLifecycleState(p.getUniqueId()) != walhalla.mmo.core.progress.PlayerData.PlayerLifecycleState.ACTIVE) return;

        Ability a = switch (which) {
            case "SECONDARY" -> kit.secondary();
            case "SPECIAL" -> kit.special();
            default -> kit.basic();
        };
        if (a == null) return;

        String key = kit.kitId() + ":" + which;
        if (isOnCooldown(p.getUniqueId(), key)) {
            p.sendActionBar(ChatColor.RED + "En cooldown.");
            return;
        }

        // Cost system is placeholder-units; real vigor resource will be added later.
        // For now, cost only acts as a soft feedback (no resource bar yet).
        int cdTicks = resolveCooldownTicks(a.cooldownLevel());
        setCooldown(p.getUniqueId(), key, cdTicks);

        boolean ok = executeTemplate(p, kit, which, a);
        if (!ok) {
            // If failed (e.g. no target), remove cooldown as courtesy.
            cooldownUntil.getOrDefault(p.getUniqueId(), Map.of()).remove(key);
        }
    }

    private boolean executeTemplate(Player p, CombatKit kit, String which, Ability a) {
        // Target in front
        LivingEntity target = findTarget(p, 5.0);
        String name = norm(stripAccents(a.name()));
        String kitId = kit.kitId();

        // Special-case patterns
        if (which.equals("SECONDARY")) {
            if (name.contains("EMBESTIDA") || name.contains("CARGA")) {
                dash(p, 1.35);
                aoeDamage(p, 2.2, baseAttack(p) * 0.8, "KIT_" + kitId + "_DASH");
                return true;
            }
            if (name.contains("QUEBRANTO") || name.contains("ROMPE") || name.contains("ESCUDO")) {
                if (target == null) { p.sendActionBar(ChatColor.RED + "Sin objetivo."); return false; }
                combat.applyDamage(p, target, baseAttack(p) * 1.2, walhalla.mmo.core.api.combat.DamageType.PHYSICAL, "KIT_" + kitId + "_BREAK");
                combat.applyStatus(p, target, "ARMOR_BREAK", 100, 1, "KIT_" + kitId);
                return true;
            }
            // Default: direct hit
            if (target == null) { p.sendActionBar(ChatColor.RED + "Sin objetivo."); return false; }
            combat.applyDamage(p, target, baseAttack(p) * 1.1, walhalla.mmo.core.api.combat.DamageType.PHYSICAL, "KIT_" + kitId + "_SEC");
            return true;
        }

        if (which.equals("SPECIAL")) {
            if (name.contains("MURALLA") || name.contains("BASTION") || name.contains("POSTURA")) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1, true, true, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 0, true, true, true));
                p.sendActionBar(ChatColor.GOLD + "Postura defensiva.");
                return true;
            }
            if (name.contains("EJECUCION")) {
                if (target == null) { p.sendActionBar(ChatColor.RED + "Sin objetivo."); return false; }
                double hp = target.getHealth();
                double max = Optional.ofNullable(target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH))
                        .map(a0 -> a0.getValue()).orElse(20.0);
                if (hp > max * 0.35) {
                    p.sendActionBar(ChatColor.RED + "Objetivo demasiado sano.");
                    return false;
                }
                combat.applyDamage(p, target, baseAttack(p) * 3.0, walhalla.mmo.core.api.combat.DamageType.PHYSICAL, "KIT_" + kitId + "_EXEC");
                return true;
            }
            // Default: self-buff + small aoe
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 0, true, true, true));
            aoeDamage(p, 2.6, baseAttack(p) * 0.6, "KIT_" + kitId + "_SPEC_AOE");
            return true;
        }

        return false;
    }

    private LivingEntity findTarget(Player p, double range) {
        var res = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), range,
                0.6, ent -> ent instanceof LivingEntity && ent != p);
        if (res == null || res.getHitEntity() == null) return null;
        return (LivingEntity) res.getHitEntity();
    }

    private void dash(Player p, double mult) {
        Vector dir = p.getLocation().getDirection().normalize().multiply(mult);
        dir.setY(Math.max(0.15, dir.getY()));
        p.setVelocity(dir);
    }

    private void aoeDamage(Player p, double radius, double dmg, String source) {
        for (var ent : p.getNearbyEntities(radius, radius, radius)) {
            if (ent instanceof LivingEntity le && le != p) {
                combat.applyDamage(p, le, dmg, walhalla.mmo.core.api.combat.DamageType.PHYSICAL, source);
            }
        }
    }

    private double baseAttack(Player p) {
        var attr = p.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
        return attr != null ? Math.max(1.0, attr.getValue()) : 2.0;
    }

    private int resolveCooldownTicks(String label) {
        String k = norm(label);
        return cooldownTicks.getOrDefault(k, 60);
    }

    private boolean isOnCooldown(UUID playerId, String key) {
        long now = System.currentTimeMillis();
        Map<String, Long> m = cooldownUntil.get(playerId);
        if (m == null) return false;
        Long until = m.get(key);
        return until != null && until > now;
    }

    private void setCooldown(UUID playerId, String key, int ticks) {
        long ms = (ticks * 50L);
        cooldownUntil.computeIfAbsent(playerId, __ -> new ConcurrentHashMap<>()).put(key, System.currentTimeMillis() + ms);
    }

    private static String stripAccents(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private static String norm(String s) {
        String x = stripAccents(s).toUpperCase(Locale.ROOT).trim();
        x = x.replaceAll("\\s+", "_");
        x = x.replaceAll("[^A-Z0-9_]+", "_");
        x = x.replaceAll("^_+|_+$", "");
        return x;
    }
}
