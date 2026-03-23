package Algorithm.determinism;

import Algorithm.AssignmentState;
import Model.entety.Bed;
import Model.entety.Patient;
import Model.enums.BedType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AssignmentStateHasherTest {

    @Test
    void sha256_sameAssignmentsDifferentInsertOrder_sameHash() {
        Patient p1 = new Patient("P1", null, null);
        Patient p2 = new Patient("P2", null, null);
        Bed b1 = new Bed("B1", "R1", BedType.REGULAR, false, false);
        Bed b2 = new Bed("B2", "R1", BedType.REGULAR, false, false);

        AssignmentState a = new AssignmentState();
        a.assign(p1, b1);
        a.assign(p2, b2);

        AssignmentState b = new AssignmentState();
        b.assign(p2, b2);
        b.assign(p1, b1);

        assertEquals(AssignmentStateHasher.sha256(a), AssignmentStateHasher.sha256(b));
    }

    @Test
    void sha256_differentAssignments_differentHash() {
        Patient p1 = new Patient("P1", null, null);
        Bed b1 = new Bed("B1", "R1", BedType.REGULAR, false, false);
        Bed b2 = new Bed("B2", "R1", BedType.REGULAR, false, false);

        AssignmentState a = new AssignmentState();
        a.assign(p1, b1);
        AssignmentState b = new AssignmentState();
        b.assign(p1, b2);

        assertNotEquals(AssignmentStateHasher.sha256(a), AssignmentStateHasher.sha256(b));
    }
}
