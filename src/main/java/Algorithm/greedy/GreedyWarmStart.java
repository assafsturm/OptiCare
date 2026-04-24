package Algorithm.greedy;

import Algorithm.AssignmentState;
import Algorithm.feasibility.HardConstraints;
import Algorithm.queue.WaitingListComparatorFactory;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.enums.PatientStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mandatory warm start: extends {@code baseline} by placing eligible waiting patients into legal free beds
 * in deterministic priority order. May leave some patients unassigned when no legal bed exists.
 */
public final class GreedyWarmStart {

    private GreedyWarmStart() {
    }

    public static AssignmentState build(Department department, Map<String, Patient> patientById,
                                        AssignmentState baseline, HardConstraints hardConstraints) {
        AssignmentState state = new AssignmentState(baseline);
        List<Patient> ordered = new ArrayList<>();
        for (Patient p : department.getWaitingList()) {
            if (p == null) continue;
            if (p.getStatus() != PatientStatus.WAITING) continue;
            if (p.isTemporarilyUnavailable()) continue;
            ordered.add(p);
        }
        ordered.sort(WaitingListComparatorFactory.forGlobalQueue());
        Comparator<Bed> bedOrder = Comparator.comparing(Bed::getRoomId, Comparator.nullsLast(String::compareTo))
                .thenComparing(Bed::getId, Comparator.nullsLast(String::compareTo));
        List<Bed> beds = new ArrayList<>();
        for (var room : department.getRooms()) {
            List<Bed> rb = new ArrayList<>(room.getBeds());
            rb.sort(bedOrder);
            beds.addAll(rb);
        }
        for (Patient p : ordered) {
            if (state.getBed(p.getId()) != null) continue;
            for (Bed b : beds) {
                if (state.isBedOccupied(b)) continue;
                if (hardConstraints.isLegalAssignOrMoveToFreeBed(p, b, state, patientById)) {
                    state.assign(p, b);
                    break;
                }
            }
        }
        return state;
    }
}
