package Algorithm.feasibility;

import Algorithm.AssignmentState;
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
 * Checks whether a valid assignment exists: enough beds, and at least one
 * legally assignable free bed per eligible waiting patient (hard constraints).
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
        HardConstraints hc = new HardConstraints(RiskMatrixFactory.fromConfig(config), department);
        List<String> violations = new ArrayList<>();
        List<Bed> allBeds = department.getAllBeds();
        List<Patient> eligibleWaiting = new ArrayList<>();
        for (Patient p : department.getWaitingList()) {
            if (p == null) continue;
            if (p.getStatus() != PatientStatus.WAITING) continue;
            if (p.isTemporarilyUnavailable()) continue;
            eligibleWaiting.add(p);
        }

        int totalNeedingBeds = (currentState != null ? currentState.size() : 0) + eligibleWaiting.size();
        int totalBeds = allBeds.size();
        if (totalNeedingBeds > totalBeds) {
            violations.add("Not enough beds: " + totalNeedingBeds + " patients, " + totalBeds + " beds");
        }

        for (Patient p : eligibleWaiting) {
            long legalFreeBeds = allBeds.stream()
                    .filter(bed -> (currentState == null || !currentState.isBedOccupied(bed))
                            && hc.isLegalAssignOrMoveToFreeBed(p, bed, currentState, patientById))
                    .count();
            if (legalFreeBeds == 0) {
                violations.add("No legal bed for patient " + p.getId()
                        + " (clinical, isolation, or cohort constraints)");
            }
        }

        if (currentState != null) {
            for (Map.Entry<String, Bed> e : currentState.getAssignments().entrySet()) {
                String pid = e.getKey();
                Bed bed = e.getValue();
                Patient p = patientById != null ? patientById.get(pid) : null;
                if (p != null && bed != null && !hc.isCurrentAssignmentHardValid(p, bed, currentState, patientById)) {
                    violations.add("Current assignment invalid: patient " + pid + " in bed " + bed.getId()
                            + " violates hard constraints");
                }
            }
        }
        return violations.isEmpty() ? FeasibilityResult.feasible() : new FeasibilityResult(false, violations);
    }
}
