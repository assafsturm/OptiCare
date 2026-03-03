package Model.entety;

import Model.enums.BedType;
import java.util.Objects;

public class Bed {
    private String id;
    private String roomId;
    private BedType type;
    private boolean hasVentilator;
    private boolean isBroken;

    public Bed() {
    }

    public Bed(String id, String roomId, BedType type, boolean hasVentilator, boolean isBroken) {
        this.id = id;
        this.roomId = roomId;
        this.type = type;
        this.hasVentilator = hasVentilator;
        this.isBroken = isBroken;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Bed other = (Bed) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

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
