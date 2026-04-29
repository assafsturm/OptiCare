package Algorithm.sa;

import Algorithm.AssignmentState;

/**
 * Immutable progress snapshot emitted during SA execution for UI observers.
 */
public record SaProgressEvent(
        int iteration,
        double temperature,
        double currentZ,
        double bestZ,
        AssignmentState bestStateSnapshot
) {
}
