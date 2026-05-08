package Model.policy;

import Model.entety.ClinicalData;
import Model.entety.Patient;
import Model.enums.RiskLevel;

// one direct way to handle the risk level of a patient when data is missing or null
public final class PatientRiskPolicy {

    private PatientRiskPolicy() {
    }

  
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

    // hard rule negativepressure room only when risk is explicitly INFECTIOUS
    // UNKNOWN / null risk does not trigger this hard constraint.
    public static boolean requiresNegativePressureRoom(Patient patient) {
        if (patient == null) {
            return false;
        }
        ClinicalData cd = patient.getClinicalData();
        return cd != null && cd.getRiskLevel() == RiskLevel.INFECTIOUS;
    }
}
