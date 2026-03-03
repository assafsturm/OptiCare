package Model.entety;

import Model.enums.BedType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    @Test
    void addBed_whenUnderCapacity_addsBed() {
        Room room = new Room("R1", "D1", 2, new ArrayList<>(), 0, false, false);
        Bed b1 = new Bed("B1", "R1", BedType.REGULAR, false, false);
        room.addBed(b1);
        assertTrue(room.getBeds().contains(b1));
        assertEquals(1, room.getBeds().size());
    }

    @Test
    void addBed_whenAtCapacity_throws() {
        Room room = new Room("R1", "D1", 1, new ArrayList<>(), 0, false, false);
        room.addBed(new Bed("B1", "R1", BedType.REGULAR, false, false));
        assertThrows(RuntimeException.class, () ->
            room.addBed(new Bed("B2", "R1", BedType.REGULAR, false, false)));
    }

    @Test
    void constructor_withNullBeds_usesEmptyList() {
        Room room = new Room("R1", "D1", 2, null, 0, false, false);
        assertNotNull(room.getBeds());
        assertTrue(room.getBeds().isEmpty());
    }
}
