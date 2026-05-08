package Persistence.dto;

import Model.enums.BedType;
// dto shaped for json 
public final class PersistedBed {

    private String id;
    private String roomId;
    private BedType type;
    private boolean hasVentilator;
    private boolean broken;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public BedType getType() {
        return type;
    }

    public void setType(BedType type) {
        this.type = type;
    }

    public boolean isHasVentilator() {
        return hasVentilator;
    }

    public void setHasVentilator(boolean hasVentilator) {
        this.hasVentilator = hasVentilator;
    }

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }
}
