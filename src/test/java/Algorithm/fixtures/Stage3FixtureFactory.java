package Algorithm.fixtures;

import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.PatientStatus;
import Model.enums.RiskLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared deterministic fixtures for Stage 3 tests.
 */
public final class Stage3FixtureFactory {

    private Stage3FixtureFactory() {
    }

    public static Department mediumDepartment() {
        Room r1 = new Room("R1", "D1", 2, new ArrayList<>(), 5, true, true);
        Room r2 = new Room("R2", "D1", 2, new ArrayList<>(), 12, false, true);
        Room r3 = new Room("R3", "D1", 1, new ArrayList<>(), 20, false, false);

        r1.addBed(new Bed("B1", "R1", BedType.ICU, true, false));
        r1.addBed(new Bed("B2", "R1", BedType.REGULAR, false, false));
        r2.addBed(new Bed("B3", "R2", BedType.REGULAR, false, false));
        r2.addBed(new Bed("B4", "R2", BedType.BARIATRIC, false, false));
        r3.addBed(new Bed("B5", "R3", BedType.REGULAR, false, false));

        Department d = new Department("D1", "Internal", List.of(r1, r2, r3), new ArrayList<>());
        d.getWaitingList().add(waiting("W1", RiskLevel.IMMUNO_COMPROMISED, 9, Instant.parse("2026-03-01T10:00:00Z")));
        d.getWaitingList().add(waiting("W2", RiskLevel.UNKNOWN, 6, Instant.parse("2026-03-01T09:00:00Z")));
        d.getWaitingList().add(waiting("W3", RiskLevel.RESPIRATORY, 7, Instant.parse("2026-03-01T10:30:00Z")));
        return d;
    }

    public static Patient waiting(String id, RiskLevel risk, int severity, Instant admittedAt) {
        Patient p = new Patient(id, null, new ClinicalData(risk, severity, false, null), admittedAt, false);
        p.setStatus(PatientStatus.WAITING);
        return p;
    }

    public static Map<String, Patient> patientIndex(Department department) {
        Map<String, Patient> byId = new LinkedHashMap<>();
        for (Patient p : department.getWaitingList()) {
            byId.put(p.getId(), p);
        }
        return byId;
    }
}
