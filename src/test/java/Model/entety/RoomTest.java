package Model.entety;

import exception.CapacityExceededException;
import Model.enums.BedType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    @Test
    void addBed_whenUnderCapacity_addsBed() {
        Room room = new Room("R1", "D1", 2, new ArrayList<>(), 0, false);
        Bed b1 = new Bed("B1", "R1", BedType.REGULAR, false);
        room.addBed(b1);
        assertTrue(room.getBeds().contains(b1));
        assertEquals(1, room.getBeds().size());
    }

    @Test
    void addBed_whenAtCapacity_throwsCapacityExceededException() {
        Room room = new Room("R1", "D1", 1, new ArrayList<>(), 0, false);
        room.addBed(new Bed("B1", "R1", BedType.REGULAR, false));
        assertThrows(CapacityExceededException.class, () ->
            room.addBed(new Bed("B2", "R1", BedType.REGULAR, false)));
    }

    @Test
    void constructor_withNullBeds_usesEmptyList() {
        Room room = new Room("R1", "D1", 2, null, 0, false);
        assertNotNull(room.getBeds());
        assertTrue(room.getBeds().isEmpty());
    }

    @Test
    void setBeds_whenSizeExceedsCapacity_throwsIllegalArgumentException() {
        Room room = new Room("R1", "D1", 1, new ArrayList<>(), 0, false);
        List<Bed> tooMany = List.of(
            new Bed("B1", "R1", BedType.REGULAR, false),
            new Bed("B2", "R1", BedType.REGULAR, false));
        assertThrows(IllegalArgumentException.class, () -> room.setBeds(tooMany));
    }
}
