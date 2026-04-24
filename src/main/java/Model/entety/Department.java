package Model.entety;

import java.util.ArrayList;
import java.util.List;

public class Department {
    private String id;
    private String name;
    private List<Room> rooms = new ArrayList<>();
    private List<Patient> waitingList = new ArrayList<>();

    public Department() {
    }

    public Department(String id, String name, List<Room> rooms, List<Patient> waitingList) {
        this.id = id;
        this.name = name;
        this.rooms = rooms != null ? rooms : new ArrayList<>();
        this.waitingList = waitingList != null ? waitingList : new ArrayList<>();
    }

    public List<Bed> getAllBeds() {
        List<Bed> allBeds = new ArrayList<>();
        for (Room room : rooms) {
            allBeds.addAll(room.getBeds());
        }
        return allBeds;
    }

    /** Returns the room with the given id, or null if not found. */
    public Room findRoomById(String roomId) {
        if (roomId == null) return null;
        for (Room r : rooms) {
            if (roomId.equals(r.getId())) return r;
        }
        return null;
    }

    /** Returns the bed with the given id in this department, or null if not found. */
    public Bed findBedById(String bedId) {
        if (bedId == null) return null;
        for (Room r : rooms) {
            for (Bed b : r.getBeds()) {
                if (bedId.equals(b.getId())) return b;
            }
        }
        return null;
    }

    public int getTotalCapacity() {
        return rooms.stream().mapToInt(Room::getCapacity).sum();
    }

    public void addRoom(Room room) {
        this.rooms.add(room);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms != null ? rooms : new ArrayList<>(); }

    public List<Patient> getWaitingList() { return waitingList; }
    public void setWaitingList(List<Patient> waitingList) { this.waitingList = waitingList != null ? waitingList : new ArrayList<>(); }
}
