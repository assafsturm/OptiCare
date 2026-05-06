package Algorithm.feasibility;

import Algorithm.AssignmentState;
import Algorithm.AlgorithmTrace;
import Algorithm.risk.RiskMatrixFactory;
import Config.AlgorithmConfig;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.enums.PatientStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pre-flight checks before optimization: enough beds for everyone who needs one,
 * and current baseline assignments must satisfy hard constraints.
 * Per-waiting-patient direct-bed checks are intentionally omitted so SA can resolve
 * placements via assign/move/swap.
 */
public class FeasibilityChecker {

    private final AlgorithmConfig config;

    public FeasibilityChecker(AlgorithmConfig config) {
        this.config = config;
    }

    /**
     * Feasibility for assigning all {@link PatientStatus#WAITING} patients on the waiting list
     * (excluding {@link Patient#isTemporarilyUnavailable()}) plus any already in {@code currentState}.
     */
    public FeasibilityResult check(Department department, Map<String, Patient> patientById, AssignmentState currentState) {
        if (department == null) {
            return new FeasibilityResult(false, List.of("Department is null"));
        }
        AlgorithmTrace.log("feasibility", "Checking feasibility for department=" + department.getId());
        HardConstraints hc = new HardConstraints(RiskMatrixFactory.fromConfig(config), department);
        List<String> violations = new ArrayList<>();
        List<Bed> allBeds = department.getAllBeds();
        List<Patient> eligibleWaiting = new ArrayList<>();
        for (Patient p : department.getWaitingList()) {
            if (p != null && p.getStatus() == PatientStatus.WAITING && !p.isTemporarilyUnavailable()
                    && (currentState == null || currentState.getBed(p.getId()) == null)) {
                eligibleWaiting.add(p);
            }
        }

        int totalNeedingBeds = (currentState != null ? currentState.size() : 0) + eligibleWaiting.size();
        int totalBeds = allBeds.size();
        AlgorithmTrace.log("feasibility", "Capacity snapshot: assignedNow="
                + (currentState != null ? currentState.size() : 0)
                + ", eligibleWaiting=" + eligibleWaiting.size()
                + ", totalBeds=" + totalBeds);
        if (totalNeedingBeds > totalBeds) {
            violations.add("Not enough beds: " + totalNeedingBeds + " patients, " + totalBeds + " beds");
        }

        if (currentState != null) {
            for (Map.Entry<String, Bed> e : currentState.getAssignments().entrySet()) {
                String pid = e.getKey();
                Bed bed = e.getValue();
                Patient p = patientById != null ? patientById.get(pid) : null;
                if (p != null && bed != null && !hc.isCurrentAssignmentHardValid(p, bed, currentState, patientById)) {
                    AlgorithmTrace.log("feasibility", "Invalid current assignment detected: patient=" + pid
                            + ", bed=" + bed.getId());
                    violations.add("Current assignment invalid: patient " + pid + " in bed " + bed.getId()
                            + " violates hard constraints");
                }
            }
        }
        AlgorithmTrace.log("feasibility", violations.isEmpty()
                ? "Feasibility passed."
                : "Feasibility failed. Violations=" + violations.size());
        return violations.isEmpty() ? FeasibilityResult.feasible() : new FeasibilityResult(false, violations);
    }
}
