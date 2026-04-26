package Algorithm.topology;

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
    /** fromRoomId -> (toRoomId -> shortest path distance). */
    private Map<String, Map<String, Double>> allPairsShortestPaths = new HashMap<>();

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
        if (out == null) return 0;
        Double weight = out.get(toRoomId);
        return weight == null ? 0.0 : weight;
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

    /**
     * Computes all-pairs shortest paths with Floyd-Warshall.
     * Time O(V^3), one-time precomputation intended at startup.
     */
    public void precomputeAllPairsShortestPaths() {
        final double inf = Double.POSITIVE_INFINITY;
        allPairsShortestPaths = new HashMap<>();

        for (String i : nodeIds) {
            Map<String, Double> row = new HashMap<>();
            allPairsShortestPaths.put(i, row);
            for (String j : nodeIds) {
                row.put(j, i.equals(j) ? 0.0 : inf);
            }
        }

        for (Map.Entry<String, Map<String, Double>> fromEntry : adjacency.entrySet()) {
            String from = fromEntry.getKey();
            Map<String, Double> row = allPairsShortestPaths.get(from);
            if (row == null) continue;
            for (Map.Entry<String, Double> edge : fromEntry.getValue().entrySet()) {
                Double w = edge.getValue();
                if (w == null) continue;
                String to = edge.getKey();
                row.put(to, Math.min(row.getOrDefault(to, inf), w));
            }
        }

        for (String k : nodeIds) {
            for (String i : nodeIds) {
                double dik = allPairsShortestPaths.get(i).getOrDefault(k, inf);
                if (!Double.isFinite(dik)) continue;
                for (String j : nodeIds) {
                    double dkj = allPairsShortestPaths.get(k).getOrDefault(j, inf);
                    if (!Double.isFinite(dkj)) continue;
                    double candidate = dik + dkj;
                    double current = allPairsShortestPaths.get(i).getOrDefault(j, inf);
                    if (candidate < current) {
                        allPairsShortestPaths.get(i).put(j, candidate);
                    }
                }
            }
        }
    }

    /**
     * Returns shortest-path distance if precomputed and reachable, otherwise +INF.
     */
    public double getShortestPathDistance(String fromRoomId, String toRoomId) {
        if (fromRoomId == null || toRoomId == null) return Double.POSITIVE_INFINITY;
        Map<String, Double> row = allPairsShortestPaths.get(fromRoomId);
        if (row == null) return Double.POSITIVE_INFINITY;
        return row.getOrDefault(toRoomId, Double.POSITIVE_INFINITY);
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
