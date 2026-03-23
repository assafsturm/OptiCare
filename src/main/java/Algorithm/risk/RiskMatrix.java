package Algorithm.risk;

import Model.enums.RiskLevel;

/**
 * 2D penalty matrix for cohorting: penalty for placing a patient with risk level X
 * in a room/cohort with effective risk level Y. Used in C_safety. Hard violations
 * (e.g. INFECTIOUS next to IMMUNO_COMPROMISED) get Big M so the solution is rejected.
 */
public class RiskMatrix {

    private final double[][] matrix;
    private final double bigM;

    public RiskMatrix(double bigM) {
        this.bigM = bigM;
        int n = RiskLevel.values().length;
        this.matrix = new double[n][n];
        initDefaultPenalties();
    }

    private void initDefaultPenalties() {
        RiskLevel[] levels = RiskLevel.values();
        for (int i = 0; i < levels.length; i++) {
            for (int j = 0; j < levels.length; j++) {
                matrix[i][j] = getDefaultPenalty(levels[i], levels[j]);
            }
        }
    }

    /**
     * Default rules: forbidden pairs get Big M; otherwise soft penalties or 0.
     * INFECTIOUS in same cohort as IMMUNO_COMPROMISED = forbidden.
     * RESPIRATORY next to IMMUNO_COMPROMISED = forbidden (per RiskLevel JavaDoc).
     */
    private double getDefaultPenalty(RiskLevel patient, RiskLevel roomOrCohort) {
        // UNKNOWN: conservative finite penalties only (not automatically hard-forbidden).
        if (patient == RiskLevel.UNKNOWN && roomOrCohort == RiskLevel.UNKNOWN) {
            return 5_000;
        }
        if (patient == RiskLevel.UNKNOWN || roomOrCohort == RiskLevel.UNKNOWN) {
            RiskLevel known = patient == RiskLevel.UNKNOWN ? roomOrCohort : patient;
            return penaltyUnknownWith(known);
        }
        if (patient == RiskLevel.INFECTIOUS && roomOrCohort == RiskLevel.IMMUNO_COMPROMISED) return bigM;
        if (patient == RiskLevel.IMMUNO_COMPROMISED && roomOrCohort == RiskLevel.INFECTIOUS) return bigM;
        if (patient == RiskLevel.RESPIRATORY && roomOrCohort == RiskLevel.IMMUNO_COMPROMISED) return bigM;
        if (patient == RiskLevel.IMMUNO_COMPROMISED && roomOrCohort == RiskLevel.RESPIRATORY) return bigM;
        if (patient == RiskLevel.INFECTIOUS && roomOrCohort == RiskLevel.CLEAN) return bigM;
        if (patient == RiskLevel.CLEAN && roomOrCohort == RiskLevel.INFECTIOUS) return bigM;
        if (patient == RiskLevel.RESPIRATORY && roomOrCohort == RiskLevel.CLEAN) return 500;
        if (patient == RiskLevel.CLEAN && roomOrCohort == RiskLevel.RESPIRATORY) return 500;
        return 0;
    }

    private double penaltyUnknownWith(RiskLevel known) {
        return switch (known) {
            case IMMUNO_COMPROMISED, INFECTIOUS -> 100_000;
            case RESPIRATORY, CLEAN -> 30_000;
            case UNKNOWN -> 5_000;
        };
    }

    public double getPenalty(RiskLevel patient, RiskLevel roomOrCohort) {
        if (patient == null || roomOrCohort == null) return 0;
        return matrix[patient.ordinal()][roomOrCohort.ordinal()];
    }

    /** Override penalty for a specific (patient, room/cohort) pair. */
    public void setPenalty(RiskLevel patient, RiskLevel roomOrCohort, double penalty) {
        if (patient == null || roomOrCohort == null) return;
        matrix[patient.ordinal()][roomOrCohort.ordinal()] = penalty;
    }
}
