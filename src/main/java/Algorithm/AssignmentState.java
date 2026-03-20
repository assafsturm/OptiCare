package Algorithm;

import Model.entety.Bed;
import Model.entety.Patient;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Snapshot of patient-to-bed assignment. Used by the optimizer as a candidate solution.
 * Supports deep copy so SA can mutate a copy without affecting the best state.
 */
public class AssignmentState {

    /** patientId -> Bed. Current assignment of each assigned patient. */
    private final Map<String, Bed> patientToBed;
    /** bedId -> patientId. Reverse map for quick lookup of who is in a bed. */
    private final Map<String, String> bedToPatient;

    public AssignmentState() {
        this.patientToBed = new HashMap<>();
        this.bedToPatient = new HashMap<>();
    }

    /**
     * Deep copy constructor. The new state has its own map; modifying it does not affect the original.
     * Bed references are shared (beds are entities, not copied).
     */
    public AssignmentState(AssignmentState other) {
        this.patientToBed = new HashMap<>(other.patientToBed);
        this.bedToPatient = new HashMap<>(other.bedToPatient);
    }

    /** Returns an unmodifiable view of patientId -> Bed. */
    public Map<String, Bed> getAssignments() {
        return Collections.unmodifiableMap(patientToBed);
    }
    // overload method for get bed by Patient or patientId
    public Bed getBed(Patient patient) {
        return patient == null ? null : patientToBed.get(patient.getId());
    }

    public Bed getBed(String patientId) {
        return patientId == null ? null : patientToBed.get(patientId);
    }

    /** Optional reverse lookup: which patient is in this bed, or null if bed is free. */
    public String getPatientIdInBed(Bed bed) {
        return bed == null ? null : bedToPatient.get(bed.getId());
    }

    /**
     * Assigns patient to bed. If the bed is already occupied, the current occupant is unassigned first.
     * Rejects null patient, null bed, or null ids (no-op).
     */
    public void assign(Patient patient, Bed bed) {
        if (patient == null || bed == null) return;
        String pid = patient.getId();
        String bid = bed.getId();
        if (pid == null || bid == null) return;
        // If bed is already occupied by another patient, unassign them first (one patient per bed).
        String currentInBed = bedToPatient.get(bid);
        if (currentInBed != null && !currentInBed.equals(pid)) {
            patientToBed.remove(currentInBed);
            bedToPatient.remove(bid);
        }
        Bed oldBed = patientToBed.get(pid);
        if (oldBed != null) bedToPatient.remove(oldBed.getId());
        patientToBed.put(pid, bed);
        bedToPatient.put(bid, pid);
    }
    // overload method for unassign by Patient or patientId
    public void unassign(Patient patient) {
        if (patient == null) return;
        Bed bed = patientToBed.remove(patient.getId());
        if (bed != null) bedToPatient.remove(bed.getId());
    }

    public void unassign(String patientId) {
        if (patientId == null) return;
        Bed bed = patientToBed.remove(patientId);
        if (bed != null) bedToPatient.remove(bed.getId());
    }

    /** Returns true if the bed is assigned to some patient in this state. */
    public boolean isBedOccupied(Bed bed) {
        return bed != null && bedToPatient.containsKey(bed.getId());
    }

    public int size() {
        return patientToBed.size();
    }
}
