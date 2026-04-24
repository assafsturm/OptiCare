package Algorithm.feasibility;

import Algorithm.AssignmentState;
import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.RiskLevel;
import Model.policy.PatientRiskPolicy;
import Algorithm.risk.RiskMatrix;

import java.util.Map;
import java.util.Objects;

/**
 * Shared hard-constraint checks for feasibility and legal neighbor generation.
 */
public final class HardConstraints {

    private final RiskMatrix riskMatrix;
    private final Department department;

    public HardConstraints(RiskMatrix riskMatrix, Department department) {
        this.riskMatrix = Objects.requireNonNull(riskMatrix);
        this.department = Objects.requireNonNull(department);
    }

    /** Same rules as legacy {@link FeasibilityChecker} bed legality (clinical / isolation). */
    public boolean isBedClinicallyLegal(Patient patient, Bed bed) {
        if (bed == null || bed.isBroken()) return false;
        Room room = department.findRoomById(bed.getRoomId());
        ClinicalData cd = patient != null ? patient.getClinicalData() : null;
        if (cd != null && cd.getRequiredBedType() != null && !cd.getRequiredBedType().equals(bed.getType())) return false;
        if (cd != null && cd.needsBariatricBed() && bed.getType() != BedType.BARIATRIC) return false;
        if (cd != null && cd.isNeedsVentilator() && !bed.isHasVentilator()) return false;
        return !PatientRiskPolicy.requiresNegativePressureRoom(patient) || (room != null && room.isHasNegativePressure());
    }

    /**
     * Cohort hard constraints vs current occupants of {@code room}, excluding {@code ignorePatientId} if present in state.
     */
    public boolean isCohortLegalInRoom(Patient patient, Room room, AssignmentState state,
                                       String ignorePatientId, Map<String, Patient> patientById) {
        if (room == null || patient == null) return false;
        RiskLevel rp = PatientRiskPolicy.effectiveRiskLevelForCohorting(patient);
        for (Bed b : room.getBeds()) {
            String pid = state.getPatientIdInBed(b);
            if (pid == null) continue;
            if (ignorePatientId != null && ignorePatientId.equals(pid)) continue;
            Patient other = patientById != null ? patientById.get(pid) : null;
            RiskLevel ro = PatientRiskPolicy.effectiveRiskLevelForCohorting(other);
            if (riskMatrix.isForbiddenCohortPair(rp, ro)) return false;
        }
        return true;
    }

    /**
     * Full hard check: clinical bed fit + no forbidden cohort in target room. Bed must be free for assign/move-to-free.
     */
    public boolean isLegalAssignOrMoveToFreeBed(Patient patient, Bed targetBed, AssignmentState state,
                                              Map<String, Patient> patientById) {
        if (patient == null || targetBed == null) return false;
        if (state.isBedOccupied(targetBed)) return false;
        if (!isBedClinicallyLegal(patient, targetBed)) return false;
        Room room = department.findRoomById(targetBed.getRoomId());
        return isCohortLegalInRoom(patient, room, state, patient.getId(), patientById);
    }

    /** Clinical + cohort validity for a patient already placed on {@code bed} in {@code state}. */
    public boolean isCurrentAssignmentHardValid(Patient patient, Bed bed, AssignmentState state,
                                                Map<String, Patient> patientById) {
        if (patient == null || bed == null) return false;
        if (!isBedClinicallyLegal(patient, bed)) return false;
        Room room = department.findRoomById(bed.getRoomId());
        return isCohortLegalInRoom(patient, room, state, patient.getId(), patientById);
    }

    /** True if every assignment in {@code state} satisfies hard constraints. */
    public boolean isAssignmentStateGloballyValid(AssignmentState state, Map<String, Patient> patientById) {
        if (state == null) return true;
        for (Map.Entry<String, Bed> e : state.getAssignments().entrySet()) {
            Patient p = patientById != null ? patientById.get(e.getKey()) : null;
            Bed b = e.getValue();
            if (p == null || b == null) return false;
            if (!isCurrentAssignmentHardValid(p, b, state, patientById)) return false;
        }
        return true;
    }
}
