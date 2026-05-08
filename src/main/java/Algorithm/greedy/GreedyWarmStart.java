package Algorithm.greedy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import Algorithm.AlgorithmTrace;
import Algorithm.AssignmentState;
import Algorithm.feasibility.HardConstraints;
import Algorithm.queue.WaitingListComparatorFactory;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.enums.PatientStatus;


//assign all waiting patients to free beds in deterministic priority order
//may leave some patients unassigned when no legal bed found that are free
//deterministic for same inputs (important for reproducibility)
//O(W + B)
public final class GreedyWarmStart {

    private GreedyWarmStart() {
    }

    public static AssignmentState build(Department department, Map<String, Patient> patientById,
                                        AssignmentState baseline, HardConstraints hardConstraints) {
        AlgorithmTrace.log("greedy", "Starting warm start for department="
                + (department != null ? department.getId() : "null"));
        AssignmentState state = new AssignmentState(baseline);
        List<Patient> ordered = new ArrayList<>();// all patiens in waiting list
        for (Patient p : department.getWaitingList()) {
            if (p != null && p.getStatus() == PatientStatus.WAITING && !p.isTemporarilyUnavailable()) {
                ordered.add(p);
            }
        }
        ordered.sort(WaitingListComparatorFactory.forGlobalQueue());// sort 
        AlgorithmTrace.log("greedy", "Eligible waiting patients=" + ordered.size());
        Comparator<Bed> bedOrder = Comparator.comparing(Bed::getRoomId, Comparator.nullsLast(String::compareTo))
                .thenComparing(Bed::getId, Comparator.nullsLast(String::compareTo));// crate a comparator for the beds
                // room id then bed id
        List<Bed> beds = new ArrayList<>();
        for (var room : department.getRooms()) {// all beds in the department
            List<Bed> rb = new ArrayList<>(room.getBeds());
            rb.sort(bedOrder);// sort the beds by the comparator
            beds.addAll(rb);// add the beds to the list
        }
        for (Patient p : ordered) {
            if (state.getBed(p.getId()) == null) {// if the patient is not assigned
                boolean placed = false;
                for (Bed b : beds) {
                    if (!placed && !state.isBedOccupied(b)
                            && hardConstraints.isLegalAssignOrMoveToFreeBed(p, b, state, patientById)) {//if bed is free and legally assignable
                        state.assign(p, b);// assign the patient to the bed
                        AlgorithmTrace.log("greedy", "Placed patient " + p.getId() + " into bed " + b.getId());
                        placed = true;
                    }
                }
                if (!placed) {
                    AlgorithmTrace.log("greedy", "Could not place patient " + p.getId() + " (no legal free bed).");
                }
            }
        }
        AlgorithmTrace.log("greedy", "Warm start done. Total assignments=" + state.size());
        return state;
    }
}
