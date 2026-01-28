package walhalla.mmo.core.api.progress;

import java.util.UUID;

/**
 * Mutation bridge for trusted runtime systems (Combat/Spells/etc).
 * UI must remain read-only via CoreAPI.
 */
public interface ProgressMutationBridge {
    boolean addGlobalXp(UUID playerId, long amount);
    boolean addBranchXp(UUID playerId, String branchId, long amount);
    boolean addSubBranchXp(UUID playerId, String subBranchKey, long amount);
    boolean addAffinityXp(UUID playerId, String affinityId, long amount);
    boolean addProfessionXp(UUID playerId, String professionId, long amount);
}
