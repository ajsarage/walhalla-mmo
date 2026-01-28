package walhalla.mmo.core.onboarding;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.progress.PlayerData;
import walhalla.mmo.core.progress.PlayerProgressService;

/**
 * Ensures lifecycle is correct on join and opens the onboarding UI when needed.
 */
public class OnboardingJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerProgressService progress;
    private final BranchSelectionMenu menu;

    public OnboardingJoinListener(JavaPlugin plugin, PlayerProgressService progress, BranchSelectionMenu menu) {
        this.plugin = plugin;
        this.progress = progress;
        this.menu = menu;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData data = progress.getOrLoad(p.getUniqueId());

        // Canon: first time => choosing branches
        if (data.getLifecycleState() == PlayerData.PlayerLifecycleState.UNINITIALIZED) {
            data.setLifecycleState(PlayerData.PlayerLifecycleState.CHOOSING_BRANCHES);
            progress.saveNow(data);
        }

        if (data.getLifecycleState() == PlayerData.PlayerLifecycleState.CHOOSING_BRANCHES) {
            // Delay a tick to avoid inventory conflicts during join
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;
                p.sendTitle(ChatColor.GOLD + "Bienvenido", ChatColor.GRAY + "Elige 4 ramas para empezar.", 10, 60, 10);
                menu.open(p);
            }, 20L);
        }
    }
}
