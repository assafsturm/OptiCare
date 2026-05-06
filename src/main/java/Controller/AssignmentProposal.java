package Controller;

import Algorithm.AssignmentState;

import java.util.List;

/**
 * Immutable proposal output for Stage-4 workflow "propose assignment".
 */
public record AssignmentProposal(
        boolean feasible,
        List<String> feasibilityViolations,
        AssignmentState baselineState,
        AssignmentState proposedState,
        double baselineZ,
        double proposedZ,
        int iterations,
        boolean stoppedByTimeLimit,
        /** Soft messages e.g. waiting patients still unassigned after SA (feasible runs only). */
        List<String> warnings
) {
    public AssignmentProposal {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public double deltaZ() {
        return proposedZ - baselineZ;
    }
}
