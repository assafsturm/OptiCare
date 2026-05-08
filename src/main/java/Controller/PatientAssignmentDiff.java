package Controller;


// record for the patient assignment diff, helps with the preview UI
public record PatientAssignmentDiff(
        String patientId,
        String fromBedId,
        String toBedId,
        AssignmentChangeType changeType
) {
}
