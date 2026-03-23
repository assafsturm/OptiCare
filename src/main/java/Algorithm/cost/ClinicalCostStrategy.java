package Algorithm.cost;

import Algorithm.AssignmentState;
import Config.AlgorithmConfig;
import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.policy.PatientRiskPolicy;

import java.util.Map;

/**
 * C_clinical: bed fit. Missing {@link ClinicalData} or {@link ClinicalData#getRequiredBedType()} → no type/ventilator/bariatric penalty;
 * negative pressure only when explicitly {@link Model.enums.RiskLevel#INFECTIOUS}.
 */
public class ClinicalCostStrategy implements CostStrategy {

    private final AlgorithmConfig config;

    public ClinicalCostStrategy(AlgorithmConfig config) {
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
            z += computeForPatient(patient, bed, room);
        }
        return z;
    }

    public double computeForPatient(Patient patient, Bed bed, Room room) {
        if (bed == null) return config.getBigM();
        if (bed.isBroken()) return config.getBigM();
        ClinicalData cd = patient != null ? patient.getClinicalData() : null;
        double cost = 0;
        if (cd != null) {
            if (cd.getRequiredBedType() != null && !cd.getRequiredBedType().equals(bed.getType())) {
                cost += config.getBigM();
            }
            if (cd.needsBariatricBed() && bed.getType() != BedType.BARIATRIC) {
                cost += config.getBigM();
            }
            if (cd.isNeedsVentilator() && !bed.isHasVentilator()) {
                cost += config.getBigM();
            }
        }
        if (PatientRiskPolicy.requiresNegativePressureRoom(patient) && room != null && !room.isHasNegativePressure()) {
            cost += config.getBigM();
        }
        return cost;
    }
}
