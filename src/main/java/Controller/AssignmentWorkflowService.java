package Controller;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import Algorithm.AssignmentState;
import Algorithm.sa.SaProgressEvent;
import Model.entety.Department;
import Model.entety.Patient;

/**
 * Stage-4 controller/service contract for optimization workflow operations.
 */
public interface AssignmentWorkflowService {

    /**
     * Builds a proposal from current ward state using greedy warm start + SA optimization.
     */
    AssignmentProposal proposeAssignment(Department department, Map<String, Patient> patientById,
                                        AssignmentState currentState);

    /**
     * Same as proposeAssignment, with SA progress observer callbacks.
     */
    AssignmentProposal proposeAssignment(Department department, Map<String, Patient> patientById,
                                        AssignmentState currentState,
                                        Consumer<SaProgressEvent> progressConsumer);

    /**
     * Builds patient-level diff and score summary for preview/approval UI.
     */
    AssignmentPreview buildPreview(AssignmentProposal proposal);

    /**
     * Stores the latest proposal per department as pending approval.
     */
    void setPendingProposal(String departmentId, AssignmentProposal proposal);

    /**
     * Returns pending proposal for department, or null when absent.
     */
    AssignmentProposal getPendingProposal(String departmentId);

    /**
     * Approves pending proposal and returns the new current assignment state.
     */
    AssignmentState approvePendingProposal(String departmentId, AssignmentState currentState);

    /**
     * Rejects pending proposal and keeps current assignment unchanged.
     */
    AssignmentState rejectPendingProposal(String departmentId, AssignmentState currentState);

    /**
     * Admits patient into department waiting flow (in-memory Stage-4 behavior).
     */
    void admitPatient(Department department, Patient patient);

    /**
     * Discharges patient: remove from waiting list and unassign from current state if assigned.
     */
    AssignmentState dischargePatient(Department department, String patientId, AssignmentState currentState);

    /**
     * Deterministic waiting queue view ordered by project comparator tuple.
     */
    List<Patient> buildWaitingQueueView(Department department);
}
