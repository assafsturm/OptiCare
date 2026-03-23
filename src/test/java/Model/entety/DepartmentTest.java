package Model.entety;

import Model.enums.BedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentTest {

    private Department department;
    private Room room1;
    private Room room2;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        department = new Department();
        department.setId("D1");
        department.setName("Internal");

        room1 = new Room("R1", "D1", 2, new ArrayList<>(), 10, false, true);
        room1.getBeds().add(new Bed("B1", "R1", BedType.REGULAR, false, false));
        room1.getBeds().add(new Bed("B2", "R1", BedType.REGULAR, false, false));

        room2 = new Room("R2", "D1", 1, new ArrayList<>(), 20, false, false);
        room2.getBeds().add(new Bed("B3", "R2", BedType.REGULAR, false, false));

        department.setRooms(List.of(room1, room2));
    }

    @Test
    void getAllBeds_returnsAllBedsFromAllRooms() {
        List<Bed> beds = department.getAllBeds();
        assertEquals(3, beds.size());
        assertTrue(beds.stream().anyMatch(b -> "B1".equals(b.getId())));
        assertTrue(beds.stream().anyMatch(b -> "B2".equals(b.getId())));
        assertTrue(beds.stream().anyMatch(b -> "B3".equals(b.getId())));
    }

    @Test
    void getTotalCapacity_sumsRoomCapacities() {
        assertEquals(3, department.getTotalCapacity());
    }

    @Test
    void addRoom_appendsRoom() {
        Department d = new Department("D1", "Test", new ArrayList<>(), new ArrayList<>());
        assertEquals(0, d.getRooms().size());
        d.addRoom(room1);
        assertEquals(1, d.getRooms().size());
        assertTrue(d.getRooms().contains(room1));
    }

    @Test
    void constructor_withNullLists_usesEmptyLists() {
        Department d = new Department("D1", "Test", null, null);
        assertNotNull(d.getRooms());
        assertNotNull(d.getWaitingList());
        assertTrue(d.getRooms().isEmpty());
        assertTrue(d.getWaitingList().isEmpty());
    }

    @Test
    void findRoomById_returnsMatchingRoomOrNull() {
        assertEquals(room1, department.findRoomById("R1"));
        assertNull(department.findRoomById("missing"));
        assertNull(department.findRoomById(null));
    }
}
