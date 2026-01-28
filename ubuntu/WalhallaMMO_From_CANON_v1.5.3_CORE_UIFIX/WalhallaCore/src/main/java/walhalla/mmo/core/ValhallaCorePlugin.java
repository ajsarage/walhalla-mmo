package walhalla.mmo.core;

import java.io.File;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.admin.WalhallaAdminCommand;
import walhalla.mmo.core.api.CoreAPI;
import walhalla.mmo.core.api.spells.SpellCatalogBridge;
import walhalla.mmo.core.api.economy.EconomyBridge;
import walhalla.mmo.core.economy.CoreEconomyService;
import walhalla.mmo.core.economy.EconomyPriceEngine;
import walhalla.mmo.core.canon.CanonDataService;
import walhalla.mmo.core.onboarding.BranchSelectionMenu;
import walhalla.mmo.core.onboarding.GameplayGateListener;
import walhalla.mmo.core.onboarding.OnboardingJoinListener;
import walhalla.mmo.core.progress.PlayerProgressService;
import walhalla.mmo.core.progress.PlayerProgressStore;

public class ValhallaCorePlugin extends JavaPlugin {

    private PlayerProgressStore store;
    private PlayerProgressService progress;
    private CanonDataService canon;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureCanonicalDataFolders();

        // Bootstrap CoreAPI first
        CoreAPI.bootstrap(this);

        this.canon = new CanonDataService(this);
        this.canon.loadOrFail();

        this.store = new PlayerProgressStore(this);
        this.progress = new PlayerProgressService(this, store);

        // CoreAPI needs access to services.
        CoreAPI.setCanonService(canon);

        // Phase 12: Economy prices + balance authority
        EconomyPriceEngine prices = new EconomyPriceEngine(canon);
        CoreAPI.setEconomyPriceEngine(prices);
        Bukkit.getServicesManager().register(EconomyBridge.class, new CoreEconomyService(this, progress), this, ServicePriority.Normal);

        progress.applyCanonCaps(canon.xpCaps());

        // Trusted mutation bridge for runtime systems (Combat/Spells). UI must stay read-only via CoreAPI.
        Bukkit.getServicesManager().register(walhalla.mmo.core.api.progress.ProgressMutationBridge.class,
                new walhalla.mmo.core.progress.ProgressMutationBridgeImpl(progress), this, ServicePriority.Normal);

        // Onboarding UI + gating (players never use commands)
        BranchSelectionMenu onboardingMenu = new BranchSelectionMenu(this, progress);
        Bukkit.getPluginManager().registerEvents(onboardingMenu, this);
        Bukkit.getPluginManager().registerEvents(new OnboardingJoinListener(this, progress, onboardingMenu), this);
        Bukkit.getPluginManager().registerEvents(new GameplayGateListener(progress), this);

        // Register listener for quit -> save (handled by progress service)
        Bukkit.getPluginManager().registerEvents(progress, this);

        // Periodic autosave
        long autosaveTicks = Math.max(20L, getConfig().getLong("autosaveTicks", 20L * 60L));
        Bukkit.getScheduler().runTaskTimer(this, () -> progress.saveAllOnline(), autosaveTicks, autosaveTicks);

        // Admin command
        PluginCommand cmd = getCommand("walhalla");
        if (cmd != null) {
            WalhallaAdminCommand exec = new WalhallaAdminCommand(progress);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }

        getLogger().info("WalhallaCore enabled (Phase 7: data-driven core).");
    }

    @Override
    public void onDisable() {
        if (progress != null) {
            progress.saveAllOnline();
        }
        getLogger().info("WalhallaCore disabled.");
    }

    /**
     * Creates canonical folders used across the project.
     */
    private void ensureCanonicalDataFolders() {
        File root = getDataFolder();
        if (!root.exists()) root.mkdirs();

        // persistence
        new File(root, "players").mkdirs();

        // canon data (extracted from jar on first run)
        new File(root, "canon").mkdirs();

        // placeholder canonical trees folder
        File trees = new File(root, "trees");
        trees.mkdirs();
        // create top-level folders (no content is invented)
        new File(trees, "combat").mkdirs();
        new File(trees, "professions").mkdirs();
    }
}
