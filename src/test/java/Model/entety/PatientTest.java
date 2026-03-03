package Model.entety;

import Model.enums.PatientStatus;
import org.junit.jupiter.api.Test;

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
        assertFalse(p.equals(null));
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
}
