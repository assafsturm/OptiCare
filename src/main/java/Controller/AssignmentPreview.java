package Controller;

import java.util.List;

/**
 * Preview/diff summary for approve-reject decision.
 */
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
