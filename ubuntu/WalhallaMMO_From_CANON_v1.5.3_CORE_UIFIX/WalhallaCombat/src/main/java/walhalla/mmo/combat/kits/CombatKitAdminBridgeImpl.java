package walhalla.mmo.combat.kits;

import java.util.Optional;
import java.util.UUID;

import walhalla.mmo.core.api.combat.CombatAdminBridge;

public final class CombatKitAdminBridgeImpl implements CombatAdminBridge {

    private final CombatKitRuntime runtime;

    public CombatKitAdminBridgeImpl(CombatKitRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public boolean reloadKits() {
        runtime.loadMappingsFromConfig();
        return runtime.reloadFromCanon();
    }

    @Override
    public String getStatus() {
        return runtime.statusLine();
    }

    @Override
    public boolean setPlayerKit(UUID playerId, String kitId) {
        return runtime.setPlayerKit(playerId, kitId);
    }

    @Override
    public Optional<String> getPlayerKit(UUID playerId) {
        return runtime.getPlayerKitId(playerId);
    }
}
