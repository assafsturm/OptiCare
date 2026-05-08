package Model.entety;

import java.util.Objects; // enum for bed type

import Model.enums.BedType; // for equals and hashCode

public class Bed {
    private String id; 
    private String roomId;
    private BedType type;
    private boolean hasVentilator; 
    private boolean isBroken;

    public Bed() {
    }

    public Bed(String id, String roomId, BedType type, boolean hasVentilator) {
        this(id, roomId, type, hasVentilator, false);
    } // constructor for bed when it's not broken by default

    public Bed(String id, String roomId, BedType type, boolean hasVentilator, boolean isBroken) { 
        this.id = id;
        this.roomId = roomId;
        this.type = type;
        this.hasVentilator = hasVentilator;
        this.isBroken = isBroken;
    } // constructor for bed when isBroken is provided

    @Override
    public boolean equals(Object obj) { // override equals method to compare beds by id and not by reference
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Bed other = (Bed) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { // override hashCode method to return the hash of the id
        return Objects.hash(id);
    }

// getters and setters for the bed

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public BedType getType() { return type; }
    public void setType(BedType type) { this.type = type; }

    public boolean isHasVentilator() { return hasVentilator; }
    public void setHasVentilator(boolean hasVentilator) { this.hasVentilator = hasVentilator; }

    public boolean isBroken() { return isBroken; }
    public void setBroken(boolean broken) { isBroken = broken; }
}


