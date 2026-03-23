package Model.policy;

import Model.entety.ClinicalData;
import Model.entety.Patient;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientRiskPolicyTest {

    @Test
    void effectiveRisk_nullOrMissingClinical_isUnknown() {
        assertEquals(RiskLevel.UNKNOWN, PatientRiskPolicy.effectiveRiskLevelForCohorting(null));
        Patient p = new Patient("P1", null, null);
        assertEquals(RiskLevel.UNKNOWN, PatientRiskPolicy.effectiveRiskLevelForCohorting(p));
        p.setClinicalData(new ClinicalData(null, 0, false, null));
        assertEquals(RiskLevel.UNKNOWN, PatientRiskPolicy.effectiveRiskLevelForCohorting(p));
    }

    @Test
    void requiresNegativePressure_onlyExplicitInfectious() {
        Patient p = new Patient("P1", null, new ClinicalData(null, 0, false, null));
        assertFalse(PatientRiskPolicy.requiresNegativePressureRoom(p));
        p.setClinicalData(new ClinicalData(RiskLevel.UNKNOWN, 0, false, null));
        assertFalse(PatientRiskPolicy.requiresNegativePressureRoom(p));
        p.setClinicalData(new ClinicalData(RiskLevel.INFECTIOUS, 0, false, null));
        assertTrue(PatientRiskPolicy.requiresNegativePressureRoom(p));
    }
}
