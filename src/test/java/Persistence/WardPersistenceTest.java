package Persistence;

import Algorithm.AssignmentState;
import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.PatientStatus;
import Model.enums.RiskLevel;
import Persistence.dto.WardStateDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class WardPersistenceTest {

    @Test
    void hydrate_rejectsUnknownSchema() {
        WardStateDocument doc = new WardStateDocument();
        doc.setSchemaVersion(999);
        assertThrows(IllegalArgumentException.class, () -> WardStateMapper.hydrate(doc));
    }

    @Test
    void save_firstWrite_writesVersionOne(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("ward-state.json");
        JsonFileWardStateRepository repo = new JsonFileWardStateRepository(file);
        MinimalWardFixture f = demoTwoDeptFixture();

        WardStateDocument draft = WardStateMapper.captureDraft(
                f.departments(), f.patientsByDepartmentId(), f.assignmentStates());
        long v = repo.save(draft);
        assertEquals(1L, v);
        WardStateDocument loaded = repo.loadIfPresent().orElseThrow();
        assertEquals(1, loaded.getSchemaVersion());
        assertEquals(1L, loaded.getPersistVersion());
    }

    @Test
    void save_twice_incrementsPersistVersion(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("ward-state.json");
        JsonFileWardStateRepository repo = new JsonFileWardStateRepository(file);
        MinimalWardFixture f = demoTwoDeptFixture();
        WardStateDocument d1 = WardStateMapper.captureDraft(
                f.departments(), f.patientsByDepartmentId(), f.assignmentStates());
        long v1 = repo.save(d1);

        WardStateDocument d2 = WardStateMapper.captureDraft(
                f.departments(), f.patientsByDepartmentId(), f.assignmentStates());
        long v2 = repo.save(d2);
        assertEquals(1L, v1);
        assertEquals(2L, v2);
    }

    @Test
    void roundTrip_mapperAndRepository_restoreAssignments(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("ward-state.json");
        JsonFileWardStateRepository repo = new JsonFileWardStateRepository(file);
        MinimalWardFixture f = demoTwoDeptFixture();
        WardStateDocument draft = WardStateMapper.captureDraft(
                f.departments(), f.patientsByDepartmentId(), f.assignmentStates());
        repo.save(draft);

        WardStateDocument loaded = repo.loadIfPresent().orElseThrow();
        WardStateMapper.WardHydration h = WardStateMapper.hydrate(loaded);

        AssignmentState st = h.assignmentStates().get("D1");
        Bed assigned = st.getBed("P2");
        assertEquals("B1", assigned.getId());
        Patient p2 = h.patientsByDepartmentId().get("D1").get("P2");
        assertEquals(PatientStatus.ASSIGNED, p2.getStatus());
    }

    /** Same shape as {@code OptiCareApp#seedDemoData} (compact) for integration-style round-trip checks. */
    private static MinimalWardFixture demoTwoDeptFixture() {
        Room r1 = new Room("R1", "D1", 2, new ArrayList<>(), 5.0, true);
        Room r2 = new Room("R2", "D1", 2, new ArrayList<>(), 12.0, false);
        r1.getBeds().add(new Bed("B1", "R1", BedType.REGULAR, false));
        r1.getBeds().add(new Bed("B2", "R1", BedType.ICU, true));
        r2.getBeds().add(new Bed("B3", "R2", BedType.REGULAR, false));
        r2.getBeds().add(new Bed("B4", "R2", BedType.BARIATRIC, false));
        Department d1 = new Department("D1", "Internal", new ArrayList<>(List.of(r1, r2)), new ArrayList<>());

        Patient p1 = waiting("P1", RiskLevel.RESPIRATORY, 7, Instant.parse("2026-03-01T10:00:00Z"));
        Patient p2 = waiting("P2", RiskLevel.CLEAN, 3, Instant.parse("2026-03-01T11:00:00Z"));
        Patient p3 = waiting("P3", RiskLevel.IMMUNO_COMPROMISED, 8, Instant.parse("2026-03-01T09:00:00Z"));
        d1.getWaitingList().addAll(List.of(p1, p2, p3));
        Map<String, Patient> pByIdD1 = new HashMap<>();
        pByIdD1.put(p1.getId(), p1);
        pByIdD1.put(p2.getId(), p2);
        pByIdD1.put(p3.getId(), p3);
        AssignmentState stateD1 = new AssignmentState();
        stateD1.assign(p2, r1.getBeds().get(0));
        p2.setStatus(PatientStatus.ASSIGNED);
        d1.getWaitingList().removeIf(p -> p != null && p2.getId().equals(p.getId()));

        Room r3 = new Room("R3", "D2", 2, new ArrayList<>(), 4.0, true);
        Room r4 = new Room("R4", "D2", 1, new ArrayList<>(), 9.0, false);
        r3.getBeds().add(new Bed("B5", "R3", BedType.ICU, true));
        r3.getBeds().add(new Bed("B6", "R3", BedType.REGULAR, false));
        r4.getBeds().add(new Bed("B7", "R4", BedType.REGULAR, false));
        Department d2 = new Department("D2", "Surgery", new ArrayList<>(List.of(r3, r4)), new ArrayList<>());
        Patient p4 = waiting("P4", RiskLevel.INFECTIOUS, 6, Instant.parse("2026-03-01T08:30:00Z"));
        Patient p5 = waiting("P5", RiskLevel.CLEAN, 2, Instant.parse("2026-03-01T08:40:00Z"));
        d2.getWaitingList().addAll(List.of(p4, p5));
        Map<String, Patient> pByIdD2 = new HashMap<>();
        pByIdD2.put(p4.getId(), p4);
        pByIdD2.put(p5.getId(), p5);
        AssignmentState stateD2 = new AssignmentState();

        List<Department> departments = List.of(d1, d2);
        Map<String, Map<String, Patient>> patients = Map.of(
                "D1", pByIdD1,
                "D2", pByIdD2);
        Map<String, AssignmentState> assigns = Map.of(
                "D1", stateD1,
                "D2", stateD2);

        return new MinimalWardFixture(departments, patients, assigns);
    }

    private static Patient waiting(String id, RiskLevel risk, int severity, Instant admittedAt) {
        Patient p = new Patient(id, null, new ClinicalData(risk, severity, false, null), admittedAt, false);
        p.setStatus(PatientStatus.WAITING);
        return p;
    }

    private record MinimalWardFixture(
            List<Department> departments,
            Map<String, Map<String, Patient>> patientsByDepartmentId,
            Map<String, AssignmentState> assignmentStates
    ) {}
}
