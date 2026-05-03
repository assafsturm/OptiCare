package Algorithm.testsupport;

import Algorithm.AssignmentState;
import Algorithm.CostCalculator;
import Algorithm.feasibility.HardConstraints;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exhaustive search: assign every patient in {@code toAssign} to a distinct bed from {@code candidateBeds},
 * minimize {@link CostCalculator#computeZ} subject to {@link HardConstraints#isAssignmentStateGloballyValid}.
 * Intended only for micro instances ( factorial-style blow-up ).
 */
public final class BruteForceLegalAssignmentSearch {

    /** Outcome of a brute-force sweep. {@code legalCount} excludes invalid constraint states. */
    public record OptimalAssignment(double bestZ, int totalAssignmentsTried, int legalAssignmentCount, boolean foundAnyLegal) {}

    private BruteForceLegalAssignmentSearch() {
    }

    /**
     * @param baselineForTransfer same snapshot used in SA / {@link CostCalculator} transfer term (never null-use empty map baseline with {@code new AssignmentState()}).
     */
    public static OptimalAssignment findMinimumZ(
            Department department,
            Map<String, Patient> patientById,
            List<Patient> toAssign,
            List<Bed> candidateBeds,
            AssignmentState baselineForTransfer,
            CostCalculator calculator,
            HardConstraints hardConstraints
    ) {
        List<Patient> patients = new ArrayList<>(toAssign);
        AssignmentState baseline = baselineForTransfer != null ? baselineForTransfer : new AssignmentState();
        if (patients.size() > candidateBeds.size()) {
            return new OptimalAssignment(Double.POSITIVE_INFINITY, 0, 0, false);
        }
        double[] bestHolder = {Double.POSITIVE_INFINITY};
        int[] tried = {0};
        int[] legal = {0};
        boolean[] bedUsed = new boolean[candidateBeds.size()];
        AssignmentState probe = new AssignmentState();
        dfs(0, patients, candidateBeds, bedUsed, probe, department, patientById, baseline, calculator,
                hardConstraints, bestHolder, tried, legal);
        boolean any = legal[0] > 0 && Double.isFinite(bestHolder[0]);
        return new OptimalAssignment(bestHolder[0], tried[0], legal[0], any);
    }

    private static void dfs(
            int depth,
            List<Patient> patients,
            List<Bed> beds,
            boolean[] bedUsed,
            AssignmentState state,
            Department department,
            Map<String, Patient> patientById,
            AssignmentState baseline,
            CostCalculator calculator,
            HardConstraints hardConstraints,
            double[] bestHolder,
            int[] tried,
            int[] legal
    ) {
        if (depth == patients.size()) {
            tried[0]++;
            if (hardConstraints.isAssignmentStateGloballyValid(state, patientById)) {
                legal[0]++;
                double z = calculator.computeZ(state, department, patientById, baseline);
                if (z < bestHolder[0]) {
                    bestHolder[0] = z;
                }
            }
            return;
        }
        Patient p = patients.get(depth);
        for (int i = 0; i < beds.size(); i++) {
            if (!bedUsed[i]) {
                bedUsed[i] = true;
                state.assign(p, beds.get(i));
                dfs(depth + 1, patients, beds, bedUsed, state, department, patientById, baseline, calculator,
                        hardConstraints, bestHolder, tried, legal);
                state.unassign(p);
                bedUsed[i] = false;
            }
        }
    }
}
