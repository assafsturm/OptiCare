package Algorithm.cost;

import Algorithm.AssignmentState;
import Config.AlgorithmConfig;
import Model.entety.Department;
import Model.entety.Patient;
import Model.enums.PatientStatus;

import java.util.Map;

/**
 * C_unassigned = W_unassigned × count of waiting patients eligible for assignment but not in {@link AssignmentState}.
 * Excludes {@link Patient#isTemporarilyUnavailable()}.
 */
public class UnassignedCostStrategy implements CostStrategy {

    private final AlgorithmConfig config;

    public UnassignedCostStrategy(AlgorithmConfig config) {
        this.config = config;
    }

    @Override
    public double computeTotal(AssignmentState state, Department department,
                               Map<String, Patient> patientById, AssignmentState initialState) {
        int waitingCount = countUnassignedEligibleWaiting(department, state);
        return config.getUnassignedPenaltyWeight() * waitingCount;
    }

    public static int countUnassignedEligibleWaiting(Department department, AssignmentState state) {
        if (department == null || department.getWaitingList() == null) return 0;
        int c = 0;
        for (Patient p : department.getWaitingList()) {
            if (p == null) continue;
            if (p.getStatus() != PatientStatus.WAITING) continue;
            if (p.isTemporarilyUnavailable()) continue;
            if (state != null && state.getBed(p.getId()) != null) continue;
            c++;
        }
        return c;
    }
}
