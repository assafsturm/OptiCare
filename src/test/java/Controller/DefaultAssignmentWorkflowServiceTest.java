package Controller;

import Algorithm.AssignmentState;
import Algorithm.fixtures.Stage3FixtureFactory;
import Config.AlgorithmConfig;
import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.PatientStatus;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAssignmentWorkflowServiceTest {

    @Test
    void proposeAssignment_feasibleDepartment_returnsValidProposal() {
        AlgorithmConfig config = new AlgorithmConfig();
        config.setRandomSeed(42L);
        config.setMaxTotalIterations(150);
        config.setIterationsPerTemperature(15);
        config.setInitialTemperature(800.0);
        config.setMinTemperature(0.1);
        config.setCoolingRate(0.9);
        config.setNeighborSampleAttemptsPerIteration(120);

        Department department = Stage3FixtureFactory.mediumDepartment();
        Map<String, Patient> patientById = new LinkedHashMap<>(Stage3FixtureFactory.patientIndex(department));
        DefaultAssignmentWorkflowService service = new DefaultAssignmentWorkflowService(config);

        AssignmentProposal proposal = service.proposeAssignment(department, patientById, new AssignmentState());

        assertNotNull(proposal);
        assertTrue(proposal.feasible());
        assertTrue(proposal.feasibilityViolations().isEmpty());
        assertNotNull(proposal.baselineState());
        assertNotNull(proposal.proposedState());
        assertTrue(proposal.proposedZ() <= proposal.baselineZ() + 1e-9);
        assertEquals(proposal.proposedZ() - proposal.baselineZ(), proposal.deltaZ(), 1e-9);
        assertTrue(proposal.iterations() > 0);
    }

    @Test
    void proposeAssignment_infeasibleDepartment_returnsViolationsAndSkipsSa() {
        AlgorithmConfig config = new AlgorithmConfig();
        Department department = new Department("D1", "Internal", new ArrayList<>(), new ArrayList<>());
        Room room = new Room("R1", "D1", 1, new ArrayList<>(), 5.0, false, true);
        room.getBeds().add(new Bed("B1", "R1", BedType.REGULAR, false, false));
        department.addRoom(room);

        Patient p1 = waiting("W1");
        Patient p2 = waiting("W2");
        department.getWaitingList().addAll(List.of(p1, p2));
        Map<String, Patient> patientById = new LinkedHashMap<>();
        patientById.put(p1.getId(), p1);
        patientById.put(p2.getId(), p2);

        DefaultAssignmentWorkflowService service = new DefaultAssignmentWorkflowService(config);
        AssignmentProposal proposal = service.proposeAssignment(department, patientById, new AssignmentState());

        assertNotNull(proposal);
        assertFalse(proposal.feasible());
        assertFalse(proposal.feasibilityViolations().isEmpty());
        assertEquals(0, proposal.iterations());
        assertEquals(0.0, proposal.baselineZ(), 1e-9);
        assertEquals(0.0, proposal.proposedZ(), 1e-9);
        assertEquals(0.0, proposal.deltaZ(), 1e-9);
    }

    @Test
    void buildPreview_classifiesAssignedMovedUnassignedAndUnchanged() {
        AlgorithmConfig config = new AlgorithmConfig();
        DefaultAssignmentWorkflowService service = new DefaultAssignmentWorkflowService(config);

        Bed b1 = new Bed("B1", "R1", BedType.REGULAR, false, false);
        Bed b2 = new Bed("B2", "R1", BedType.REGULAR, false, false);
        Bed b3 = new Bed("B3", "R2", BedType.REGULAR, false, false);

        Patient p1 = waiting("P1");
        Patient p2 = waiting("P2");
        Patient p3 = waiting("P3");
        Patient p4 = waiting("P4");

        AssignmentState baseline = new AssignmentState();
        baseline.assign(p1, b1); // unchanged
        baseline.assign(p2, b2); // moved
        baseline.assign(p3, b3); // unassigned

        AssignmentState proposed = new AssignmentState();
        proposed.assign(p1, b1); // unchanged
        proposed.assign(p2, b3); // moved
        proposed.assign(p4, b2); // assigned

        AssignmentProposal proposal = new AssignmentProposal(
                true,
                List.of(),
                baseline,
                proposed,
                100.0,
                70.0,
                10,
                false
        );

        AssignmentPreview preview = service.buildPreview(proposal);

        assertNotNull(preview);
        assertEquals(4, preview.patientDiffs().size());
        assertEquals(3, preview.changedPatients());
        assertEquals(1, preview.unchangedPatients());
        assertEquals(-30.0, preview.deltaZ(), 1e-9);
        assertTrue(preview.patientDiffs().stream().anyMatch(d ->
                "P1".equals(d.patientId()) && d.changeType() == AssignmentChangeType.UNCHANGED));
        assertTrue(preview.patientDiffs().stream().anyMatch(d ->
                "P2".equals(d.patientId()) && d.changeType() == AssignmentChangeType.MOVED));
        assertTrue(preview.patientDiffs().stream().anyMatch(d ->
                "P3".equals(d.patientId()) && d.changeType() == AssignmentChangeType.UNASSIGNED));
        assertTrue(preview.patientDiffs().stream().anyMatch(d ->
                "P4".equals(d.patientId()) && d.changeType() == AssignmentChangeType.ASSIGNED));
    }

    @Test
    void approvePendingProposal_appliesProposedStateAndClearsPending() {
        AlgorithmConfig config = new AlgorithmConfig();
        DefaultAssignmentWorkflowService service = new DefaultAssignmentWorkflowService(config);
        String departmentId = "D1";

        Bed b1 = new Bed("B1", "R1", BedType.REGULAR, false, false);
        Bed b2 = new Bed("B2", "R1", BedType.REGULAR, false, false);
        Patient p1 = waiting("P1");

        AssignmentState current = new AssignmentState();
        current.assign(p1, b1);
        AssignmentState proposed = new AssignmentState();
        proposed.assign(p1, b2);

        AssignmentProposal proposal = new AssignmentProposal(
                true, List.of(), new AssignmentState(current), proposed, 100.0, 80.0, 10, false);
        service.setPendingProposal(departmentId, proposal);

        AssignmentState approved = service.approvePendingProposal(departmentId, current);

        assertEquals("B2", approved.getBed("P1").getId());
        assertNull(service.getPendingProposal(departmentId));
    }

    @Test
    void rejectPendingProposal_keepsCurrentStateAndClearsPending() {
        AlgorithmConfig config = new AlgorithmConfig();
        DefaultAssignmentWorkflowService service = new DefaultAssignmentWorkflowService(config);
        String departmentId = "D1";

        Bed b1 = new Bed("B1", "R1", BedType.REGULAR, false, false);
        Bed b2 = new Bed("B2", "R1", BedType.REGULAR, false, false);
        Patient p1 = waiting("P1");

        AssignmentState current = new AssignmentState();
        current.assign(p1, b1);
        AssignmentState proposed = new AssignmentState();
        proposed.assign(p1, b2);

        AssignmentProposal proposal = new AssignmentProposal(
                true, List.of(), new AssignmentState(current), proposed, 100.0, 80.0, 10, false);
        service.setPendingProposal(departmentId, proposal);

        AssignmentState rejected = service.rejectPendingProposal(departmentId, current);

        assertEquals("B1", rejected.getBed("P1").getId());
        assertNull(service.getPendingProposal(departmentId));
    }

    @Test
    void admitPatient_addsToWaitingListAsWaiting() {
        AlgorithmConfig config = new AlgorithmConfig();
        DefaultAssignmentWorkflowService service = new DefaultAssignmentWorkflowService(config);
        Department department = new Department("D1", "Internal", new ArrayList<>(), new ArrayList<>());

        Patient p1 = waiting("P1");
        p1.setStatus(PatientStatus.DISCHARGED);
        service.admitPatient(department, p1);

        assertEquals(1, department.getWaitingList().size());
        assertEquals("P1", department.getWaitingList().get(0).getId());
        assertEquals(PatientStatus.WAITING, department.getWaitingList().get(0).getStatus());
    }

    @Test
    void dischargePatient_unassignsAndRemovesFromWaitingList() {
        AlgorithmConfig config = new AlgorithmConfig();
        DefaultAssignmentWorkflowService service = new DefaultAssignmentWorkflowService(config);
        Department department = new Department("D1", "Internal", new ArrayList<>(), new ArrayList<>());
        Room room = new Room("R1", "D1", 1, new ArrayList<>(), 5.0, false, true);
        Bed bed = new Bed("B1", "R1", BedType.REGULAR, false, false);
        room.getBeds().add(bed);
        department.addRoom(room);

        Patient p1 = waiting("P1");
        department.getWaitingList().add(p1);
        AssignmentState current = new AssignmentState();
        current.assign(p1, bed);

        AssignmentState after = service.dischargePatient(department, "P1", current);

        assertEquals(0, department.getWaitingList().size());
        assertNull(after.getBed("P1"));
    }

    @Test
    void buildWaitingQueueView_sortsByConfiguredDeterministicComparator() {
        AlgorithmConfig config = new AlgorithmConfig();
        DefaultAssignmentWorkflowService service = new DefaultAssignmentWorkflowService(config);
        Department department = new Department("D1", "Internal", new ArrayList<>(), new ArrayList<>());

        Patient p1 = new Patient("P1", null,
                new ClinicalData(RiskLevel.CLEAN, 4, false, null),
                Instant.parse("2026-03-01T10:00:00Z"), false);
        p1.setStatus(PatientStatus.WAITING);
        Patient p2 = new Patient("P2", null,
                new ClinicalData(RiskLevel.IMMUNO_COMPROMISED, 2, false, null),
                Instant.parse("2026-03-01T11:00:00Z"), false);
        p2.setStatus(PatientStatus.WAITING);
        Patient p3 = new Patient("P3", null,
                new ClinicalData(RiskLevel.CLEAN, 9, false, null),
                Instant.parse("2026-03-01T09:00:00Z"), true);
        p3.setStatus(PatientStatus.WAITING); // unavailable -> excluded from queue view

        department.getWaitingList().addAll(List.of(p1, p2, p3));
        List<Patient> queue = service.buildWaitingQueueView(department);

        assertEquals(2, queue.size());
        assertEquals("P2", queue.get(0).getId()); // better risk priority
        assertEquals("P1", queue.get(1).getId());
    }

    @Test
    void stage4FacadeFlow_admitProposePreviewApprove_endToEnd() {
        AlgorithmConfig config = new AlgorithmConfig();
        config.setRandomSeed(42L);
        config.setMaxTotalIterations(120);
        config.setIterationsPerTemperature(12);
        config.setInitialTemperature(600.0);
        config.setMinTemperature(0.1);
        config.setCoolingRate(0.9);
        config.setNeighborSampleAttemptsPerIteration(100);

        DefaultAssignmentWorkflowService service = new DefaultAssignmentWorkflowService(config);
        Department department = new Department("D1", "Internal", new ArrayList<>(), new ArrayList<>());
        Room room = new Room("R1", "D1", 1, new ArrayList<>(), 5.0, true, true);
        room.getBeds().add(new Bed("B1", "R1", BedType.REGULAR, false, false));
        department.addRoom(room);

        Patient admitted = waiting("P_ADMIT");
        service.admitPatient(department, admitted);
        Map<String, Patient> patientById = new LinkedHashMap<>();
        patientById.put(admitted.getId(), admitted);

        AssignmentState current = new AssignmentState();
        AssignmentProposal proposal = service.proposeAssignment(department, patientById, current);
        assertTrue(proposal.feasible());

        AssignmentPreview preview = service.buildPreview(proposal);
        assertNotNull(preview);
        assertEquals(1, preview.patientDiffs().size());
        assertEquals(1, preview.changedPatients() + preview.unchangedPatients());

        service.setPendingProposal(department.getId(), proposal);
        AssignmentState approved = service.approvePendingProposal(department.getId(), current);

        assertNotNull(approved.getBed(admitted.getId()));
        assertEquals("B1", approved.getBed(admitted.getId()).getId());
        assertEquals(PatientStatus.WAITING, admitted.getStatus());
        assertNull(service.getPendingProposal(department.getId()));
    }

    private static Patient waiting(String id) {
        Patient p = new Patient(id, null,
                new ClinicalData(RiskLevel.CLEAN, 1, false, null),
                Instant.parse("2026-03-01T10:00:00Z"), false);
        p.setStatus(PatientStatus.WAITING);
        return p;
    }
}
