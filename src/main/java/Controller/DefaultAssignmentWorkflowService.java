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

// implementation of the assignment workflow service
public class DefaultAssignmentWorkflowService implements AssignmentWorkflowService {

    private final AlgorithmConfig config;// algorithm config
    private final SimulatedAnnealingEngine saEngine;// sa engine
    private final FeasibilityChecker feasibilityChecker;// feasibility checker
    private final Map<String, AssignmentProposal> pendingByDepartmentId = new HashMap<>();// pending proposals by department id

    public DefaultAssignmentWorkflowService(AlgorithmConfig config) {
        this(config, new SimulatedAnnealingEngine(), new FeasibilityChecker(config));
    }
    // overload constructors for mising params
    public DefaultAssignmentWorkflowService(AlgorithmConfig config,
                                            SimulatedAnnealingEngine saEngine,
                                            FeasibilityChecker feasibilityChecker) {
        this.config = config;
        this.saEngine = saEngine;
        this.feasibilityChecker = feasibilityChecker;
    }

    // propose an assignment for a department (run the optimization - greedy warm start + sa)
    @Override
    public AssignmentProposal proposeAssignment(Department department, Map<String, Patient> patientById,
                                                AssignmentState currentState) {
        AlgorithmTrace.log("workflow", "Starting proposal for department="
                + (department != null ? department.getId() : "null")
                + ", patients=" + (patientById != null ? patientById.size() : 0));
        AssignmentState baselineInput = currentState != null ? currentState : new AssignmentState();
        FeasibilityResult feasibility = feasibilityChecker.check(department, patientById, baselineInput);// check if the assignment is feasible
        if (!feasibility.isFeasible()) {// if not feasible, return a proposal with the violations
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

        RiskMatrix riskMatrix = RiskMatrixFactory.fromConfig(config);// create a risk matrix from the config
        HardConstraints hardConstraints = new HardConstraints(riskMatrix, department);// create hard constraints from the risk matrix and the department
        AssignmentState warmStartState = GreedyWarmStart.build(department, patientById, baselineInput, hardConstraints);// build a greedy start state from the department, patients and hard constraints
        AlgorithmTrace.log("workflow", "Warm start completed. Assigned count=" + warmStartState.size());
        AssignmentState baselineForTransfer = new AssignmentState(warmStartState);// copy the greedy start state (shalow)
        CostCalculator costCalculator = new CostCalculator(riskMatrix, config);// create a cost calculator from the risk matrix and the config

        double baselineZ = costCalculator.computeZ(baselineForTransfer, department, patientById, baselineForTransfer);// compute the baseline energy
        AlgorithmTrace.log("workflow", "Baseline energy (Z)=" + baselineZ);
        SaResult result = saEngine.run( // run the sa
                department,
                patientById,
                new AssignmentState(warmStartState),
                baselineForTransfer,
                costCalculator,
                config,
                hardConstraints
        );
        double proposedZ = result.bestZ();// get the best energy
        AssignmentState proposedState = result.bestState();// get the best state
        List<String> warnings = unplacedWaitingWarnings(department, proposedState);// get the warnings
        AlgorithmTrace.log("workflow", "SA finished. proposedZ=" + proposedZ
                + ", iterations=" + result.iterations()
                + ", stoppedByTime=" + result.stoppedByTimeLimit()
                + ", placementWarnings=" + warnings.size());
        return new AssignmentProposal(// return a proposal with the results
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


    // waiting patients who remain eligible but have no bed in the proposed state after optimization
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

    // build a preview of the assignment proposal
    @Override
    public AssignmentPreview buildPreview(AssignmentProposal proposal) {
        AssignmentState baseline = proposal != null && proposal.baselineState() != null
                ? proposal.baselineState() : new AssignmentState();// get the baseline state
        AssignmentState proposed = proposal != null && proposal.proposedState() != null
                ? proposal.proposedState() : new AssignmentState();// get the proposed state

        Set<String> patientIds = new HashSet<>();// get the patient ids, no duplicates
        patientIds.addAll(baseline.getAssignments().keySet());// add the patient ids from the baseline
        patientIds.addAll(proposed.getAssignments().keySet());// add the patient ids from the proposed

        List<PatientAssignmentDiff> diffs = new ArrayList<>();
        for (String patientId : patientIds) {// for each patient, get the from and to bed ids
            String fromBedId = baseline.getBed(patientId) != null ? baseline.getBed(patientId).getId() : null;
            String toBedId = proposed.getBed(patientId) != null ? proposed.getBed(patientId).getId() : null;
            AssignmentChangeType type = classifyChange(fromBedId, toBedId);// classify the change
            diffs.add(new PatientAssignmentDiff(patientId, fromBedId, toBedId, type));// add the diff to the list
        }
        diffs.sort(Comparator.comparing(PatientAssignmentDiff::patientId, Comparator.nullsLast(String::compareTo)));// sort the diffs by the patient id

        int unchanged = (int) diffs.stream().filter(d -> d.changeType() == AssignmentChangeType.UNCHANGED).count(); //stream the diffs and count the unchanged ones
        int changed = diffs.size() - unchanged; // count the changed ones
        return new AssignmentPreview(// return a preview with the results
                diffs,
                changed,
                unchanged,// count the unchanged ones
                proposal != null ? proposal.baselineZ() : 0.0,// get the baseline energy
                proposal != null ? proposal.proposedZ() : 0.0// get the proposed energy
        );
    }

    // set the pending proposal for a department in the map
    @Override
    public void setPendingProposal(String departmentId, AssignmentProposal proposal) {
        if (departmentId == null) return;
        if (proposal == null) {
            pendingByDepartmentId.remove(departmentId);
            return;// if proposal is null, 
        }
        pendingByDepartmentId.put(departmentId, proposal);// esle put the proposal in the map
    }

    // get the pending proposal for a department from the map
    @Override
    public AssignmentProposal getPendingProposal(String departmentId) {
        if (departmentId == null) return null;
        return pendingByDepartmentId.get(departmentId);
    }
    //approve the pending proposal for a department and return the new current assignment state
    @Override
    public AssignmentState approvePendingProposal(String departmentId, AssignmentState currentState) {
        AssignmentProposal pending = getPendingProposal(departmentId);
        if (pending == null || !pending.feasible() || pending.proposedState() == null) {
            return currentState != null ? new AssignmentState(currentState) : new AssignmentState();
        }
        pendingByDepartmentId.remove(departmentId);// remove the proposal from pending
        return new AssignmentState(pending.proposedState());// return the new current assignment state
    }

    // reject the pending proposal for a department and return the unchanged current assignment state
    @Override
    public AssignmentState rejectPendingProposal(String departmentId, AssignmentState currentState) {
        if (departmentId != null) {
            pendingByDepartmentId.remove(departmentId); // remove the proposal from pending
        }
        return currentState != null ? new AssignmentState(currentState) : new AssignmentState();
    }

    // admit a patient into the department waiting list
    @Override
    public void admitPatient(Department department, Patient patient) {
        if (department == null || patient == null || patient.getId() == null) return;
        boolean exists = department.getWaitingList().stream() // check if the patient is already in the waiting list
                .anyMatch(p -> p != null && patient.getId().equals(p.getId()));
        if (!exists) {
            patient.setStatus(PatientStatus.WAITING); // set the patient status to waiting
            department.getWaitingList().add(patient); // add the patient to the waiting list
        }
    }

    // discharge a patient from the department and return the assignment state after the discharge
    @Override
    public AssignmentState dischargePatient(Department department, String patientId, AssignmentState currentState) {
        AssignmentState next = currentState != null ? new AssignmentState(currentState) : new AssignmentState();
        if (patientId == null) return next;

        next.unassign(patientId); // unassign the patient from the bed if assigned
        if (department != null) {
            for (Patient p : department.getWaitingList()) { // check if the patient is in the waiting list
                if (p != null && patientId.equals(p.getId())) {
                    p.setStatus(PatientStatus.DISCHARGED); // set the patient status to discharged
                }
            }
            department.getWaitingList().removeIf(p -> p != null && patientId.equals(p.getId()));
        }
        return next;
    }
    // build a waiting list view for the department 
    @Override
    public List<Patient> buildWaitingQueueView(Department department) {
        if (department == null) return List.of();
        List<Patient> queue = new ArrayList<>();
        for (Patient p : department.getWaitingList()) {
            if (p != null && p.getStatus() == PatientStatus.WAITING && !p.isTemporarilyUnavailable()) {
                queue.add(p);
            }
        }
        queue.sort(WaitingListComparatorFactory.forGlobalQueue()); // sort the waiting list by the comparator
        return queue;
    }

    // classify the change type between two beds
    private static AssignmentChangeType classifyChange(String fromBedId, String toBedId) {
        if (fromBedId == null && toBedId == null) return AssignmentChangeType.UNCHANGED; //missing in both sates
        if (fromBedId == null) return AssignmentChangeType.ASSIGNED;// not in baseline state but in proposed state
        if (toBedId == null) return AssignmentChangeType.UNASSIGNED; // to = null and from = not null, so unassigned
        if (fromBedId.equals(toBedId)) return AssignmentChangeType.UNCHANGED; // same bed in both states
        return AssignmentChangeType.MOVED; // moved to a diffrent bed
    }
}
