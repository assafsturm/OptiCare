package Algorithm.neighborhood;

import Algorithm.AssignmentState;
import Algorithm.AlgorithmTrace;
import Algorithm.feasibility.HardConstraints;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.enums.PatientStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

// rejection sampling: one random legal NeighborMove, or null if none found within the attempt budget.
// where SA gets candidate moves no actually applies them, just picks and verefy legal
public final class RandomLegalNeighborSampler { 

    private final Department department;
    private final HardConstraints hardConstraints;
    private final NeighborMoveExecutor executor;
    private final int maxAttemptsPerSample;
    private int sampleCalls;

    public RandomLegalNeighborSampler(Department department, HardConstraints hardConstraints,
                                      int maxAttemptsPerSample) {
        this.department = department;
        this.hardConstraints = hardConstraints;
        this.executor = new NeighborMoveExecutor();
        this.maxAttemptsPerSample = Math.max(1, maxAttemptsPerSample);
    }

    public NeighborMove sample(Random rng, AssignmentState state, Map<String, Patient> patientById) {
        sampleCalls++;
        for (int attempt = 0; attempt < maxAttemptsPerSample; attempt++) {
            int kind = rng.nextInt(3);// same odds for each move type
            NeighborMove move = switch (kind) { //switch statement for the move type
                case 0 -> tryAssign(rng, state, patientById);
                case 1 -> tryMove(rng, state, patientById);
                default -> trySwap(rng, state, patientById);
            };
            if (move != null && validate(move, state, patientById)) { // check if  the move after applying is valid
                if (sampleCalls <= 20 || sampleCalls % 200 == 0) {
                    AlgorithmTrace.log("neighborhood", "Sample #" + sampleCalls + " accepted move type=" + move.getType()
                            + " on attempt=" + (attempt + 1));
                }
                return move;
            }
        }
        if (sampleCalls <= 20 || sampleCalls % 200 == 0) {
            AlgorithmTrace.log("neighborhood", "Sample #" + sampleCalls + " found no legal move.");
        }
        return null;
    }
    // validate the move by applying it and then unapplying it
    private boolean validate(NeighborMove move, AssignmentState state, Map<String, Patient> patientById) {
        NeighborMoveExecutor.UndoToken undo = executor.apply(move, state, department, patientById);
        boolean ok = hardConstraints.isAssignmentStateGloballyValid(state, patientById);
        executor.undo(undo, state, department, patientById);
        return ok;
    }

    // try to assign a patient to a free bed
    // pick random patient and bed and check if the assign is legal
    private NeighborMove tryAssign(Random rng, AssignmentState state, Map<String, Patient> patientById) {
        List<Patient> candidates = new ArrayList<>();
        for (Patient p : department.getWaitingList()) {// get all eligible patients in wating list
            if (p != null && p.getStatus() == PatientStatus.WAITING && !p.isTemporarilyUnavailable()
                    && state.getBed(p.getId()) == null) {
                candidates.add(p);
            }
        }
        List<Bed> free = new ArrayList<>();
        for (Bed b : bedsInDeterministicOrder()) {// get all free beds 
            if (!state.isBedOccupied(b)) free.add(b);
        }
        if (candidates.isEmpty() || free.isEmpty()) return null;
        Patient p = candidates.get(rng.nextInt(candidates.size())); // random patient from candidates
        Bed b = free.get(rng.nextInt(free.size())); // random free bed from free beds
        if (!hardConstraints.isLegalAssignOrMoveToFreeBed(p, b, state, patientById)) return null; // if the move is not legal return null
        return NeighborMove.assign(p.getId(), b.getId()); // return the assign move
    }// O(W + b)

    // try to move a patient to a free bed
    // pick random patient and bed and check if the move is legal
    private NeighborMove tryMove(Random rng, AssignmentState state, Map<String, Patient> patientById) {
        List<String> movable = new ArrayList<>();
        for (String pid : state.getAssignments().keySet()) {
            Patient p = patientById != null ? patientById.get(pid) : null;
            if (p != null && !p.isTemporarilyUnavailable()) movable.add(pid);// if patient is assigned and not temporarily unavailable
        }
        if (movable.isEmpty()) return null;
        String pid = movable.get(rng.nextInt(movable.size()));// random patient from movable
        Bed from = state.getBed(pid);// get the bed the patient is assigned to
        if (from == null) return null;
        List<Bed> targets = bedsInDeterministicOrder();
        Bed to = targets.get(rng.nextInt(targets.size()));// random bed from targets
        if (to.getId().equals(from.getId())) return null; // if the bed is the same as the current bed return null
        if (state.isBedOccupied(to)) return null;// if the bed is occupied return null
        if (BedEquivalence.areEquivalent(from, to)) return null; // if the beds are equivalent (in terms of medical) return null
        Patient p = patientById.get(pid);
        if (!hardConstraints.isLegalAssignOrMoveToFreeBed(p, to, state, patientById)) return null; // if the move is not legal return null
        return NeighborMove.move(pid, from.getId(), to.getId());
    }// O(P + B)

    // try to swap two patients
    // pick 2random patients and 2 random beds and check if the swap is legal
    private NeighborMove trySwap(Random rng, AssignmentState state, Map<String, Patient> patientById) {
        List<String> movable = new ArrayList<>();
        for (String pid : state.getAssignments().keySet()) { // get all assigned patients
            Patient p = patientById != null ? patientById.get(pid) : null;
            if (p != null && !p.isTemporarilyUnavailable()) movable.add(pid);
        }
        if (movable.size() < 2) return null;
        int i = rng.nextInt(movable.size()); // random patient from movable
        int j = rng.nextInt(movable.size() - 1); // random patient from movable - 1 (allredy took one)
        if (j >= i) j++;// assures that allways takes 2 different patients
        String idA = movable.get(i);// first patient
        String idB = movable.get(j);// second patient
        Bed bedA = state.getBed(idA);// first bed
        Bed bedB = state.getBed(idB);// second bed
        if (bedA == null || bedB == null) return null;
        if (BedEquivalence.areEquivalent(bedA, bedB)) return null; // if the beds are equivalent (in terms of medical) return null
        return NeighborMove.swap(idA, idB, bedA.getId(), bedB.getId()); 
    }// O(P)

    // ************* if lists dont change
    private List<Bed> bedsInDeterministicOrder() {
        List<Bed> beds = new ArrayList<>();
        for (var room : department.getRooms()) {
            beds.addAll(room.getBeds());
        }
        return beds;
    }
}
