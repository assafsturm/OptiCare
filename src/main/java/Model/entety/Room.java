package Model.entety;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String id;
    private String departmentId;
    private int capacity;
    private List<Bed> beds = new ArrayList<>();
    private double distanceFromNurseStation;
    private boolean hasNegativePressure;
    private boolean hasBathroom;

    public Room() {
    }

    public Room(String id, String departmentId, int capacity, List<Bed> beds,
                double distanceFromNurseStation, boolean hasNegativePressure, boolean hasBathroom) {
        this.id = id;
        this.departmentId = departmentId;
        this.capacity = capacity;
        this.beds = beds != null ? beds : new ArrayList<>();
        this.distanceFromNurseStation = distanceFromNurseStation;
        this.hasNegativePressure = hasNegativePressure;
        this.hasBathroom = hasBathroom;
    }

    public void addBed(Bed bed) {
        if (beds.size() < capacity) {
            beds.add(bed);
        } else {
            throw new RuntimeException("Room full");
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<Bed> getBeds() { return beds; }
    public void setBeds(List<Bed> beds) { this.beds = beds != null ? beds : new ArrayList<>(); }

    public double getDistanceFromNurseStation() { return distanceFromNurseStation; }
    public void setDistanceFromNurseStation(double distanceFromNurseStation) { this.distanceFromNurseStation = distanceFromNurseStation; }

    public boolean isHasNegativePressure() { return hasNegativePressure; }
    public void setHasNegativePressure(boolean hasNegativePressure) { this.hasNegativePressure = hasNegativePressure; }

    public boolean isHasBathroom() { return hasBathroom; }
    public void setHasBathroom(boolean hasBathroom) { this.hasBathroom = hasBathroom; }
}
