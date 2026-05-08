package Algorithm.feasibility;

import java.util.Map;
import java.util.Objects;

import Algorithm.AlgorithmTrace;
import Algorithm.AssignmentState;
import Algorithm.risk.RiskMatrix;
import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.RiskLevel;
import Model.policy.PatientRiskPolicy;


// Shared hard constraint checks for feasibility and legal neighbor generation (one source of truth for all hard constraints)


public final class HardConstraints {

    private final RiskMatrix riskMatrix;
    private final Department department;

    public HardConstraints(RiskMatrix riskMatrix, Department department) {
        this.riskMatrix = Objects.requireNonNull(riskMatrix);
        this.department = Objects.requireNonNull(department); // enforces required constructor not null 
        AlgorithmTrace.log("hard-constraints", "Initialized for department=" + department.getId()); // logs
    }

    //Checks if a bed is clinically suitable for a patient
    public boolean isBedClinicallyLegal(Patient patient, Bed bed) {
        if (bed == null || bed.isBroken()) return false;
        Room room = department.findRoomById(bed.getRoomId());
        ClinicalData cd = patient != null ? patient.getClinicalData() : null; 
        if (cd != null && cd.getRequiredBedType() != null && !cd.getRequiredBedType().equals(bed.getType())) return false; // required bed type matches
        if (cd != null && cd.needsBariatricBed() && bed.getType() != BedType.BARIATRIC) return false; // bariatric bed required
        if (cd != null && cd.isNeedsVentilator() && !bed.isHasVentilator()) return false; // is ventilator required
        return !PatientRiskPolicy.requiresNegativePressureRoom(patient) || (room != null && room.isHasNegativePressure());
    } // bed levle

//Checks if placing patient into room would violate forbidden risk pairing with current occupants.
//ignorePatientId is used during moves rechecks so no compare patient against themselves
    public boolean isCohortLegalInRoom(Patient patient, Room room, AssignmentState state,
                                       String ignorePatientId, Map<String, Patient> patientById) {
        if (room == null || patient == null) return false;
        RiskLevel rp = PatientRiskPolicy.effectiveRiskLevelForCohorting(patient);
        for (Bed b : room.getBeds()) {
            String pid = state.getPatientIdInBed(b);
            if (pid != null && (ignorePatientId == null || !ignorePatientId.equals(pid))) {
                Patient other = patientById != null ? patientById.get(pid) : null;
                RiskLevel ro = PatientRiskPolicy.effectiveRiskLevelForCohorting(other);
                if (riskMatrix.isForbiddenCohortPair(rp, ro)) {
                    return false;
                }
            }
        }
        return true;
    }// room level

//Full legal check for assign or move to free bed ( is bed free and clinically legal and cohort legal)
    public boolean isLegalAssignOrMoveToFreeBed(Patient patient, Bed targetBed, AssignmentState state,
                                              Map<String, Patient> patientById) {
        if (patient == null || targetBed == null) return false;
        if (state.isBedOccupied(targetBed)) return false;
        if (!isBedClinicallyLegal(patient, targetBed)) return false;
        Room room = department.findRoomById(targetBed.getRoomId());
        return isCohortLegalInRoom(patient, room, state, patient.getId(), patientById);
    }// action level

//Checks if the patient is legally assigned to the bed (already assigned to a bed in the current state) (clinical and cohort legal)
    public boolean isCurrentAssignmentHardValid(Patient patient, Bed bed, AssignmentState state,
                                                Map<String, Patient> patientById) {
        if (patient == null || bed == null) return false;
        if (!isBedClinicallyLegal(patient, bed)) return false;
        Room room = department.findRoomById(bed.getRoomId());
        return isCohortLegalInRoom(patient, room, state, patient.getId(), patientById);
    } 

    // Checks if the entire assignment state is globally valid (all assignments are hard valid)
    public boolean isAssignmentStateGloballyValid(AssignmentState state, Map<String, Patient> patientById) {
        if (state == null) return true;
        for (Map.Entry<String, Bed> e : state.getAssignments().entrySet()) {
            Patient p = patientById != null ? patientById.get(e.getKey()) : null;
            Bed b = e.getValue();
            if (p == null || b == null) return false;
            if (!isCurrentAssignmentHardValid(p, b, state, patientById)) return false;
        }
        return true;
    }// state level
}
