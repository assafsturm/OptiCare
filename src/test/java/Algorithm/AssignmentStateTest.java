package Algorithm;

import Model.entety.Bed;
import Model.entety.Patient;
import Model.enums.BedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentStateTest {

    private AssignmentState state;
    private Patient p1;
    private Bed b1, b2;

    @BeforeEach
    void setUp() {
        state = new AssignmentState();
        p1 = new Patient("P1", null, null);
        b1 = new Bed("B1", "R1", BedType.REGULAR, false, false);
        b2 = new Bed("B2", "R1", BedType.REGULAR, false, false);
    }

    @Test
    void assign_addsPatientToBed() {
        state.assign(p1, b1);
        assertEquals(b1, state.getBed("P1"));
        assertEquals("P1", state.getPatientIdInBed(b1));
        assertTrue(state.isBedOccupied(b1));
        assertEquals(1, state.size());
    }

    @Test
    void unassign_removesAssignment() {
        state.assign(p1, b1);
        state.unassign("P1");
        assertNull(state.getBed("P1"));
        assertNull(state.getPatientIdInBed(b1));
        assertFalse(state.isBedOccupied(b1));
        assertEquals(0, state.size());
    }

    @Test
    void unassign_byPatient_removesAssignment() {
        state.assign(p1, b1);
        state.unassign(p1);
        assertNull(state.getBed("P1"));
        assertFalse(state.isBedOccupied(b1));
    }

    @Test
    void assign_overwritesPreviousBed() {
        state.assign(p1, b1);
        state.assign(p1, b2);
        assertEquals(b2, state.getBed("P1"));
        assertNull(state.getPatientIdInBed(b1));
        assertEquals("P1", state.getPatientIdInBed(b2));
    }

    @Test
    void deepCopy_isIndependent() {
        state.assign(p1, b1);
        AssignmentState copy = new AssignmentState(state);
        copy.unassign("P1");
        assertEquals(b1, state.getBed("P1"));
        assertNull(copy.getBed("P1"));
    }

    @Test
    void assign_withNullPatient_doesNothing() {
        state.assign(null, b1);
        assertEquals(0, state.size());
    }

    @Test
    void assign_withNullBed_doesNothing() {
        state.assign(p1, null);
        assertEquals(0, state.size());
    }

    @Test
    void getBed_withNullId_returnsNull() {
        assertNull(state.getBed((String) null));
    }

    @Test
    void getBed_byPatient_returnsSameAsById() {
        state.assign(p1, b1);
        assertEquals(state.getBed("P1"), state.getBed(p1));
    }

    @Test
    void getAssignments_returnsUnmodifiableMap() {
        state.assign(p1, b1);
        assertThrows(UnsupportedOperationException.class, () ->
            state.getAssignments().put("other", b2));
    }
}
