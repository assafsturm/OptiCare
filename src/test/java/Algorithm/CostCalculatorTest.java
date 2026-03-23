package Algorithm;

import Config.AlgorithmConfig;
import Algorithm.risk.RiskMatrix;
import Algorithm.risk.RiskMatrixFactory;
import Model.entety.*;
import Model.enums.BedType;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CostCalculatorTest {

    private AlgorithmConfig config;
    private RiskMatrix riskMatrix;
    private CostCalculator calculator;
    private Department department;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        config = new AlgorithmConfig();
        riskMatrix = RiskMatrixFactory.fromConfig(config);
        calculator = new CostCalculator(riskMatrix, config);
        department = new Department("D1", "Internal", new java.util.ArrayList<>(), List.of());
        Room r1 = new Room("R1", "D1", 2, new java.util.ArrayList<>(), 10.0, false, true);
        r1.getBeds().add(new Bed("B1", "R1", BedType.REGULAR, false, false));
        r1.getBeds().add(new Bed("B2", "R1", BedType.REGULAR, false, false));
        department.addRoom(r1);
    }

    @Test
    void computeZ_emptyState_returnsZero() {
        AssignmentState state = new AssignmentState();
        double z = calculator.computeZ(state, department, Map.of());
        assertEquals(0, z);
    }

    @Test
    void computeCClinical_brokenBed_returnsBigM() {
        Patient p = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        Bed broken = new Bed("B1", "R1", BedType.REGULAR, false, true);
        broken.setBroken(true);
        double c = calculator.computeCClinical(p, broken, department.getRooms().get(0));
        assertEquals(config.getBigM(), c);
    }

    @Test
    void computeCClinical_ventilatorMismatch_returnsBigM() {
        ClinicalData cd = new ClinicalData(RiskLevel.CLEAN, 0, true, BedType.ICU);
        Patient p = new Patient("P1", null, cd);
        Bed noVent = new Bed("B1", "R1", BedType.ICU, false, false);
        double c = calculator.computeCClinical(p, noVent, department.getRooms().get(0));
        assertEquals(config.getBigM(), c);
    }

    @Test
    void computeCClinical_infectiousWithoutNegativePressure_returnsBigM() {
        Patient p = new Patient("P1", null, new ClinicalData(RiskLevel.INFECTIOUS, 0, false, null));
        Room noPressure = new Room("R2", "D1", 1, new java.util.ArrayList<>(), 0, false, false);
        noPressure.getBeds().add(new Bed("B3", "R2", BedType.REGULAR, false, false));
        double c = calculator.computeCClinical(p, noPressure.getBeds().get(0), noPressure);
        assertEquals(config.getBigM(), c);
    }

    @Test
    void computeCClinical_goodMatch_returnsZero() {
        Patient p = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, BedType.REGULAR));
        Bed bed = new Bed("B1", "R1", BedType.REGULAR, false, false);
        double c = calculator.computeCClinical(p, bed, department.getRooms().get(0));
        assertEquals(0, c);
    }

    @Test
    void computeCSafety_forbiddenPair_returnsBigM() {
        Patient infectious = new Patient("P1", null, new ClinicalData(RiskLevel.INFECTIOUS, 0, false, null));
        Patient immuno = new Patient("P2", null, new ClinicalData(RiskLevel.IMMUNO_COMPROMISED, 0, false, null));
        AssignmentState state = new AssignmentState();
        state.assign(infectious, department.getRooms().get(0).getBeds().get(0));
        state.assign(immuno, department.getRooms().get(0).getBeds().get(1));
        Map<String, Patient> byId = Map.of("P1", infectious, "P2", immuno);
        double c1 = calculator.computeCSafety(infectious, department.getRooms().get(0), state, department, byId, "P1",
                department.getRooms().get(0).getBeds().get(0));
        double c2 = calculator.computeCSafety(immuno, department.getRooms().get(0), state, department, byId, "P2",
                department.getRooms().get(0).getBeds().get(1));
        assertTrue(c1 >= config.getBigM() || c2 >= config.getBigM());
    }

    @Test
    void computeCPolicy_distanceAndSeverity_increasesCost() {
        Patient p = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 5, false, null));
        Room far = new Room("R2", "D1", 1, new java.util.ArrayList<>(), 20.0, false, false);
        double c = calculator.computeCPolicy(p, far);
        assertEquals(config.getPolicyPenaltyWeight() * 20 * 5, c);
    }

    @Test
    void computeCTransfer_whenMoved_returnsTransferWeight() {
        AssignmentState initial = new AssignmentState();
        AssignmentState current = new AssignmentState();
        Patient p = new Patient("P1", null, null);
        Bed b1 = department.getRooms().get(0).getBeds().get(0);
        Bed b2 = department.getRooms().get(0).getBeds().get(1);
        initial.assign(p, b1);
        current.assign(p, b2);
        double c = calculator.computeCTransfer("P1", b2, current, initial);
        assertEquals(config.getTransferPenaltyWeight(), c);
    }

    @Test
    void computeCTransfer_whenNotMoved_returnsZero() {
        AssignmentState initial = new AssignmentState();
        AssignmentState current = new AssignmentState();
        Patient p = new Patient("P1", null, null);
        Bed b1 = department.getRooms().get(0).getBeds().get(0);
        initial.assign(p, b1);
        current.assign(p, b1);
        double c = calculator.computeCTransfer("P1", b1, current, initial);
        assertEquals(0, c);
    }

    @Test
    void computeZ_withInitialState_includesTransferCost() {
        Patient p = new Patient("P1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        AssignmentState initial = new AssignmentState();
        AssignmentState current = new AssignmentState();
        Bed b1 = department.getRooms().get(0).getBeds().get(0);
        Bed b2 = department.getRooms().get(0).getBeds().get(1);
        initial.assign(p, b1);
        current.assign(p, b2);
        Map<String, Patient> byId = Map.of("P1", p);
        double z = calculator.computeZ(current, department, byId, initial);
        assertTrue(z >= config.getTransferPenaltyWeight());
    }

    @Test
    void computeCClinical_nullClinicalData_noTypeOrVentPenalties_brokenBedStillBigM() {
        Patient p = new Patient("P1", null, null);
        Bed bed = new Bed("B1", "R1", BedType.REGULAR, false, false);
        double c = calculator.computeCClinical(p, bed, department.getRooms().get(0));
        assertEquals(0, c);
        bed.setBroken(true);
        assertEquals(config.getBigM(), calculator.computeCClinical(p, bed, department.getRooms().get(0)));
    }

    @Test
    void computeCSafety_nullRisk_treatedAsUnknown_finitePenaltyVsClean() {
        Patient unknownRisk = new Patient("P1", null, new ClinicalData(null, 0, false, null));
        Patient clean = new Patient("P2", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        AssignmentState state = new AssignmentState();
        state.assign(unknownRisk, department.getRooms().get(0).getBeds().get(0));
        state.assign(clean, department.getRooms().get(0).getBeds().get(1));
        Map<String, Patient> byId = Map.of("P1", unknownRisk, "P2", clean);
        double c = calculator.computeCSafety(unknownRisk, department.getRooms().get(0), state, department, byId, "P1",
                department.getRooms().get(0).getBeds().get(0));
        assertEquals(30_000, c, 1e-9);
    }

    @Test
    void computeCUnassigned_waitingPatientNotInState_addsWeightedCost() {
        Patient waiting = new Patient("W1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        department.getWaitingList().add(waiting);
        AssignmentState state = new AssignmentState();
        double cu = calculator.computeCUnassigned(state, department);
        assertEquals(config.getUnassignedPenaltyWeight(), cu);
        double z = calculator.computeZ(state, department, Map.of("W1", waiting));
        assertEquals(cu, z);
    }

    @Test
    void computeZ_includesUnassignedAlongsideAssigned() {
        Patient assigned = new Patient("A1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        Patient waiting = new Patient("W1", null, new ClinicalData(RiskLevel.CLEAN, 0, false, null));
        department.getWaitingList().add(waiting);
        AssignmentState state = new AssignmentState();
        state.assign(assigned, department.getRooms().get(0).getBeds().get(0));
        Map<String, Patient> byId = Map.of("A1", assigned, "W1", waiting);
        double z = calculator.computeZ(state, department, byId);
        assertTrue(z >= config.getUnassignedPenaltyWeight());
    }
}
