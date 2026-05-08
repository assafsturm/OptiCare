package Model.enums;

/**
 * Patient lifecycle state for assignment and dynamics.
 * WAITING → ASSIGNED → DISCHARGED.
 */
public enum PatientStatus {
    // on waiting list, not yet assigned to a bed.
    WAITING,

    // currently assigned to a bed.
    ASSIGNED,

    // Released removed from assignment and bed freed.
    DISCHARGED
}
