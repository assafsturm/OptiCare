package Algorithm;

import Algorithm.cost.ClinicalCostStrategy;
import Algorithm.cost.CostStrategy;
import Algorithm.cost.PolicyCostStrategy;
import Algorithm.cost.SafetyCostStrategy;
import Algorithm.cost.TransferCostStrategy;
import Algorithm.cost.UnassignedCostStrategy;
import Algorithm.risk.RiskMatrix;
import Algorithm.topology.RoomTopologyGraph;
import Config.AlgorithmConfig;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;

import java.util.List;
import java.util.Map;

/**
 * Computes total cost Z and components (C_safety, C_clinical, C_policy, C_transfer, C_unassigned)
 * by delegating to {@link CostStrategy} implementations.
 */
public class CostCalculator {

    private final List<CostStrategy> strategies;
    private final SafetyCostStrategy safety;
    private final ClinicalCostStrategy clinical;
    private final PolicyCostStrategy policy;
    private final TransferCostStrategy transfer;
    private final AlgorithmConfig config;

    public CostCalculator(RiskMatrix riskMatrix, AlgorithmConfig config) {
        this(riskMatrix, config, null);
    }

    public CostCalculator(RiskMatrix riskMatrix, AlgorithmConfig config, RoomTopologyGraph topologyGraph) {
        this.config = config;
        this.safety = new SafetyCostStrategy(riskMatrix);
        this.clinical = new ClinicalCostStrategy(config);
        this.policy = new PolicyCostStrategy(config);
        this.transfer = new TransferCostStrategy(config, topologyGraph);
        this.strategies = List.of(
                safety,
                clinical,
                policy,
                transfer,
                new UnassignedCostStrategy(config)
        );
    }

    /**
     * Z_total = C_safety + C_clinical + C_policy + C_transfer + C_unassigned.
     * Use this overload when no baseline exists; C_transfer will be 0.
     */
    public double computeZ(AssignmentState state, Department department, Map<String, Patient> patientById) {
        return computeZ(state, department, patientById, null);
    }

    /**
     * Z with optional baseline for C_transfer.
     */
    public double computeZ(AssignmentState state, Department department, Map<String, Patient> patientById,
                           AssignmentState initialState) {
        double z = 0;
        for (CostStrategy s : strategies) {
            z += s.computeTotal(state, department, patientById, initialState);
        }
        return z;
    }

    /** C_safety: cohorting within the same room. */
    public double computeCSafety(Patient patient, Room room, AssignmentState state, Department department,
                                 Map<String, Patient> patientById, String currentPatientId, Bed currentBed) {
        return safety.computeForPatient(patient, room, state, patientById, currentPatientId, currentBed);
    }

    /** C_clinical: bed fit. */
    public double computeCClinical(Patient patient, Bed bed, Room room) {
        return clinical.computeForPatient(patient, bed, room);
    }

    /** C_policy: distance × severity. */
    public double computeCPolicy(Patient patient, Room room) {
        return policy.computeForPatient(patient, room);
    }

    /** C_transfer: move vs baseline. */
    public double computeCTransfer(String patientId, Bed currentBed, AssignmentState state, AssignmentState initialState) {
        return transfer.computeForPatient(patientId, currentBed, state, initialState);
    }

    /** C_unassigned = W_unassigned × eligible unassigned waiting count. */
    public double computeCUnassigned(AssignmentState state, Department department) {
        return config.getUnassignedPenaltyWeight()
                * UnassignedCostStrategy.countUnassignedEligibleWaiting(department, state);
    }
}
