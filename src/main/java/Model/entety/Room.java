package Model.entety;

import java.util.ArrayList; // for the capacity exceeded exception
import java.util.List; // for the list of beds

import exception.CapacityExceededException; 

public class Room {
    private String id;
    private String departmentId;
    private int capacity;
    private List<Bed> beds = new ArrayList<>();
    private double distanceFromNurseStation;
    private boolean hasNegativePressure; // used to chohort patients with infectious risk level

    public Room() {
    }

    public Room(String id, String departmentId, int capacity, List<Bed> beds,
                double distanceFromNurseStation, boolean hasNegativePressure) {
        this.id = id;
        this.departmentId = departmentId;
        this.capacity = capacity;
        List<Bed> initial = beds != null ? beds : new ArrayList<>();
        if (initial.size() > capacity) {
            throw new IllegalArgumentException("Beds list size " + initial.size() + " exceeds room capacity " + capacity);
        }
        this.beds = new ArrayList<>(initial);
        this.distanceFromNurseStation = distanceFromNurseStation;
        this.hasNegativePressure = hasNegativePressure;
    }

    public void addBed(Bed bed) {
        if (beds.size() < capacity) {
            beds.add(bed);
        } else {
            throw new CapacityExceededException("Room full");
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<Bed> getBeds() { return beds; }
    // sets the bed list if the size exceeds the capacity throws an exception
    public void setBeds(List<Bed> beds) {
        List<Bed> newBeds = beds != null ? beds : new ArrayList<>();
        if (newBeds.size() > capacity) {
            throw new IllegalArgumentException("Beds list size " + newBeds.size() + " exceeds room capacity " + capacity);
        }
        this.beds = new ArrayList<>(newBeds);
    }

    public double getDistanceFromNurseStation() { return distanceFromNurseStation; }
    public void setDistanceFromNurseStation(double distanceFromNurseStation) { this.distanceFromNurseStation = distanceFromNurseStation; }

    public boolean isHasNegativePressure() { return hasNegativePressure; }
    public void setHasNegativePressure(boolean hasNegativePressure) { this.hasNegativePressure = hasNegativePressure; }

}
