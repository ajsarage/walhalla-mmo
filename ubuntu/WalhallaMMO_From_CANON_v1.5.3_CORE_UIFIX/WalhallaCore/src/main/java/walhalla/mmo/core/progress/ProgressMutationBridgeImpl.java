package walhalla.mmo.core.progress;

import java.util.UUID;
import walhalla.mmo.core.api.progress.ProgressMutationBridge;

/**
 * Simple adapter exposing PlayerProgressService mutation methods to trusted plugins.
 */
public class ProgressMutationBridgeImpl implements ProgressMutationBridge {

    private final PlayerProgressService progress;

    public ProgressMutationBridgeImpl(PlayerProgressService progress) {
        this.progress = progress;
    }

    @Override public boolean addGlobalXp(UUID playerId, long amount) { return progress.addGlobalXp(playerId, amount); }
    @Override public boolean addBranchXp(UUID playerId, String branchId, long amount) { return progress.addBranchXp(playerId, branchId, amount); }
    @Override public boolean addSubBranchXp(UUID playerId, String subBranchKey, long amount) { return progress.addSubBranchXp(playerId, subBranchKey, amount); }
    @Override public boolean addAffinityXp(UUID playerId, String affinityId, long amount) { return progress.addAffinityXp(playerId, affinityId, amount); }
    @Override public boolean addProfessionXp(UUID playerId, String professionId, long amount) { return progress.addProfessionXp(playerId, professionId, amount); }
}
