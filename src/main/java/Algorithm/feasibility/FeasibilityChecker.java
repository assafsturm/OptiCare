package Algorithm.feasibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Algorithm.AlgorithmTrace;
import Algorithm.AssignmentState;
import Algorithm.risk.RiskMatrixFactory;
import Config.AlgorithmConfig;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.enums.PatientStatus;

// pre run checks before any optimization is run
// checks if the current state is feasible and if there are enough beds legal for all patients
public class FeasibilityChecker {

    private final AlgorithmConfig config;

    public FeasibilityChecker(AlgorithmConfig config) {
        this.config = config;
    }

//Feasibility for assigning all waiting patients on the waiting list (excluding temporarily unavailable) plus any already in currentState
    public FeasibilityResult check(Department department, Map<String, Patient> patientById, AssignmentState currentState) {
        if (department == null) {
            return new FeasibilityResult(false, List.of("Department is null"));
        }
        AlgorithmTrace.log("feasibility", "Checking feasibility for department=" + department.getId());
        HardConstraints hc = new HardConstraints(RiskMatrixFactory.fromConfig(config), department);
        List<String> violations = new ArrayList<>();
        List<Bed> allBeds = department.getAllBeds();
        List<Patient> eligibleWaiting = new ArrayList<>();
        for (Patient p : department.getWaitingList()) { // Build eligibleWaiting List (waiting and not temporarily unavailable and not currently assigned to a bed)
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
        if (totalNeedingBeds > totalBeds) { // if there are more patients than beds, return false
            violations.add("Not enough beds: " + totalNeedingBeds + " patients, " + totalBeds + " beds");
        }

        if (currentState != null) { // check if the current state is feasible
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
