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
import java.util.Objects;

/**
 * Computes the total cost Z and its components (C_safety, C_clinical, C_policy, C_transfer)
 * for a given assignment state. Used by the Simulated Annealing optimizer.
 */
public class CostCalculator {

    private final RiskMatrix riskMatrix;
    private final AlgorithmConfig config;

    public CostCalculator(RiskMatrix riskMatrix, AlgorithmConfig config) {
        this.riskMatrix = riskMatrix;
        this.config = config;
    }

    /**
     * Total cost Z = sum over all assigned patients of (C_safety + C_clinical + C_policy + C_transfer).
     * Use this overload when no "initial" assignment exists (e.g. first evaluation); C_transfer will be 0.
     */
    public double computeZ(AssignmentState state, Department department, Map<String, Patient> patientById) {
        return computeZ(state, department, patientById, null);
    }

    /**
     * Total cost Z with optional initial state for C_transfer (patients who moved from initial pay W_move).
     */
    public double computeZ(AssignmentState state, Department department, Map<String, Patient> patientById, AssignmentState initialState) {
        double z = 0;
        for (Map.Entry<String, Bed> e : state.getAssignments().entrySet()) {
            String patientId = e.getKey();
            Bed bed = e.getValue();
            Patient patient = patientById != null ? patientById.get(patientId) : null;
            Room room = findRoom(department, bed != null ? bed.getRoomId() : null);
            z += computeCSafety(patient, room, state, department, patientById, patientId, bed);
            z += computeCClinical(patient, bed, room);
            z += computeCPolicy(patient, room);
            z += computeCTransfer(patientId, bed, state, initialState);
        }
        return z;
    }

    /** C_safety: cohorting – penalty for patient's risk vs other occupants in the same room. */
    public double computeCSafety(Patient patient, Room room, AssignmentState state, Department department, Map<String, Patient> patientById, String currentPatientId, Bed currentBed) {
        RiskLevel patientRisk = getRiskLevel(patient);
        List<Patient> othersInRoom = getOtherPatientsInRoom(state, room, currentPatientId, currentBed, patientById);
        double cost = 0;
        for (Patient other : othersInRoom) {
            RiskLevel otherRisk = getRiskLevel(other);
            cost += riskMatrix.getPenalty(patientRisk, otherRisk);
        }
        return cost;
    }

    /** C_clinical: bed fit – BedType, ventilator, negative pressure (for INFECTIOUS), bariatric, broken bed. */
    public double computeCClinical(Patient patient, Bed bed, Room room) {
        if (bed == null) return config.getBigM();
        if (bed.isBroken()) return config.getBigM();
        ClinicalData cd = patient != null ? patient.getClinicalData() : null;
        double cost = 0;
        // Bed type: requiredBedType must match (or null = no requirement)
        if (cd != null && cd.getRequiredBedType() != null && !cd.getRequiredBedType().equals(bed.getType())) {
            cost += config.getBigM();
        }
        // Bariatric: if patient needs bariatric, bed must be BARIATRIC
        if (cd != null && cd.needsBariatricBed() && bed.getType() != BedType.BARIATRIC) {
            cost += config.getBigM();
        }
        // Ventilator
        if (cd != null && cd.isNeedsVentilator() && !bed.isHasVentilator()) {
            cost += config.getBigM();
        }
        // INFECTIOUS requires negative pressure room
        if (getRiskLevel(patient) == RiskLevel.INFECTIOUS && room != null && !room.isHasNegativePressure()) {
            cost += config.getBigM();
        }
        return cost;
    }

    /** C_policy: soft policy – e.g. distance from nurse station × severity. */
    public double computeCPolicy(Patient patient, Room room) {
        if (room == null) return 0;
        ClinicalData cd = patient != null ? patient.getClinicalData() : null;
        int severity = cd != null ? cd.getSeverityScore() : 0;
        double distance = room.getDistanceFromNurseStation();
        return config.getPolicyPenaltyWeight() * distance * Math.max(0, severity);
    }

    /** C_transfer: W_move for each patient who was moved relative to initial state. */
    public double computeCTransfer(String patientId, Bed currentBed, AssignmentState state, AssignmentState initialState) {
        if (initialState == null || patientId == null) return 0;
        Bed initialBed = initialState.getBed(patientId);
        if (initialBed == null) return 0; // was not assigned before
        if (Objects.equals(initialBed.getId(), currentBed != null ? currentBed.getId() : null)) return 0;
        return config.getTransferPenaltyWeight();
    }

    private static RiskLevel getRiskLevel(Patient patient) {
        if (patient == null) return RiskLevel.CLEAN;
        ClinicalData cd = patient.getClinicalData();
        if (cd == null || cd.getRiskLevel() == null) return RiskLevel.CLEAN;
        return cd.getRiskLevel();
    }

    private static Room findRoom(Department department, String roomId) {
        if (department == null || roomId == null) return null;
        for (Room r : department.getRooms()) {
            if (roomId.equals(r.getId())) return r;
        }
        return null;
    }

    private static List<Patient> getOtherPatientsInRoom(AssignmentState state, Room room, String excludePatientId, Bed excludeBed, Map<String, Patient> patientById) {
        List<Patient> others = new ArrayList<>();
        if (room == null || state == null) return others;
        for (Bed b : room.getBeds()) {
            if (b == excludeBed) continue;
            String pid = state.getPatientIdInBed(b);
            if (pid == null || pid.equals(excludePatientId)) continue;
            Patient p = patientById != null ? patientById.get(pid) : null;
            if (p != null) others.add(p);
        }
        return others;
    }
}
