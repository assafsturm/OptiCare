package Algorithm.neighborhood;

import Model.entety.Bed;
import Model.enums.BedType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedEquivalenceTest {

    @Test
    void sameRoomSameSignature_areEquivalent() {
        Bed a = new Bed("B1", "R1", BedType.REGULAR, false, false);
        Bed b = new Bed("B2", "R1", BedType.REGULAR, false, false);
        assertTrue(BedEquivalence.areEquivalent(a, b));
    }

    @Test
    void differentRoom_notEquivalent() {
        Bed a = new Bed("B1", "R1", BedType.REGULAR, false, false);
        Bed b = new Bed("B2", "R2", BedType.REGULAR, false, false);
        assertFalse(BedEquivalence.areEquivalent(a, b));
    }
}
