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
        Room r1 = new Room("R1", "D1", 2, new ArrayList<>(), 0, false);
        r1.getBeds().add(new Bed("B1", "R1", BedType.REGULAR, false));
        r1.getBeds().add(new Bed("B2", "R1", BedType.REGULAR, false));
        List<Room> roomList = new ArrayList<>();
        roomList.add(r1);
        department = new Department("D1", "Internal", roomList, new ArrayList<>());
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
    void check_patientNeedsVentilatorNoSuchBed_stillFeasibleUnderCapacityOnly() {
        Patient p = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 0, true, BedType.ICU));
        department.getWaitingList().add(p);
        FeasibilityResult result = checker.check(department, Map.of("P1", p), new AssignmentState());
        assertTrue(result.isFeasible(), "Capacity-only feasibility allows SA to penalize impossible fits.");
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    void check_noDirectFreeVentilatorBed_butCapacityOk_isFeasible() {
        Room r = department.getRooms().get(0);
        Bed bVent = new Bed("BV", r.getId(), BedType.REGULAR, true);
        Bed bPlain = new Bed("BP", r.getId(), BedType.REGULAR, false);
        r.getBeds().clear();
        r.getBeds().add(bVent);
        r.getBeds().add(bPlain);

        Patient occupant = new Patient("P0", null, new ClinicalData(RiskLevel.CLEAN, 1, false, null));
        occupant.setStatus(PatientStatus.ASSIGNED);
        Patient waiter = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 1, true, null));
        waiter.setStatus(PatientStatus.WAITING);
        department.getWaitingList().add(waiter);

        AssignmentState current = new AssignmentState();
        current.assign(occupant, bVent);

        FeasibilityResult result = checker.check(department,
                Map.of("P0", occupant, "P1", waiter), current);
        assertTrue(result.isFeasible());
        assertTrue(result.getViolations().isEmpty());
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
        Room rIso = new Room("R2", "D1", 1, new ArrayList<>(), 0, true);
        rIso.getBeds().add(new Bed("B3", "R2", BedType.REGULAR, false));
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

    @Test
    void check_waitingPatientAlreadyAssigned_notDoubleCountedForCapacity() {
        Department oneBedDepartment = new Department("D2", "OneBed", new ArrayList<>(), new ArrayList<>());
        Room room = new Room("R1", "D2", 1, new ArrayList<>(), 0, false);
        Bed bed = new Bed("B1", "R1", BedType.REGULAR, false);
        room.getBeds().add(bed);
        oneBedDepartment.addRoom(room);

        Patient p1 = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        p1.setStatus(PatientStatus.WAITING);
        oneBedDepartment.getWaitingList().add(p1);

        AssignmentState current = new AssignmentState();
        current.assign(p1, bed);

        FeasibilityResult r = checker.check(oneBedDepartment, Map.of("P1", p1), current);

        assertTrue(r.isFeasible(), "Patient already assigned should not be counted again from waiting list.");
        assertTrue(r.getViolations().isEmpty());
    }
}
