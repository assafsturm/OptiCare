package Algorithm.cost;

import Algorithm.AssignmentState;
import Model.entety.Department;
import Model.entety.Patient;

import java.util.Map;

// interface for all cost strategies
public interface CostStrategy {

    // computes the total cost for the given state
    double computeTotal(AssignmentState state, Department department,
                        Map<String, Patient> patientById, AssignmentState initialState);
}
