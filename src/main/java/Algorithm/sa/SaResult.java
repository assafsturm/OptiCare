package Algorithm.sa;

import Algorithm.AssignmentState;


// recoerd for the sa result, a data carrier
public record SaResult(AssignmentState bestState, double bestZ, int iterations, double finalTemperature,
                       boolean stoppedByTimeLimit) {
}
