package Algorithm.cost;

import Algorithm.AssignmentState;
import Algorithm.risk.RiskMatrix;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.RiskLevel;
import Model.policy.PatientRiskPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** C_safety: cohorting penalties within the same room. */
public class SafetyCostStrategy implements CostStrategy {

    private final RiskMatrix riskMatrix;

    public SafetyCostStrategy(RiskMatrix riskMatrix) {
        this.riskMatrix = riskMatrix;
    }

    @Override
    public double computeTotal(AssignmentState state, Department department,
                               Map<String, Patient> patientById, AssignmentState initialState) {
        double z = 0;
        for (Map.Entry<String, Bed> e : state.getAssignments().entrySet()) {
            String patientId = e.getKey();
            Bed bed = e.getValue();
            Patient patient = patientById != null ? patientById.get(patientId) : null;
            Room room = department != null && bed != null ? department.findRoomById(bed.getRoomId()) : null;
            z += computeForPatient(patient, room, state, patientById, patientId, bed);
        }
        return z;
    }

    public double computeForPatient(Patient patient, Room room, AssignmentState state,
                                    Map<String, Patient> patientById, String currentPatientId, Bed currentBed) {
        RiskLevel patientRisk = PatientRiskPolicy.effectiveRiskLevelForCohorting(patient);
        List<Patient> othersInRoom = getOtherPatientsInRoom(state, room, currentPatientId, currentBed, patientById);
        double cost = 0;
        for (Patient other : othersInRoom) {
            RiskLevel otherRisk = PatientRiskPolicy.effectiveRiskLevelForCohorting(other);
            cost += riskMatrix.getPenalty(patientRisk, otherRisk);
        }
        return cost;
    }

    private static List<Patient> getOtherPatientsInRoom(AssignmentState state, Room room, String excludePatientId,
                                                        Bed excludeBed, Map<String, Patient> patientById) {
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
