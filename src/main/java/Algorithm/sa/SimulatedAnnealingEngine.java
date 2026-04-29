package Algorithm.sa;

import Algorithm.AssignmentState;
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
        return run(department, patientById, warmStartState, baselineForTransfer, calculator, config, hardConstraints, null);
    }

    public SaResult run(Department department, Map<String, Patient> patientById,
                        AssignmentState warmStartState, AssignmentState baselineForTransfer,
                        CostCalculator calculator, AlgorithmConfig config, HardConstraints hardConstraints,
                        SaProgressListener progressListener) {
        Random rng = new Random(config.getRandomSeed());
        RandomLegalNeighborSampler sampler = new RandomLegalNeighborSampler(
                department, hardConstraints, config.getNeighborSampleAttemptsPerIteration());

        double zCurrent = calculator.computeZ(warmStartState, department, patientById, baselineForTransfer);
        double zBest = zCurrent;
        AssignmentState best = new AssignmentState(warmStartState);

        double t = config.getInitialTemperature();
        int iter = 0;
        int noImprove = 0;
        long start = System.currentTimeMillis();
        long lastSnapshotAt = start;
        long timeLimit = config.getMaxTimeMillis();
        boolean stoppedByTime = false;

        while (iter < config.getMaxTotalIterations() && t >= config.getMinTemperature()) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            if (timeLimit > 0 && System.currentTimeMillis() - start > timeLimit) {
                stoppedByTime = true;
                break;
            }
            for (int k = 0; k < config.getIterationsPerTemperature() && iter < config.getMaxTotalIterations(); k++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (timeLimit > 0 && System.currentTimeMillis() - start > timeLimit) {
                    stoppedByTime = true;
                    break;
                }
                iter++;
                NeighborMove move = sampler.sample(rng, warmStartState, patientById);
                if (move == null) {
                    continue;
                }
                NeighborMoveExecutor.UndoToken undo = executor.apply(move, warmStartState, department, patientById);
                double zNew = calculator.computeZ(warmStartState, department, patientById, baselineForTransfer);
                double delta = zNew - zCurrent;
                boolean accept = delta <= 0.0 || rng.nextDouble() < Math.exp(-delta / t);
                if (accept) {
                    zCurrent = zNew;
                    if (zCurrent < zBest) {
                        zBest = zCurrent;
                        best = new AssignmentState(warmStartState);
                        noImprove = 0;
                    } else {
                        noImprove++;
                    }
                } else {
                    executor.undo(undo, warmStartState, department, patientById);
                    noImprove++;
                }
                if (config.getTargetEnergyThreshold() > 0.0 && zBest <= config.getTargetEnergyThreshold()) {
                    publishProgress(progressListener, iter, t, zCurrent, zBest, best);
                    return new SaResult(best, zBest, iter, t, stoppedByTime);
                }
                if (config.getNoImprovementStepsToStop() > 0
                        && noImprove >= config.getNoImprovementStepsToStop()) {
                    publishProgress(progressListener, iter, t, zCurrent, zBest, best);
                    return new SaResult(best, zBest, iter, t, stoppedByTime);
                }
                long now = System.currentTimeMillis();
                if (progressListener != null && now - lastSnapshotAt >= Math.max(1L, config.getSaProgressSnapshotCadenceMillis())) {
                    publishProgress(progressListener, iter, t, zCurrent, zBest, best);
                    lastSnapshotAt = now;
                }
            }
            if (stoppedByTime) break;
            t *= config.getCoolingRate();
        }
        publishProgress(progressListener, iter, t, zCurrent, zBest, best);
        return new SaResult(best, zBest, iter, t, stoppedByTime);
    }

    private static void publishProgress(SaProgressListener listener, int iteration, double temperature,
                                        double currentZ, double bestZ, AssignmentState bestState) {
        if (listener == null) return;
        listener.onProgress(new SaProgressEvent(
                iteration,
                temperature,
                currentZ,
                bestZ,
                new AssignmentState(bestState)
        ));
    }
}
