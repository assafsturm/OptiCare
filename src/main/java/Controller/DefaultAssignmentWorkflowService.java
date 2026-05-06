package Controller;

import Algorithm.AssignmentState;
import Algorithm.AlgorithmTrace;
import Algorithm.CostCalculator;
import Algorithm.feasibility.FeasibilityChecker;
import Algorithm.feasibility.FeasibilityResult;
import Algorithm.feasibility.HardConstraints;
import Algorithm.greedy.GreedyWarmStart;
import Algorithm.queue.WaitingListComparatorFactory;
import Algorithm.risk.RiskMatrix;
import Algorithm.risk.RiskMatrixFactory;
import Algorithm.sa.SaResult;
import Algorithm.sa.SimulatedAnnealingEngine;
import Config.AlgorithmConfig;
import Model.entety.Department;
import Model.entety.Patient;
import Model.enums.PatientStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * Default Stage-4 workflow service (slice 1): propose assignment only.
 */
public class DefaultAssignmentWorkflowService implements AssignmentWorkflowService {

    private final AlgorithmConfig config;
    private final SimulatedAnnealingEngine saEngine;
    private final FeasibilityChecker feasibilityChecker;
    private final Map<String, AssignmentProposal> pendingByDepartmentId = new HashMap<>();

    public DefaultAssignmentWorkflowService(AlgorithmConfig config) {
        this(config, new SimulatedAnnealingEngine(), new FeasibilityChecker(config));
    }

    public DefaultAssignmentWorkflowService(AlgorithmConfig config,
                                            SimulatedAnnealingEngine saEngine,
                                            FeasibilityChecker feasibilityChecker) {
        this.config = config;
        this.saEngine = saEngine;
        this.feasibilityChecker = feasibilityChecker;
    }

    @Override
    public AssignmentProposal proposeAssignment(Department department, Map<String, Patient> patientById,
                                                AssignmentState currentState) {
        AlgorithmTrace.log("workflow", "Starting proposal for department="
                + (department != null ? department.getId() : "null")
                + ", patients=" + (patientById != null ? patientById.size() : 0));
        AssignmentState baselineInput = currentState != null ? currentState : new AssignmentState();
        FeasibilityResult feasibility = feasibilityChecker.check(department, patientById, baselineInput);
        if (!feasibility.isFeasible()) {
            AlgorithmTrace.log("workflow", "Feasibility failed with " + feasibility.getViolations().size()
                    + " violation(s): " + feasibility.getViolations());
            return new AssignmentProposal(
                    false,
                    new ArrayList<>(feasibility.getViolations()),
                    new AssignmentState(baselineInput),
                    new AssignmentState(baselineInput),
                    0.0,
                    0.0,
                    0,
                    false,
                    List.of()
            );
        }

        RiskMatrix riskMatrix = RiskMatrixFactory.fromConfig(config);
        HardConstraints hardConstraints = new HardConstraints(riskMatrix, department);
        AssignmentState warmStartState = GreedyWarmStart.build(department, patientById, baselineInput, hardConstraints);
        AlgorithmTrace.log("workflow", "Warm start completed. Assigned count=" + warmStartState.size());
        AssignmentState baselineForTransfer = new AssignmentState(warmStartState);
        CostCalculator costCalculator = new CostCalculator(riskMatrix, config);

        double baselineZ = costCalculator.computeZ(baselineForTransfer, department, patientById, baselineForTransfer);
        AlgorithmTrace.log("workflow", "Baseline energy (Z)=" + baselineZ);
        SaResult result = saEngine.run(
                department,
                patientById,
                new AssignmentState(warmStartState),
                baselineForTransfer,
                costCalculator,
                config,
                hardConstraints
        );
        double proposedZ = result.bestZ();
        AssignmentState proposedState = result.bestState();
        List<String> warnings = unplacedWaitingWarnings(department, proposedState);
        AlgorithmTrace.log("workflow", "SA finished. proposedZ=" + proposedZ
                + ", iterations=" + result.iterations()
                + ", stoppedByTime=" + result.stoppedByTimeLimit()
                + ", placementWarnings=" + warnings.size());
        return new AssignmentProposal(
                true,
                List.of(),
                baselineForTransfer,
                proposedState,
                baselineZ,
                proposedZ,
                result.iterations(),
                result.stoppedByTimeLimit(),
                warnings
        );
    }

    /**
     * Waiting patients who remain eligible but have no bed in the proposed state after optimization.
     */
    private static List<String> unplacedWaitingWarnings(Department department, AssignmentState proposedState) {
        List<String> out = new ArrayList<>();
        if (department == null || proposedState == null || department.getWaitingList() == null) {
            return out;
        }
        for (Patient p : department.getWaitingList()) {
            if (p != null && p.getStatus() == PatientStatus.WAITING && !p.isTemporarilyUnavailable()
                    && proposedState.getBed(p.getId()) == null) {
                out.add("Patient " + p.getId()
                        + " could not be placed (no legal bed reachable via assign/move/swap).");
            }
        }
        return out;
    }

    @Override
    public AssignmentPreview buildPreview(AssignmentProposal proposal) {
        AssignmentState baseline = proposal != null && proposal.baselineState() != null
                ? proposal.baselineState() : new AssignmentState();
        AssignmentState proposed = proposal != null && proposal.proposedState() != null
                ? proposal.proposedState() : new AssignmentState();

        Set<String> patientIds = new HashSet<>();
        patientIds.addAll(baseline.getAssignments().keySet());
        patientIds.addAll(proposed.getAssignments().keySet());

        List<PatientAssignmentDiff> diffs = new ArrayList<>();
        for (String patientId : patientIds) {
            String fromBedId = baseline.getBed(patientId) != null ? baseline.getBed(patientId).getId() : null;
            String toBedId = proposed.getBed(patientId) != null ? proposed.getBed(patientId).getId() : null;
            AssignmentChangeType type = classifyChange(fromBedId, toBedId);
            diffs.add(new PatientAssignmentDiff(patientId, fromBedId, toBedId, type));
        }
        diffs.sort(Comparator.comparing(PatientAssignmentDiff::patientId, Comparator.nullsLast(String::compareTo)));

        int unchanged = (int) diffs.stream().filter(d -> d.changeType() == AssignmentChangeType.UNCHANGED).count();
        int changed = diffs.size() - unchanged;
        return new AssignmentPreview(
                diffs,
                changed,
                unchanged,
                proposal != null ? proposal.baselineZ() : 0.0,
                proposal != null ? proposal.proposedZ() : 0.0
        );
    }

    @Override
    public void setPendingProposal(String departmentId, AssignmentProposal proposal) {
        if (departmentId == null) return;
        if (proposal == null) {
            pendingByDepartmentId.remove(departmentId);
            return;
        }
        pendingByDepartmentId.put(departmentId, proposal);
    }

    @Override
    public AssignmentProposal getPendingProposal(String departmentId) {
        if (departmentId == null) return null;
        return pendingByDepartmentId.get(departmentId);
    }

    @Override
    public AssignmentState approvePendingProposal(String departmentId, AssignmentState currentState) {
        AssignmentProposal pending = getPendingProposal(departmentId);
        if (pending == null || !pending.feasible() || pending.proposedState() == null) {
            return currentState != null ? new AssignmentState(currentState) : new AssignmentState();
        }
        pendingByDepartmentId.remove(departmentId);
        return new AssignmentState(pending.proposedState());
    }

    @Override
    public AssignmentState rejectPendingProposal(String departmentId, AssignmentState currentState) {
        if (departmentId != null) {
            pendingByDepartmentId.remove(departmentId);
        }
        return currentState != null ? new AssignmentState(currentState) : new AssignmentState();
    }

    @Override
    public void admitPatient(Department department, Patient patient) {
        if (department == null || patient == null || patient.getId() == null) return;
        boolean exists = department.getWaitingList().stream()
                .anyMatch(p -> p != null && patient.getId().equals(p.getId()));
        if (!exists) {
            patient.setStatus(PatientStatus.WAITING);
            department.getWaitingList().add(patient);
        }
    }

    @Override
    public AssignmentState dischargePatient(Department department, String patientId, AssignmentState currentState) {
        AssignmentState next = currentState != null ? new AssignmentState(currentState) : new AssignmentState();
        if (patientId == null) return next;

        next.unassign(patientId);
        if (department != null) {
            for (Patient p : department.getWaitingList()) {
                if (p != null && patientId.equals(p.getId())) {
                    p.setStatus(PatientStatus.DISCHARGED);
                }
            }
            department.getWaitingList().removeIf(p -> p != null && patientId.equals(p.getId()));
        }
        return next;
    }

    @Override
    public List<Patient> buildWaitingQueueView(Department department) {
        if (department == null) return List.of();
        List<Patient> queue = new ArrayList<>();
        for (Patient p : department.getWaitingList()) {
            if (p != null && p.getStatus() == PatientStatus.WAITING && !p.isTemporarilyUnavailable()) {
                queue.add(p);
            }
        }
        queue.sort(WaitingListComparatorFactory.forGlobalQueue());
        return queue;
    }

    private static AssignmentChangeType classifyChange(String fromBedId, String toBedId) {
        if (fromBedId == null && toBedId == null) return AssignmentChangeType.UNCHANGED;
        if (fromBedId == null) return AssignmentChangeType.ASSIGNED;
        if (toBedId == null) return AssignmentChangeType.UNASSIGNED;
        if (fromBedId.equals(toBedId)) return AssignmentChangeType.UNCHANGED;
        return AssignmentChangeType.MOVED;
    }
}
