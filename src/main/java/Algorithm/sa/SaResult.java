package Algorithm.sa;

import Algorithm.AssignmentState;

/**
 * Outcome of a simulated annealing run: best feasible snapshot and run statistics.
 */
public record SaResult(AssignmentState bestState, double bestZ, int iterations, double finalTemperature,
                       boolean stoppedByTimeLimit) {
}
