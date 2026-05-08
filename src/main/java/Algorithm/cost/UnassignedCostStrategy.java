package Algorithm.cost;

import Algorithm.AssignmentState;
import Config.AlgorithmConfig;
import Model.entety.Department;
import Model.entety.Patient;
import Model.enums.PatientStatus;

import java.util.Map;

// Cost strategy for the unassigned penalty (waiting patients eligible for assignment but not in the state) 
// for algorithm to prefer assigning waiting patients over not assigning them
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
        // O(w)
    }

    public static int countUnassignedEligibleWaiting(Department department, AssignmentState state) {
        if (department == null || department.getWaitingList() == null) return 0;
        int c = 0;
        for (Patient p : department.getWaitingList()) {
            if (p != null && p.getStatus() == PatientStatus.WAITING && !p.isTemporarilyUnavailable()
                    && (state == null || state.getBed(p.getId()) == null)) {
                c++;
            }
        }
        return c;
    }
}
