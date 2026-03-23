package Algorithm;

import Config.AlgorithmConfig;
import Algorithm.feasibility.FeasibilityChecker;
import Algorithm.feasibility.FeasibilityResult;
import Model.entety.*;
import Model.enums.BedType;
import Model.enums.PatientStatus;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeasibilityCheckerTest {

    private AlgorithmConfig config;
    private FeasibilityChecker checker;
    private Department department;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        config = new AlgorithmConfig();
        checker = new FeasibilityChecker(config);
        Room r1 = new Room("R1", "D1", 2, new ArrayList<>(), 0, false, false);
        r1.getBeds().add(new Bed("B1", "R1", BedType.REGULAR, false, false));
        r1.getBeds().add(new Bed("B2", "R1", BedType.REGULAR, false, false));
        department = new Department("D1", "Internal", List.of(r1), new ArrayList<>());
    }

    @Test
    void check_enoughBedsAndNoWaiting_isFeasible() {
        FeasibilityResult r = checker.check(department, Map.of(), new AssignmentState());
        assertTrue(r.isFeasible());
        assertTrue(r.getViolations().isEmpty());
    }

    @Test
    void check_morePatientsThanBeds_isInfeasible() {
        Patient p1 = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        Patient p2 = new Patient("P2", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        Patient p3 = new Patient("P3", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        department.getWaitingList().add(p1);
        department.getWaitingList().add(p2);
        department.getWaitingList().add(p3);
        FeasibilityResult r = checker.check(department, Map.of("P1", p1, "P2", p2, "P3", p3), new AssignmentState());
        assertFalse(r.isFeasible());
        assertTrue(r.getViolations().stream().anyMatch(s -> s.contains("Not enough beds")));
    }

    @Test
    void check_patientNeedsVentilatorNoSuchBed_isInfeasible() {
        Patient p = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 0, true, BedType.ICU));
        department.getWaitingList().add(p);
        FeasibilityResult result = checker.check(department, Map.of("P1", p), new AssignmentState());
        assertFalse(result.isFeasible());
        assertTrue(result.getViolations().stream().anyMatch(s -> s.contains("No legal bed")));
    }

    @Test
    void feasible_hasNoViolations() {
        FeasibilityResult r = FeasibilityResult.feasible();
        assertTrue(r.isFeasible());
        assertTrue(r.getViolations().isEmpty());
    }

    @Test
    void infeasible_hasReasons() {
        FeasibilityResult r = FeasibilityResult.infeasible("Not enough beds", "No isolation room");
        assertFalse(r.isFeasible());
        assertEquals(2, r.getViolations().size());
        assertTrue(r.getViolations().contains("Not enough beds"));
    }

    @Test
    void check_temporarilyUnavailableWaiting_excludedFromBedCount() {
        Patient p1 = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        Patient p2 = new Patient("P2", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        p2.setTemporarilyUnavailable(true);
        department.getWaitingList().add(p1);
        department.getWaitingList().add(p2);
        FeasibilityResult r = checker.check(department, Map.of("P1", p1, "P2", p2), new AssignmentState());
        assertTrue(r.isFeasible());
    }

    @Test
    void check_nullRisk_notForcedIntoNegativePressureBed() {
        Room rIso = new Room("R2", "D1", 1, new ArrayList<>(), 0, true, false);
        rIso.getBeds().add(new Bed("B3", "R2", BedType.REGULAR, false, false));
        department.addRoom(rIso);
        Patient p = new Patient("P1", null, new ClinicalData(null, 0, false, null));
        department.getWaitingList().add(p);
        FeasibilityResult r = checker.check(department, Map.of("P1", p), new AssignmentState());
        assertTrue(r.isFeasible());
    }

    @Test
    void check_nonWaitingOnWaitingList_ignoredForCapacity() {
        Patient p1 = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        Patient p2 = new Patient("P2", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        p2.setStatus(PatientStatus.ASSIGNED);
        department.getWaitingList().add(p1);
        department.getWaitingList().add(p2);
        FeasibilityResult r = checker.check(department, Map.of("P1", p1, "P2", p2), new AssignmentState());
        assertTrue(r.isFeasible());
    }
}
