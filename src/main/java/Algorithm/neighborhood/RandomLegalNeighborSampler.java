package Algorithm.neighborhood;

import Algorithm.AssignmentState;
import Algorithm.feasibility.HardConstraints;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.enums.PatientStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Rejection sampling: one random legal {@link NeighborMove}, or {@code null} if none found within the attempt budget.
 */
public final class RandomLegalNeighborSampler {

    private final Department department;
    private final HardConstraints hardConstraints;
    private final NeighborMoveExecutor executor;
    private final int maxAttemptsPerSample;

    public RandomLegalNeighborSampler(Department department, HardConstraints hardConstraints,
                                      int maxAttemptsPerSample) {
        this.department = department;
        this.hardConstraints = hardConstraints;
        this.executor = new NeighborMoveExecutor();
        this.maxAttemptsPerSample = Math.max(1, maxAttemptsPerSample);
    }

    public NeighborMove sample(Random rng, AssignmentState state, Map<String, Patient> patientById) {
        for (int attempt = 0; attempt < maxAttemptsPerSample; attempt++) {
            int kind = rng.nextInt(3);
            NeighborMove move = switch (kind) {
                case 0 -> tryAssign(rng, state, patientById);
                case 1 -> tryMove(rng, state, patientById);
                default -> trySwap(rng, state, patientById);
            };
            if (move != null && validate(move, state, patientById)) {
                return move;
            }
        }
        return null;
    }

    private boolean validate(NeighborMove move, AssignmentState state, Map<String, Patient> patientById) {
        NeighborMoveExecutor.UndoToken undo = executor.apply(move, state, department, patientById);
        boolean ok = hardConstraints.isAssignmentStateGloballyValid(state, patientById);
        executor.undo(undo, state, department, patientById);
        return ok;
    }

    private NeighborMove tryAssign(Random rng, AssignmentState state, Map<String, Patient> patientById) {
        List<Patient> candidates = new ArrayList<>();
        for (Patient p : department.getWaitingList()) {
            if (p == null) continue;
            if (p.getStatus() != PatientStatus.WAITING) continue;
            if (p.isTemporarilyUnavailable()) continue;
            if (state.getBed(p.getId()) != null) continue;
            candidates.add(p);
        }
        List<Bed> free = new ArrayList<>();
        for (Bed b : bedsInDeterministicOrder()) {
            if (!state.isBedOccupied(b)) free.add(b);
        }
        if (candidates.isEmpty() || free.isEmpty()) return null;
        Patient p = candidates.get(rng.nextInt(candidates.size()));
        Bed b = free.get(rng.nextInt(free.size()));
        if (!hardConstraints.isLegalAssignOrMoveToFreeBed(p, b, state, patientById)) return null;
        return NeighborMove.assign(p.getId(), b.getId());
    }

    private NeighborMove tryMove(Random rng, AssignmentState state, Map<String, Patient> patientById) {
        List<String> movable = new ArrayList<>();
        for (String pid : state.getAssignments().keySet()) {
            Patient p = patientById != null ? patientById.get(pid) : null;
            if (p != null && !p.isTemporarilyUnavailable()) movable.add(pid);
        }
        if (movable.isEmpty()) return null;
        String pid = movable.get(rng.nextInt(movable.size()));
        Bed from = state.getBed(pid);
        if (from == null) return null;
        List<Bed> targets = bedsInDeterministicOrder();
        Bed to = targets.get(rng.nextInt(targets.size()));
        if (to.getId().equals(from.getId())) return null;
        if (state.isBedOccupied(to)) return null;
        if (BedEquivalence.areEquivalent(from, to)) return null;
        Patient p = patientById.get(pid);
        if (!hardConstraints.isLegalAssignOrMoveToFreeBed(p, to, state, patientById)) return null;
        return NeighborMove.move(pid, from.getId(), to.getId());
    }

    private NeighborMove trySwap(Random rng, AssignmentState state, Map<String, Patient> patientById) {
        List<String> movable = new ArrayList<>();
        for (String pid : state.getAssignments().keySet()) {
            Patient p = patientById != null ? patientById.get(pid) : null;
            if (p != null && !p.isTemporarilyUnavailable()) movable.add(pid);
        }
        if (movable.size() < 2) return null;
        int i = rng.nextInt(movable.size());
        int j = rng.nextInt(movable.size() - 1);
        if (j >= i) j++;
        String idA = movable.get(i);
        String idB = movable.get(j);
        Bed bedA = state.getBed(idA);
        Bed bedB = state.getBed(idB);
        if (bedA == null || bedB == null) return null;
        if (BedEquivalence.areEquivalent(bedA, bedB)) return null;
        return NeighborMove.swap(idA, idB, bedA.getId(), bedB.getId());
    }

    private List<Bed> bedsInDeterministicOrder() {
        List<Bed> beds = new ArrayList<>();
        for (var room : department.getRooms()) {
            beds.addAll(room.getBeds());
        }
        return beds;
    }
}
