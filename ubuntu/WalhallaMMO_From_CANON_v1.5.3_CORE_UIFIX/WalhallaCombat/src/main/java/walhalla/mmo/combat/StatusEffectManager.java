package walhalla.mmo.combat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Minimal status effect runtime container.
 * Stores duration + stacks per entity. Advanced interactions are future phases.
 */
public class StatusEffectManager {

    public static record StatusState(String id, int stacks, long expiresAtTick) {}

    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, StatusState>> statuses = new ConcurrentHashMap<>();

    public StatusEffectManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Tick cleanup
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void apply(LivingEntity entity, String statusId, int durationTicks, int stacks) {
        if (entity == null) return;
        long now = Bukkit.getCurrentTick();
        long exp = now + Math.max(1, durationTicks);

        statuses.computeIfAbsent(entity.getUniqueId(), k -> new ConcurrentHashMap<>())
                .merge(statusId, new StatusState(statusId, stacks, exp),
                        (oldV, newV) -> new StatusState(statusId, Math.max(oldV.stacks(), stacks), Math.max(oldV.expiresAtTick(), exp)));
    }

    public boolean hasStatus(LivingEntity entity, String statusId) {
        if (entity == null) return false;
        Map<String, StatusState> m = statuses.get(entity.getUniqueId());
        if (m == null) return false;
        StatusState s = m.get(statusId);
        return s != null && s.expiresAtTick() > Bukkit.getCurrentTick();
    }

    public List<StatusState> getStatuses(LivingEntity entity) {
        if (entity == null) return List.of();
        Map<String, StatusState> m = statuses.get(entity.getUniqueId());
        if (m == null) return List.of();
        long now = Bukkit.getCurrentTick();
        List<StatusState> out = new ArrayList<>();
        for (StatusState s : m.values()) {
            if (s.expiresAtTick() > now) out.add(s);
        }
        return out;
    }

    private void tick() {
        long now = Bukkit.getCurrentTick();
        for (Iterator<Map.Entry<UUID, Map<String, StatusState>>> it = statuses.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Map<String, StatusState>> e = it.next();
            Map<String, StatusState> m = e.getValue();
            m.values().removeIf(s -> s.expiresAtTick() <= now);
            if (m.isEmpty()) it.remove();
        }
    }
}
