package Persistence.dto;

import java.util.ArrayList;
import java.util.List;

public final class PersistedRoom {

    private String id;
    private String departmentId;
    private int capacity;
    private List<PersistedBed> beds = new ArrayList<>();
    private double distanceFromNurseStation;
    private boolean hasNegativePressure;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<PersistedBed> getBeds() {
        return beds;
    }

    public void setBeds(List<PersistedBed> beds) {
        this.beds = beds != null ? beds : new ArrayList<>();
    }

    public double getDistanceFromNurseStation() {
        return distanceFromNurseStation;
    }

    public void setDistanceFromNurseStation(double distanceFromNurseStation) {
        this.distanceFromNurseStation = distanceFromNurseStation;
    }

    public boolean isHasNegativePressure() {
        return hasNegativePressure;
    }

    public void setHasNegativePressure(boolean hasNegativePressure) {
        this.hasNegativePressure = hasNegativePressure;
    }

}
