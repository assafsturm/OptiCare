package Algorithm.neighborhood;

import java.util.Objects;

/**
 * Immutable move descriptor for neighborhood generation.
 * This object carries only identifiers so it is safe to log/hash/undo.
 */
public final class NeighborMove {

    private final MoveType type;
    private final String patientIdA;
    private final String patientIdB;
    private final String fromBedIdA;
    private final String toBedIdA;
    private final String fromBedIdB;
    private final String toBedIdB;

    private NeighborMove(MoveType type, String patientIdA, String patientIdB,
                         String fromBedIdA, String toBedIdA, String fromBedIdB, String toBedIdB) {
        this.type = Objects.requireNonNull(type, "type");
        this.patientIdA = patientIdA;
        this.patientIdB = patientIdB;
        this.fromBedIdA = fromBedIdA;
        this.toBedIdA = toBedIdA;
        this.fromBedIdB = fromBedIdB;
        this.toBedIdB = toBedIdB;
    }

    public static NeighborMove assign(String patientId, String toBedId) {
        return new NeighborMove(MoveType.ASSIGN, patientId, null, null, toBedId, null, null);
    }

    public static NeighborMove move(String patientId, String fromBedId, String toBedId) {
        return new NeighborMove(MoveType.MOVE, patientId, null, fromBedId, toBedId, null, null);
    }

    public static NeighborMove swap(String patientIdA, String patientIdB, String fromBedIdA, String fromBedIdB) {
        return new NeighborMove(MoveType.SWAP, patientIdA, patientIdB, fromBedIdA, fromBedIdB, fromBedIdB, fromBedIdA);
    }

    public MoveType getType() { return type; }
    public String getPatientIdA() { return patientIdA; }
    public String getPatientIdB() { return patientIdB; }
    public String getFromBedIdA() { return fromBedIdA; }
    public String getToBedIdA() { return toBedIdA; }
    public String getFromBedIdB() { return fromBedIdB; }
    public String getToBedIdB() { return toBedIdB; }
}
