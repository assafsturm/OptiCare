package Controller;

/**
 * Patient-level assignment diff line for preview UI.
 */
public record PatientAssignmentDiff(
        String patientId,
        String fromBedId,
        String toBedId,
        AssignmentChangeType changeType
) {
}
