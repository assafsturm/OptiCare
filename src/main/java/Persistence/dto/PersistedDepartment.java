package Persistence.dto;

import java.util.ArrayList;
import java.util.List;

// dto shaped for json 
public final class PersistedDepartment {

    private String id;
    private String name;
    private List<String> waitingPatientIds = new ArrayList<>();
    private List<PersistedRoom> rooms = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getWaitingPatientIds() {
        return waitingPatientIds;
    }

    public void setWaitingPatientIds(List<String> waitingPatientIds) {
        this.waitingPatientIds = waitingPatientIds != null ? waitingPatientIds : new ArrayList<>();
    }

    public List<PersistedRoom> getRooms() {
        return rooms;
    }

    public void setRooms(List<PersistedRoom> rooms) {
        this.rooms = rooms != null ? rooms : new ArrayList<>();
    }
}
