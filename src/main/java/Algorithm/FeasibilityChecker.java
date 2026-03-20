package Algorithm;

import Config.AlgorithmConfig;
import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.RiskLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks whether a valid assignment exists: enough beds, and at least one
 * legally assignable bed per patient (hard constraints). Used before/after
 * running the optimizer to report "no valid assignment" and why.
 */
public class FeasibilityChecker {

    private final AlgorithmConfig config;

    public FeasibilityChecker(AlgorithmConfig config) {
        this.config = config;
    }

    /**
     * Check if it is feasible to assign all patients in the waiting list (plus any already assigned)
     * to beds in the department. Returns violations (e.g. "Not enough beds", "No bed for patient X") if not.
     */
    public FeasibilityResult check(Department department, Map<String, Patient> patientById, AssignmentState currentState) {
        List<String> violations = new ArrayList<>();
        List<Bed> allBeds = department.getAllBeds();
        List<Patient> toAssign = new ArrayList<>(department.getWaitingList());
        for (Patient p : toAssign) {
            if (p == null) continue;
            String pid = p.getId();
            if (currentState != null && currentState.getBed(pid) != null) { // already counted in size? No - size is number assigned. So waiting list includes only waiting. So total to place = currentState.size() + waitingList.size()? No - currentState already has some assigned. So "to assign" = we need beds for (currentState.size() + waitingList.size()). Actually: we have currentState with some patients assigned. The "waiting list" are patients not yet assigned. So total patients that need a bed = currentState.size() + toAssign.size(). And we have allBeds.size() beds. So feasibility: currentState.size() + toAssign.size() <= allBeds.size(). And for each patient (in state or waiting), there must exist at least one bed that is legal for them (hard constraints only).
            }
        }
        int totalPatients = (currentState != null ? currentState.size() : 0) + toAssign.size();
        int totalBeds = allBeds.size();
        if (totalPatients > totalBeds) {
            violations.add("Not enough beds: " + totalPatients + " patients, " + totalBeds + " beds");
        }
        for (Patient p : toAssign) {
            if (p == null) continue;
            long legalFreeBeds = allBeds.stream()
                .filter(bed -> (currentState == null || !currentState.isBedOccupied(bed)) && isBedLegalForPatient(p, bed, department))
                .count();
            if (legalFreeBeds == 0) {
                violations.add("No legal bed for patient " + p.getId() + " (constraints: bed type, ventilator, negative pressure, or bariatric)");
            }
        }
        for (Map.Entry<String, Bed> e : currentState != null ? currentState.getAssignments().entrySet() : List.<Map.Entry<String, Bed>>of()) {
            String pid = e.getKey();
            Bed bed = e.getValue();
            Patient p = patientById != null ? patientById.get(pid) : null;
            if (p != null && bed != null && !isBedLegalForPatient(p, bed, department)) {
                violations.add("Current assignment invalid: patient " + pid + " in bed " + bed.getId() + " violates hard constraints");
            }
        }
        return violations.isEmpty() ? FeasibilityResult.feasible() : new FeasibilityResult(false, violations);
    }

    private boolean isBedLegalForPatient(Patient patient, Bed bed, Department department) {
        if (bed.isBroken()) return false;
        Room room = findRoom(department, bed.getRoomId());
        ClinicalData cd = patient != null ? patient.getClinicalData() : null;
        if (cd != null && cd.getRequiredBedType() != null && !cd.getRequiredBedType().equals(bed.getType())) return false;
        if (cd != null && cd.needsBariatricBed() && bed.getType() != BedType.BARIATRIC) return false;
        if (cd != null && cd.isNeedsVentilator() && !bed.isHasVentilator()) return false;
        RiskLevel risk = cd != null && cd.getRiskLevel() != null ? cd.getRiskLevel() : RiskLevel.CLEAN;
        if (risk == RiskLevel.INFECTIOUS && (room == null || !room.isHasNegativePressure())) return false;
        return true;
    }

    private static Room findRoom(Department department, String roomId) {
        if (department == null || roomId == null) return null;
        for (Room r : department.getRooms()) {
            if (roomId.equals(r.getId())) return r;
        }
        return null;
    }
}
