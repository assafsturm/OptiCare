package Algorithm.neighborhood;

import Algorithm.AssignmentState;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;

import java.util.Map;

// applies the move in place and supports deterministic undo without deep copying the state
public final class NeighborMoveExecutor {
// seald interface for the undo tokens
    public sealed interface UndoToken permits AssignUndo, MoveUndo, SwapUndo { }

    // minimal records for saving the undo information
    public record AssignUndo(String patientId) implements UndoToken { } // undo token for assign

    public record MoveUndo(String patientId, String fromBedId) implements UndoToken { } // undo token for move

    public record SwapUndo(String patientIdA, String patientIdB, String bedIdA, String bedIdB) implements UndoToken { } // undo token for swap  

    // make the move based on the type of move
    public UndoToken apply(NeighborMove move, AssignmentState state, Department department,
                           Map<String, Patient> patientById) {
        switch (move.getType()) {
            case ASSIGN:
                return applyAssign(move, state, department, patientById);
            case MOVE:
                return applyMove(move, state, department, patientById);
            case SWAP:
                return applySwap(move, state, department, patientById);
            default:
                throw new IllegalStateException("Unknown move type");
        }
    }

    private static AssignUndo applyAssign(NeighborMove move, AssignmentState state, Department department,
                                          Map<String, Patient> patientById) {
        Patient p = patientById.get(move.getPatientIdA());
        Bed to = department.findBedById(move.getToBedIdA());
        state.assign(p, to);
        return new AssignUndo(move.getPatientIdA());// returns the undo token for the assign
    }

    private static MoveUndo applyMove(NeighborMove move, AssignmentState state, Department department,
                                        Map<String, Patient> patientById) {
        Patient p = patientById.get(move.getPatientIdA());
        Bed to = department.findBedById(move.getToBedIdA());
        state.assign(p, to);// allredy has unassign if allredy assigned to another bed
        return new MoveUndo(move.getPatientIdA(), move.getFromBedIdA());// returns the undo token for the move 
    }

    private static SwapUndo applySwap(NeighborMove move, AssignmentState state, Department department,
                                       Map<String, Patient> patientById) {
        Patient pa = patientById.get(move.getPatientIdA());
        Patient pb = patientById.get(move.getPatientIdB());
        Bed bedA = department.findBedById(move.getFromBedIdA());
        Bed bedB = department.findBedById(move.getFromBedIdB());
        state.assign(pa, bedB);
        state.assign(pb, bedA);
        return new SwapUndo(move.getPatientIdA(), move.getPatientIdB(),
                move.getFromBedIdA(), move.getFromBedIdB());// returns the undo token for the swap
    }

    // undo the move based on the type of move
    public void undo(UndoToken token, AssignmentState state, Department department,
                     Map<String, Patient> patientById) {
        if (token instanceof AssignUndo a) {
            state.unassign(a.patientId());// unassign the patient from the bed
        } else if (token instanceof MoveUndo m) {
            Patient p = patientById.get(m.patientId());
            Bed from = department.findBedById(m.fromBedId());
            state.assign(p, from);// assign the patient to the old bed (again with unassign if allredy assigned to another bed)
        } else if (token instanceof SwapUndo s) {
            Patient pa = patientById.get(s.patientIdA());
            Patient pb = patientById.get(s.patientIdB());
            Bed bedA = department.findBedById(s.bedIdA());
            Bed bedB = department.findBedById(s.bedIdB());
            state.assign(pa, bedA);// same
            state.assign(pb, bedB);// same
        }
    }
}
