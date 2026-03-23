package Model.policy;

import Model.entety.ClinicalData;
import Model.entety.Patient;
import Model.enums.RiskLevel;

/**
 * Deterministic interpretation of missing clinical risk data (see PLAN: null policy).
 */
public final class PatientRiskPolicy {

    private PatientRiskPolicy() {
    }

    /**
     * Risk used for cohorting (C_safety): missing or null patient / null risk → {@link RiskLevel#UNKNOWN}.
     */
    public static RiskLevel effectiveRiskLevelForCohorting(Patient patient) {
        if (patient == null) {
            return RiskLevel.UNKNOWN;
        }
        ClinicalData cd = patient.getClinicalData();
        if (cd == null || cd.getRiskLevel() == null) {
            return RiskLevel.UNKNOWN;
        }
        return cd.getRiskLevel();
    }

    /**
     * Hard rule: negative-pressure room only when risk is explicitly {@link RiskLevel#INFECTIOUS}.
     * UNKNOWN / null risk does not trigger this hard constraint.
     */
    public static boolean requiresNegativePressureRoom(Patient patient) {
        if (patient == null) {
            return false;
        }
        ClinicalData cd = patient.getClinicalData();
        return cd != null && cd.getRiskLevel() == RiskLevel.INFECTIOUS;
    }
}
