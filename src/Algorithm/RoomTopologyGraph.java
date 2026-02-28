package Algorithm;

import Model.entety.Room;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Directed weighted graph over rooms. Nodes = room IDs; edges = proximity/passage;
 * weight = distance or infection coefficient. Used for C_policy distance penalties
 * and neighbor scans. Inside a room, bed positions are not modeled (room is the unit).
 */
public class RoomTopologyGraph {

    /** fromRoomId -> (toRoomId -> weight). */
    private final Map<String, Map<String, Double>> adjacency = new HashMap<>();
    private final Set<String> nodeIds = new HashSet<>();

    public RoomTopologyGraph() {
    }

    /** Add a room as a node by id. */
    public void addRoom(String roomId) {
        if (roomId == null) return;
        nodeIds.add(roomId);
        adjacency.putIfAbsent(roomId, new HashMap<>());
    }

    /** Add a room node from entity. */
    public void addRoom(Room room) {
        if (room != null) addRoom(room.getId());
    }

    /**
     * Add directed edge from -> to with weight (e.g. distance or infection factor).
     * Creates nodes if they don't exist.
     */
    public void addEdge(String fromRoomId, String toRoomId, double weight) {
        if (fromRoomId == null || toRoomId == null) return;
        addRoom(fromRoomId);
        addRoom(toRoomId);
        adjacency.get(fromRoomId).put(toRoomId, weight);
    }

    /** Get edge weight, or 0 if no edge. */
    public double getWeight(String fromRoomId, String toRoomId) {
        if (fromRoomId == null || toRoomId == null) return 0;
        Map<String, Double> out = adjacency.get(fromRoomId);
        return out == null ? 0 : out.getOrDefault(toRoomId, 0.0);
    }

    /** All room IDs (nodes). */
    public Set<String> getRoomIds() {
        return new HashSet<>(nodeIds);
    }

    /** Neighbors of a room (rooms reachable by one directed edge). */
    public Set<String> getNeighbors(String roomId) {
        Map<String, Double> out = adjacency.get(roomId);
        return out == null ? Set.of() : new HashSet<>(out.keySet());
    }

    /** Build graph from a list of rooms; no edges. Call addEdge to set weights. */
    public static RoomTopologyGraph fromRooms(Iterable<Room> rooms) {
        RoomTopologyGraph g = new RoomTopologyGraph();
        if (rooms != null) {
            for (Room r : rooms) g.addRoom(r);
        }
        return g;
    }
}
