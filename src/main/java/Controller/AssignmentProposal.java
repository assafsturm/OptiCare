package Controller;

import java.util.List;

import Algorithm.AssignmentState;


// recored for the proposle, all ifno that the optimazer returns
public record AssignmentProposal(
        boolean feasible,
        List<String> feasibilityViolations,
        AssignmentState baselineState,
        AssignmentState proposedState, 
        double baselineZ,// sa meta data
        double proposedZ,
        int iterations,
        boolean stoppedByTimeLimit,
        List<String> warnings// if patients not assigned after sa
) {
    public AssignmentProposal { // compact constructor
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public double deltaZ() {
        return proposedZ - baselineZ;
    }
}
