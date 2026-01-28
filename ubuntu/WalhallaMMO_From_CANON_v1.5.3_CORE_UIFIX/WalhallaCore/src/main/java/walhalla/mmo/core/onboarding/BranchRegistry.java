package walhalla.mmo.core.onboarding;

import java.util.*;

/**
 * Canonical registries for Phase 1 onboarding.
 * No gameplay balance here, only IDs.
 */
public final class BranchRegistry {

    private BranchRegistry() {}

    // Combat branches (canon section 5)
    public static final String BR_GUERRERO = "GUERRERO";
    public static final String BR_TANQUE   = "TANQUE";
    public static final String BR_CAZADOR  = "CAZADOR";
    public static final String BR_MAGO     = "MAGO";

    // Professions (canon section 8)
    public static final String PR_MINERO         = "MINERO";
    public static final String PR_LENADOR        = "LEÑADOR";
    public static final String PR_AGRICULTOR     = "AGRICULTOR";
    public static final String PR_COCINERO       = "COCINERO";
    public static final String PR_PESCADOR       = "PESCADOR";
    public static final String PR_CARTOGRAFO     = "CARTÓGRAFO";
    public static final String PR_HERRERO        = "HERRERO";
    public static final String PR_DON_DE_HIERBAS = "DON_DE_HIERBAS";

    private static final List<String> COMBAT = List.of(
            BR_GUERRERO, BR_TANQUE, BR_CAZADOR, BR_MAGO
    );

    private static final List<String> PROFESSIONS = List.of(
            PR_MINERO,
            PR_LENADOR,
            PR_AGRICULTOR,
            PR_COCINERO,
            PR_PESCADOR,
            PR_CARTOGRAFO,
            PR_HERRERO,
            PR_DON_DE_HIERBAS
    );

    public static List<String> combatBranches() { return COMBAT; }
    public static List<String> professionBranches() { return PROFESSIONS; }

    public static List<String> allBranches() {
        ArrayList<String> all = new ArrayList<>(COMBAT.size() + PROFESSIONS.size());
        all.addAll(COMBAT);
        all.addAll(PROFESSIONS);
        return Collections.unmodifiableList(all);
    }

    public static boolean isValidBranchId(String id) {
        if (id == null) return false;
        String s = id.trim();
        return COMBAT.contains(s) || PROFESSIONS.contains(s);
    }
}
