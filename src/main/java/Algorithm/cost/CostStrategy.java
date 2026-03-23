package Algorithm.cost;

import Algorithm.AssignmentState;
import Model.entety.Department;
import Model.entety.Patient;

import java.util.Map;

/**
 * One term of the objective (e.g. C_safety). Strategies sum their contribution for the full state.
 */
public interface CostStrategy {

    double computeTotal(AssignmentState state, Department department,
                        Map<String, Patient> patientById, AssignmentState initialState);
}
