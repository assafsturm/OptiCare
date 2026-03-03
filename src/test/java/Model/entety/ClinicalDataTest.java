package Model.entety;

import Model.enums.BedType;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalDataTest {

    @Test
    void needsBariatricBed_whenWeightAtOrAboveThreshold_returnsTrue() {
        ClinicalData data = new ClinicalData(null, 0, false, null, 120);
        assertTrue(data.needsBariatricBed());
    }

    @Test
    void needsBariatricBed_whenWeightAboveThreshold_returnsTrue() {
        ClinicalData data = new ClinicalData(null, 0, false, null, 150);
        assertTrue(data.needsBariatricBed());
    }

    @Test
    void needsBariatricBed_whenWeightBelow_returnsFalse() {
        ClinicalData data = new ClinicalData(null, 0, false, null, 119);
        assertFalse(data.needsBariatricBed());
    }

    @Test
    void needsBariatricBed_whenWeightNull_returnsFalse() {
        ClinicalData data = new ClinicalData();
        data.setWeightKg(null);
        assertFalse(data.needsBariatricBed());
    }

    @Test
    void needsBariatricBed_withCustomThreshold() {
        ClinicalData data = new ClinicalData(null, 0, false, null, 100);
        assertTrue(data.needsBariatricBed(100));
        assertFalse(data.needsBariatricBed(101));
    }
}
