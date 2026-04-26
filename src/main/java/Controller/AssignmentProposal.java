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
        boolean stoppedByTimeLimit
) {
    public double deltaZ() {
        return proposedZ - baselineZ;
    }
}
