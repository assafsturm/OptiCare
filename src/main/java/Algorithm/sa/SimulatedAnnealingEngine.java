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

    /**
     * @param warmStartState      current candidate after greedy (mutated in place during the run)
     * @param baselineForTransfer fixed snapshot for {@link CostCalculator} transfer term (typically copy of warm start at entry)
     */
    public SaResult run(Department department, Map<String, Patient> patientById,
                        AssignmentState warmStartState, AssignmentState baselineForTransfer,
                        CostCalculator calculator, AlgorithmConfig config, HardConstraints hardConstraints) {
        AlgorithmTrace.log("sa", "Starting SA with seed=" + config.getRandomSeed()
                + ", maxIterations=" + config.getMaxTotalIterations()
                + ", initialTemperature=" + config.getInitialTemperature());
        Random rng = new Random(config.getRandomSeed());
        RandomLegalNeighborSampler sampler = new RandomLegalNeighborSampler(
                department, hardConstraints, config.getNeighborSampleAttemptsPerIteration());

        double zCurrent = calculator.computeZ(warmStartState, department, patientById, baselineForTransfer);
        double zBest = zCurrent;
        AssignmentState best = new AssignmentState(warmStartState);
        AlgorithmTrace.log("sa", "Initial energy zCurrent=zBest=" + zCurrent);

        double t = config.getInitialTemperature();
        int iter = 0;
        int noImprove = 0;
        long start = System.currentTimeMillis();
        long timeLimit = config.getMaxTimeMillis();
        boolean stoppedByTime = false;

        boolean stopOuter = false;
        while (!stopOuter && iter < config.getMaxTotalIterations() && t >= config.getMinTemperature()) {
            AlgorithmTrace.log("sa", "Temperature loop start: T=" + t + ", iter=" + iter + ", zBest=" + zBest);
            if (Thread.currentThread().isInterrupted()) {
                AlgorithmTrace.log("sa", "Stopping SA because thread was interrupted.");
                stopOuter = true;
            } else if (timeLimit > 0 && System.currentTimeMillis() - start > timeLimit) {
                stoppedByTime = true;
                AlgorithmTrace.log("sa", "Stopping SA due to time limit.");
                stopOuter = true;
            } else {
                int k = 0;
                boolean innerDone = false;
                while (!innerDone && k < config.getIterationsPerTemperature() && iter < config.getMaxTotalIterations()) {
                    if (Thread.currentThread().isInterrupted()) {
                        AlgorithmTrace.log("sa", "Inner loop interrupted.");
                        innerDone = true;
                    } else if (timeLimit > 0 && System.currentTimeMillis() - start > timeLimit) {
                        stoppedByTime = true;
                        AlgorithmTrace.log("sa", "Inner loop stopped by time limit.");
                        innerDone = true;
                    } else {
                        iter++;
                        NeighborMove move = sampler.sample(rng, warmStartState, patientById);
                        if (move != null) {
                            NeighborMoveExecutor.UndoToken undo = executor.apply(move, warmStartState, department, patientById);
                            double zNew = calculator.computeZ(warmStartState, department, patientById, baselineForTransfer);
                            double delta = zNew - zCurrent;
                            boolean accept = delta <= 0.0 || rng.nextDouble() < Math.exp(-delta / t);
                            if (accept) {
                                zCurrent = zNew;
                                if (iter <= 20 || iter % 200 == 0) {
                                    AlgorithmTrace.log("sa", "Accepted move at iter=" + iter
                                            + ", delta=" + delta + ", zCurrent=" + zCurrent);
                                }
                                if (zCurrent < zBest) {
                                    zBest = zCurrent;
                                    best = new AssignmentState(warmStartState);
                                    noImprove = 0;
                                    AlgorithmTrace.log("sa", "New best found at iter=" + iter + ", zBest=" + zBest);
                                } else {
                                    noImprove++;
                                }
                            } else {
                                executor.undo(undo, warmStartState, department, patientById);
                                if (iter <= 20 || iter % 200 == 0) {
                                    AlgorithmTrace.log("sa", "Rejected move at iter=" + iter
                                            + ", delta=" + delta + ", T=" + t);
                                }
                                noImprove++;
                            }
                            SaResult cutoff = cutoffIfConfigured(config, best, zBest, iter, t, stoppedByTime,
                                    noImprove);
                            if (cutoff != null) {
                                AlgorithmTrace.log("sa", "Early cutoff triggered at iter=" + iter
                                        + ", zBest=" + zBest + ", noImprove=" + noImprove);
                                return cutoff;
                            }
                        }
                        k++;
                    }
                }
                if (stoppedByTime) {
                    stopOuter = true;
                } else {
                    t *= config.getCoolingRate();
                }
            }
        }
        AlgorithmTrace.log("sa", "SA complete. iterations=" + iter + ", zBest=" + zBest
                + ", finalTemperature=" + t + ", stoppedByTime=" + stoppedByTime);
        return new SaResult(best, zBest, iter, t, stoppedByTime);
    }

    private static SaResult cutoffIfConfigured(AlgorithmConfig config, AssignmentState best, double zBest, int iter,
                                               double temperature, boolean stoppedByTime, int noImproveSteps) {
        if (config.getTargetEnergyThreshold() > 0.0 && zBest <= config.getTargetEnergyThreshold()) {
            return new SaResult(best, zBest, iter, temperature, stoppedByTime);
        }
        int cap = config.getNoImprovementStepsToStop();
        if (cap > 0 && noImproveSteps >= cap) {
            return new SaResult(best, zBest, iter, temperature, stoppedByTime);
        }
        return null;
    }
}
