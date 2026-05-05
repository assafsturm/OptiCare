package Model.entety;

import Model.enums.BedType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BedTest {

    @Test
    void equals_sameId_returnsTrue() {
        Bed b1 = new Bed("B1", "R1", BedType.REGULAR, false);
        Bed b2 = new Bed("B1", "R2", BedType.BARIATRIC, true);
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
    }

    @Test
    void equals_differentId_returnsFalse() {
        Bed b1 = new Bed("B1", "R1", BedType.REGULAR, false);
        Bed b2 = new Bed("B2", "R1", BedType.REGULAR, false);
        assertNotEquals(b1, b2);
    }

    @Test
    void constructor_setsAllFields() {
        Bed b = new Bed("B1", "R1", BedType.ICU, true);
        assertEquals("B1", b.getId());
        assertEquals("R1", b.getRoomId());
        assertEquals(BedType.ICU, b.getType());
        assertTrue(b.isHasVentilator());
        assertFalse(b.isBroken());
    }
}
