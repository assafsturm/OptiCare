package Algorithm.sa;

import Algorithm.AssignmentState;
import Algorithm.AlgorithmTrace;
import Algorithm.CostCalculator;
import Algorithm.feasibility.HardConstraints;
import Algorithm.neighborhood.NeighborMove;
import Algorithm.neighborhood.NeighborMoveExecutor;
import Algorithm.neighborhood.RandomLegalNeighborSampler;
import Config.AlgorithmConfig;
import Model.entety.Department;
import Model.entety.Patient;

import java.util.Map;
import java.util.Random;

/**
 * Single working state, in-place apply/undo per proposal, geometric cooling, seeded RNG, best-state tracking.
 */
public final class SimulatedAnnealingEngine {


    private final NeighborMoveExecutor executor = new NeighborMoveExecutor();

    // warmStartState - current candidate after greedy changed in run, calculator - to compute the energy of the state,
    //  config - to get the parameters for the sa, hardConstraints - to check the legality of the moves
    // baselineForTransfer - copy on greedy not bied changed in run, department - the department being optimized, patientById - all patients in the department
    public SaResult run(Department department, Map<String, Patient> patientById,
                        AssignmentState warmStartState, AssignmentState baselineForTransfer,
                        CostCalculator calculator, AlgorithmConfig config, HardConstraints hardConstraints) {
        AlgorithmTrace.log("sa", "Starting SA with seed=" + config.getRandomSeed()
                + ", maxIterations=" + config.getMaxTotalIterations()
                + ", initialTemperature=" + config.getInitialTemperature());
        Random rng = new Random(config.getRandomSeed());// can be with seed or without. 
        RandomLegalNeighborSampler sampler = new RandomLegalNeighborSampler(// sampler to get the candidate moves
                department, hardConstraints, config.getNeighborSampleAttemptsPerIteration());

        double zCurrent = calculator.computeZ(warmStartState, department, patientById, baselineForTransfer);// compute the energy of the state after greedy
        double zBest = zCurrent;
        AssignmentState best = new AssignmentState(warmStartState);
        AlgorithmTrace.log("sa", "Initial energy zCurrent=zBest=" + zCurrent);

        double t = config.getInitialTemperature();// temperature starts at the initial temperature
        int iter = 0;// iterations 
        int noImprove = 0;// no improvement 
        long start = System.currentTimeMillis();// start time
        long timeLimit = config.getMaxTimeMillis();// time limit
        boolean stoppedByTime = false;// stopped by time

        boolean stopOuter = false;
        while (!stopOuter && iter < config.getMaxTotalIterations() && t >= config.getMinTemperature()) {// while not stopped by time or iterations or temperature
            AlgorithmTrace.log("sa", "Temperature loop start: T=" + t + ", iter=" + iter + ", zBest=" + zBest);
            if (Thread.currentThread().isInterrupted()) {// like clisong program mid run
                AlgorithmTrace.log("sa", "Stopping SA because thread was interrupted.");
                stopOuter = true;
            } else if (timeLimit > 0 && System.currentTimeMillis() - start > timeLimit) {// if time limit is set and the time has passed the time limit
                stoppedByTime = true;
                AlgorithmTrace.log("sa", "Stopping SA due to time limit.");
                stopOuter = true;
            } else {
                int k = 0;// inner iterations
                boolean innerDone = false;
                // INNER LOOP
                while (!innerDone && k < config.getIterationsPerTemperature() && iter < config.getMaxTotalIterations()) {// while not stopped by time or iterations or temperature
                    if (Thread.currentThread().isInterrupted()) {// like clisong program mid run
                        AlgorithmTrace.log("sa", "Inner loop interrupted.");
                        innerDone = true;
                    } else if (timeLimit > 0 && System.currentTimeMillis() - start > timeLimit) {// if time limit is set and the time has passed the time limit
                        stoppedByTime = true;
                        AlgorithmTrace.log("sa", "Inner loop stopped by time limit.");
                        innerDone = true;
                    } else {
                        iter++;// inc global iterations
                        NeighborMove move = sampler.sample(rng, warmStartState, patientById);// get a candidate move
                        if (move != null) {
                            NeighborMoveExecutor.UndoToken undo = executor.apply(move, warmStartState, department, patientById);// apply the move
                            double zNew = calculator.computeZ(warmStartState, department, patientById, baselineForTransfer);// compute the energy of the state after the move
                            double delta = zNew - zCurrent;// calculate the delta of the energy
                            boolean accept = delta <= 0.0 || rng.nextDouble() < Math.exp(-delta / t);// accept the move if the delta is less than 0 or the probability is less than the exp of the delta divided by the temperature
                            if (accept) {
                                zCurrent = zNew;
                                if (iter <= 20 || iter % 200 == 0) {
                                    AlgorithmTrace.log("sa", "Accepted move at iter=" + iter
                                            + ", delta=" + delta + ", zCurrent=" + zCurrent);
                                }
                                if (zCurrent < zBest) {// if found new global minima
                                    zBest = zCurrent;
                                    best = new AssignmentState(warmStartState);// update the best state
                                    noImprove = 0;// reset no improvement
                                    AlgorithmTrace.log("sa", "New best found at iter=" + iter + ", zBest=" + zBest);
                                } else {
                                    noImprove++;// inc no improvement
                                }
                            } else {
                                executor.undo(undo, warmStartState, department, patientById);// undo the move if not accepted
                                if (iter <= 20 || iter % 200 == 0) {
                                    AlgorithmTrace.log("sa", "Rejected move at iter=" + iter
                                            + ", delta=" + delta + ", T=" + t);
                                }
                                noImprove++;
                            }
                            SaResult cutoff = cutoffIfConfigured(config, best, zBest, iter, t, stoppedByTime,
                                    noImprove);// check if the sa should be stopped
                            if (cutoff != null) {
                                AlgorithmTrace.log("sa", "Early cutoff triggered at iter=" + iter
                                        + ", zBest=" + zBest + ", noImprove=" + noImprove);
                                return cutoff;
                            }
                        }
                        k++;// inc inner iterations
                    }
                }
                // END INNER LOOP
                if (stoppedByTime) {
                    stopOuter = true;
                } else {
                    t *= config.getCoolingRate();// after stebelizing around an energy, cool down
                }
            }
        }
        AlgorithmTrace.log("sa", "SA complete. iterations=" + iter + ", zBest=" + zBest
                + ", finalTemperature=" + t + ", stoppedByTime=" + stoppedByTime);
        return new SaResult(best, zBest, iter, t, stoppedByTime);
    }

    // check if the sa should be stopped
    private static SaResult cutoffIfConfigured(AlgorithmConfig config, AssignmentState best, double zBest, int iter,
                                               double temperature, boolean stoppedByTime, int noImproveSteps) {
        if (config.getTargetEnergyThreshold() > 0.0 && zBest <= config.getTargetEnergyThreshold()) {// stoped dur to found solution with energy less than the target energy - "good enough"
            return new SaResult(best, zBest, iter, temperature, stoppedByTime);
        }
        int cap = config.getNoImprovementStepsToStop();
        if (cap > 0 && noImproveSteps >= cap) {// stopped due to no improvement
            return new SaResult(best, zBest, iter, temperature, stoppedByTime);
        }
        return null;// not stopped
    }
}



// O((I * K * S) + Cz)