package Algorithm;

import Algorithm.topology.RoomTopologyGraph;
import Model.entety.Room;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoomTopologyGraphTest {

    @Test
    void addRoom_addsNode() {
        RoomTopologyGraph g = new RoomTopologyGraph();
        g.addRoom("R1");
        assertTrue(g.getRoomIds().contains("R1"));
    }

    @Test
    void addRoom_fromEntity_addsNode() {
        Room r = new Room();
        r.setId("R1");
        RoomTopologyGraph g = new RoomTopologyGraph();
        g.addRoom(r);
        assertTrue(g.getRoomIds().contains("R1"));
    }

    @Test
    void addEdge_setsWeight() {
        RoomTopologyGraph g = new RoomTopologyGraph();
        g.addEdge("R1", "R2", 5.0);
        assertEquals(5.0, g.getWeight("R1", "R2"));
        assertTrue(g.getNeighbors("R1").contains("R2"));
    }

    @Test
    void getWeight_missingEdge_returnsZero() {
        RoomTopologyGraph g = new RoomTopologyGraph();
        g.addRoom("R1");
        g.addRoom("R2");
        assertEquals(0, g.getWeight("R1", "R2"));
    }

    @Test
    void getNeighbors_empty_returnsEmptySet() {
        RoomTopologyGraph g = new RoomTopologyGraph();
        g.addRoom("R1");
        assertTrue(g.getNeighbors("R1").isEmpty());
    }

    @Test
    void fromRooms_buildsGraphWithAllRoomIds() {
        Room r1 = new Room();
        r1.setId("R1");
        Room r2 = new Room();
        r2.setId("R2");
        RoomTopologyGraph g = RoomTopologyGraph.fromRooms(List.of(r1, r2));
        Set<String> ids = g.getRoomIds();
        assertTrue(ids.contains("R1"));
        assertTrue(ids.contains("R2"));
    }

    @Test
    void fromRooms_withNull_doesNotThrow() {
        RoomTopologyGraph g = RoomTopologyGraph.fromRooms(null);
        assertTrue(g.getRoomIds().isEmpty());
    }
}
