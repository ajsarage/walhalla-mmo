package walhalla.mmo.professions;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.professions.ProfessionsUiBridge;
import walhalla.mmo.core.api.progress.ProgressMutationBridge;

public class WalhallaProfessionsPlugin extends JavaPlugin {

    private GatheringRegistry gathering;
    private RecipeRegistry recipes;
    private ProfessionsMenus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ProgressMutationBridge mut = Bukkit.getServicesManager().load(ProgressMutationBridge.class);
        if (mut == null) {
            getLogger().severe("ProgressMutationBridge not found. Is WalhallaCore enabled?");
        }

        this.gathering = new GatheringRegistry(this);
        this.recipes = new RecipeRegistry(this);
        this.menus = new ProfessionsMenus(this, gathering, recipes, mut);

        Bukkit.getPluginManager().registerEvents(new GatheringListener(this, gathering, mut), this);
        Bukkit.getPluginManager().registerEvents(menus, this);

        Bukkit.getServicesManager().register(ProfessionsUiBridge.class, menus, this, ServicePriority.Normal);

        getLogger().info("WalhallaProfessions enabled (Phase 4). Nodes=" + gathering.size() + ", recipes=" + recipes.size());
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        getLogger().info("WalhallaProfessions disabled.");
    }
}
