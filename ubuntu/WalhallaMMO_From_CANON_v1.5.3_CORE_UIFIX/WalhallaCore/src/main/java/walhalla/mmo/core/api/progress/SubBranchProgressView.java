package walhalla.mmo.core.api.progress;

/**
 * CONTRACT_COREAPI_PROGRESS_READONLY_v1 — SubBranchProgressView.
 * Canon status: NOT_IMPLEMENTED.
 */
public record SubBranchProgressView(
        String subBranchId,
        String state
) {
    public static SubBranchProgressView notImplemented(String subBranchId) {
        return new SubBranchProgressView(subBranchId, "NOT_IMPLEMENTED");
    }
}
