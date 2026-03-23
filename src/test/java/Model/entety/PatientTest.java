package Model.entety;

import Model.enums.PatientStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PatientTest {

    @Test
    void equals_sameId_returnsTrue() {
        Patient p1 = new Patient("P1", null, null);
        Patient p2 = new Patient("P1", null, null);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void equals_differentId_returnsFalse() {
        Patient p1 = new Patient("P1", null, null);
        Patient p2 = new Patient("P2", null, null);
        assertNotEquals(p1, p2);
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        Patient p = new Patient("P1", null, null);
        assertEquals(p, p);
    }

    @Test
    void equals_null_returnsFalse() {
        Patient p = new Patient("P1", null, null);
        assertNotEquals(null, p);
    }

    @Test
    void defaultStatus_isWaiting() {
        Patient p = new Patient();
        assertEquals(PatientStatus.WAITING, p.getStatus());
    }

    @Test
    void setStatus_updatesStatus() {
        Patient p = new Patient("P1", null, null);
        p.setStatus(PatientStatus.ASSIGNED);
        assertEquals(PatientStatus.ASSIGNED, p.getStatus());
    }

    @Test
    void admittedAtAndTemporarilyUnavailable_roundTrip() {
        Patient p = new Patient("P1", null, null);
        Instant admitted = Instant.parse("2026-03-16T10:15:30Z");
        p.setAdmittedAt(admitted);
        p.setTemporarilyUnavailable(true);
        assertEquals(admitted, p.getAdmittedAt());
        assertTrue(p.isTemporarilyUnavailable());
    }

    @Test
    void constructor_withAdmissionAndAvailability_setsFields() {
        Instant admitted = Instant.parse("2026-03-16T10:15:30Z");
        Patient p = new Patient("P1", null, null, admitted, true);
        assertEquals(admitted, p.getAdmittedAt());
        assertTrue(p.isTemporarilyUnavailable());
    }
}
