package walhalla.mmo.bosses.endgame;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Optional world bosses spawner (data-driven).
 * No player commands are required. OP can manage via config reload on plugin reload.
 */
public final class WorldBossSpawner {

    private final JavaPlugin plugin;
    private final EndgameConfig cfg;
    private BukkitTask task;

    // Boss id -> entity uuid
    private final Map<String, UUID> alive = new HashMap<>();
    private final Map<String, Long> nextSpawnAtMs = new HashMap<>();

    public WorldBossSpawner(JavaPlugin plugin, EndgameConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    public void start() {
        if (!cfg.worldBossesEnabled) return;
        if (cfg.worldBosses == null || cfg.worldBosses.isEmpty()) {
            plugin.getLogger().warning("WorldBosses enabled but list is empty. No bosses will spawn.");
            return;
        }

        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L * 30L); // every 30s
    }

    private void tick() {
        long now = System.currentTimeMillis();

        for (WorldBossSpec spec : cfg.worldBosses) {
            if (spec == null) continue;

            UUID uuid = alive.get(spec.id());
            if (uuid != null) {
                Entity ent = Bukkit.getEntity(uuid);
                if (ent instanceof LivingEntity le && !le.isDead()) {
                    continue; // still alive
                } else {
                    alive.remove(spec.id());
                    nextSpawnAtMs.put(spec.id(), now + spec.respawnMinutes() * 60_000L);
                }
            }

            long next = nextSpawnAtMs.getOrDefault(spec.id(), 0L);
            if (now < next) continue;

            World w = Bukkit.getWorld(spec.worldName());
            if (w == null) continue;

            LivingEntity boss = (LivingEntity) w.spawnEntity(spec.toLocation(w), spec.entityType());
            boss.addScoreboardTag("WH_WORLD_BOSS");
            boss.setCustomNameVisible(true);
            boss.setCustomName("§5WORLD BOSS §7" + spec.id());

            alive.put(spec.id(), boss.getUniqueId());
            nextSpawnAtMs.put(spec.id(), now + spec.respawnMinutes() * 60_000L);

            if (spec.announce()) {
                Bukkit.broadcastMessage("§5[Walhalla] §dWorld Boss spawned: §f" + spec.id());
            }
        }
    }
}
