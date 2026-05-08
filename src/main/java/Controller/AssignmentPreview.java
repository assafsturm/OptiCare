package Controller;

import java.util.List;


// record for the preview UI
// has the changes and sa stat run
public record AssignmentPreview(
        List<PatientAssignmentDiff> patientDiffs,
        int changedPatients,
        int unchangedPatients,
        double baselineZ,
        double proposedZ
) {
    public double deltaZ() {
        return proposedZ - baselineZ;
    }
}
