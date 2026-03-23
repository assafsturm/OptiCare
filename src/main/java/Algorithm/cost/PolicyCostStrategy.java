package Algorithm.cost;

import Algorithm.AssignmentState;
import Config.AlgorithmConfig;
import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;

import java.util.Map;

/** C_policy: nurse-station distance × severity (missing clinical → severity 0). */
public class PolicyCostStrategy implements CostStrategy {

    private final AlgorithmConfig config;

    public PolicyCostStrategy(AlgorithmConfig config) {
        this.config = config;
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
            z += computeForPatient(patient, room);
        }
        return z;
    }

    public double computeForPatient(Patient patient, Room room) {
        if (room == null) return 0;
        ClinicalData cd = patient != null ? patient.getClinicalData() : null;
        int severity = cd != null ? cd.getSeverityScore() : 0;
        double distance = room.getDistanceFromNurseStation();
        return config.getPolicyPenaltyWeight() * distance * Math.max(0, severity);
    }
}
