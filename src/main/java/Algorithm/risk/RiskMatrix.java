package Algorithm.risk;

import Model.enums.RiskLevel;


// 2D penalty matrix for cohorting penalty for placing a patient with risk level X
// in a room with effective risk level Y. see in C_safety. Hard violations
public class RiskMatrix {

    private final double[][] matrix;
    private final double bigM;

    public RiskMatrix(double bigM) { // bigM is the penalty for hard violations
        this.bigM = bigM;
        int n = RiskLevel.values().length;
        this.matrix = new double[n][n];
        initDefaultPenalties(); 
    }

    private void initDefaultPenalties() { // initialize the default penalties for each risk level pair
        RiskLevel[] levels = RiskLevel.values();
        for (int i = 0; i < levels.length; i++) {
            for (int j = 0; j < levels.length; j++) {
                matrix[i][j] = getDefaultPenalty(levels[i], levels[j]);
            }
        }
    }// O(|RL|^2)

    
    // default rules: forbidden pairs get Big M otherwise soft penalties or 0.
    private double getDefaultPenalty(RiskLevel patient, RiskLevel roomOrCohort) {
        // unknown + unknown = 5000
        // unknown + other = penaltyUnknownWith(known)
        // infectious + immuno compromised = bigM
        // immuno compromised + infectious = bigM
        // respiratory + immuno compromised = bigM
        // immuno compromised + respiratory = bigM
        // infectious + clean = bigM
        // clean + infectious = bigM
        // respiratory + clean = 500
        // clean + respiratory = 500
        // all other pairs = 0
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
    } // asymmetric hadeling

    private double penaltyUnknownWith(RiskLevel known) {
        return switch (known) {
            case IMMUNO_COMPROMISED, INFECTIOUS -> 100_000;// unknown + immuno compromised / infectious
            case RESPIRATORY, CLEAN -> 30_000;// unkonwn + clean / respiratory 
            case UNKNOWN -> 5_000; // unknown + unknown 
        };
    }
    // ordinal is the index of the risk level in the enum (same as RiskLevel.values() same order)
    public double getPenalty(RiskLevel patient, RiskLevel roomOrCohort) {
        if (patient == null || roomOrCohort == null) return 0;
        return matrix[patient.ordinal()][roomOrCohort.ordinal()];
    } // returns the corsed value if the matrix for risk levels

    
    // override the penalty for a specific risk level pair
    public void setPenalty(RiskLevel patient, RiskLevel roomOrCohort, double penalty) {
        if (patient == null || roomOrCohort == null) return;
        matrix[patient.ordinal()][roomOrCohort.ordinal()] = penalty;
    }

    //any near bigM penalty is considered hard
    public boolean isForbiddenCohortPair(RiskLevel a, RiskLevel b) {
        if (a == null || b == null) return false;
        return getPenalty(a, b) >= bigM * 0.5; // If only exact hard pairs then  (== bigM)
        // but this is for making sure even coustom penalties are considered hard
    }
}
