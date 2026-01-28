package walhalla.mmo.combat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import walhalla.mmo.core.api.combat.CombatBridge;
import walhalla.mmo.core.api.progress.ProgressMutationBridge;

/**
 * Phase 3: Combat runtime authority.
 */
public class WalhallaCombatPlugin extends JavaPlugin {

    private CombatConfig cfg;
    private CombatFeedback feedback;
    private CombatService combat;
    private RespawnProtectionService respawnProtection;
    private walhalla.mmo.combat.kits.CombatKitRuntime kits;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.cfg = new CombatConfig(getConfig());
        this.feedback = new CombatFeedback(this);

        ProgressMutationBridge progressMut = Bukkit.getServicesManager().load(ProgressMutationBridge.class);
        if (progressMut == null) {
            getLogger().severe("ProgressMutationBridge not found. Is WalhallaCore enabled?");
        }

        this.respawnProtection = new RespawnProtectionService();
        this.combat = new CombatService(this, progressMut, respawnProtection);

        // Register CombatBridge for other plugins (Spells/Weapons)
        Bukkit.getServicesManager().register(CombatBridge.class, combat, this, ServicePriority.Normal);

        // Basic attacks
        Bukkit.getPluginManager().registerEvents(new BasicAttackListener(combat, respawnProtection), this);

        // Phase 13: death/respawn baseline (spawn protection)
        Bukkit.getPluginManager().registerEvents(new DeathRespawnListener(this, respawnProtection), this);

        // Phase 9: combat kits (non-mage)
        this.kits = new walhalla.mmo.combat.kits.CombatKitRuntime(this, combat);
        this.kits.loadMappingsFromConfig();
        this.kits.reloadFromCanon();
        Bukkit.getPluginManager().registerEvents(kits, this);

        // Register admin bridge for /walhalla combat ...
        Bukkit.getServicesManager().register(walhalla.mmo.core.api.combat.CombatAdminBridge.class, new walhalla.mmo.combat.kits.CombatKitAdminBridgeImpl(kits), this, ServicePriority.Normal);

        getLogger().info("WalhallaCombat enabled (Phase 3).");
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        getLogger().info("WalhallaCombat disabled.");
    }

    public CombatConfig cfg() { return cfg; }
    public CombatFeedback feedback() { return feedback; }

    public RespawnProtectionService respawnProtection() { return respawnProtection; }

    public boolean isPlayerActive(java.util.UUID playerId) {
        // Use CoreAPI's progress presence + internal state; mutation bridge also enforces.
        return walhalla.mmo.core.api.CoreAPI.getPlayerProgress(playerId).isPresent()
                && Bukkit.getPlayer(playerId) != null
                && Bukkit.getPlayer(playerId).isOnline()
                && Bukkit.getServicesManager().load(ProgressMutationBridge.class) != null;
    }
}
