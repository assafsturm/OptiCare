package Algorithm.cost;

import Algorithm.AssignmentState;
import Config.AlgorithmConfig;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;

import java.util.Map;
import java.util.Objects;

/** C_transfer: baseline-relative move penalty. */
public class TransferCostStrategy implements CostStrategy {

    private final AlgorithmConfig config;

    public TransferCostStrategy(AlgorithmConfig config) {
        this.config = config;
    }

    @Override
    public double computeTotal(AssignmentState state, Department department,
                               Map<String, Patient> patientById, AssignmentState initialState) {
        if (initialState == null) return 0;
        double z = 0;
        for (Map.Entry<String, Bed> e : state.getAssignments().entrySet()) {
            z += computeForPatient(e.getKey(), e.getValue(), state, initialState);
        }
        return z;
    }

    public double computeForPatient(String patientId, Bed currentBed, AssignmentState state, AssignmentState initialState) {
        if (initialState == null || patientId == null) return 0;
        Bed initialBed = initialState.getBed(patientId);
        if (initialBed == null) return 0;
        if (Objects.equals(initialBed.getId(), currentBed != null ? currentBed.getId() : null)) return 0;
        return config.getTransferPenaltyWeight();
    }
}
