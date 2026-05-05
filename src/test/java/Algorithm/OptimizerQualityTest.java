package Algorithm;

import Algorithm.feasibility.FeasibilityChecker;
import Algorithm.feasibility.FeasibilityResult;
import Algorithm.feasibility.HardConstraints;
import Algorithm.fixtures.Stage3FixtureFactory;
import Algorithm.greedy.GreedyWarmStart;
import Algorithm.risk.RiskMatrixFactory;
import Algorithm.sa.SaResult;
import Algorithm.sa.SimulatedAnnealingEngine;
import Algorithm.testsupport.BruteForceLegalAssignmentSearch;
import Algorithm.testsupport.BruteForceLegalAssignmentSearch.OptimalAssignment;
import Config.AlgorithmConfig;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves solution qualityâ€”not only that the optimizer runsâ€”with micro instances whose true optimum is known
 * (exhaustive search) or deliberately worse than greedy.
 */
class OptimizerQualityTest {

    /**
     * Two CLEAN patients differing only in severity: greedy places higher-severity first and, with bed sorting,
     * can pick the clinically legal but policy-expensive farther room first. Exhaustive search finds the cheaper
     * placement; SA should converge to that same objective under a generous iteration budget on this tiny landscape.
     */
    @Test
    void bruteForceOptimal_isBetterThanGreedy_saConvergesToBruteMinimum() {
        Instant t0 = Instant.parse("2026-05-15T09:00:00Z");

        Bed bFar = new Bed("BFar", "R_far", BedType.REGULAR, false);
        Room rFar = new Room("R_far", "D_BF", 1, new ArrayList<>(List.of(bFar)), 100.0, false);

        Bed bNear = new Bed("BNear", "R_near", BedType.REGULAR, false);
        Room rNear = new Room("R_near", "D_BF", 1, new ArrayList<>(List.of(bNear)), 10.0, false);

        Patient pHighSeverity = Stage3FixtureFactory.waiting("P_hi", RiskLevel.CLEAN, 50, t0);
        Patient pLowSeverity = Stage3FixtureFactory.waiting("P_lo", RiskLevel.CLEAN, 1, t0.plusSeconds(60));

        List<Patient> waiting = List.of(pHighSeverity, pLowSeverity);
        Department department = new Department("D_BF", "MicroPolicy", List.of(rFar, rNear), new ArrayList<>(waiting));

        Map<String, Patient> patientById = new LinkedHashMap<>();
        patientById.put(pHighSeverity.getId(), pHighSeverity);
        patientById.put(pLowSeverity.getId(), pLowSeverity);

        AlgorithmConfig config = new AlgorithmConfig();
        config.setRandomSeed(7L);
        config.setNeighborSampleAttemptsPerIteration(96);
        config.setIterationsPerTemperature(80);
        config.setInitialTemperature(5_000.0);
        config.setCoolingRate(0.88);
        config.setMinTemperature(0.01);
        config.setMaxTotalIterations(35_000);
        HardConstraints hardConstraints = new HardConstraints(RiskMatrixFactory.fromConfig(config), department);
        CostCalculator calculator = new CostCalculator(RiskMatrixFactory.fromConfig(config), config);

        FeasibilityResult feasibility = new FeasibilityChecker(config).check(department, patientById,
                new AssignmentState());
        assertTrue(feasibility.isFeasible(), feasibility.getViolations()::toString);

        AssignmentState greedyWarm = GreedyWarmStart.build(department, patientById, new AssignmentState(),
                hardConstraints);
        AssignmentState baselineForTransfer = new AssignmentState(greedyWarm);
        double greedyZ = calculator.computeZ(greedyWarm, department, patientById, baselineForTransfer);

        List<Bed> allBeds = List.of(bFar, bNear);
        OptimalAssignment optimum = BruteForceLegalAssignmentSearch.findMinimumZ(
                department,
                patientById,
                waiting,
                allBeds,
                baselineForTransfer,
                calculator,
                hardConstraints
        );

        assertTrue(optimum.foundAnyLegal(), () -> "tried=" + optimum.totalAssignmentsTried()
                + " legal=" + optimum.legalAssignmentCount());
        assertEquals(2, optimum.legalAssignmentCount(), "Exactly two injective assignments exist for two beds.");

        double bruteZ = optimum.bestZ();
        assertTrue(bruteZ < greedyZ - 100,
                () -> "Expected greedy strictly worse here; bruteZ=" + bruteZ + " greedyZ=" + greedyZ);

        SimulatedAnnealingEngine engine = new SimulatedAnnealingEngine();
        SaResult run = engine.run(
                department,
                patientById,
                new AssignmentState(greedyWarm),
                baselineForTransfer,
                calculator,
                config,
                hardConstraints
        );

        assertNotNull(run.bestState());

        double saZ = run.bestZ();
        assertEquals(bruteZ, saZ, 1.0,
                "SA must reach the enumerated global minimum on this two-assignment discrete landscape.");

        SaResult rerun = engine.run(
                department,
                patientById,
                new AssignmentState(greedyWarm),
                baselineForTransfer,
                calculator,
                config,
                hardConstraints
        );
        assertEquals(saZ, rerun.bestZ(), 1e-5);
        assertEquals(run.iterations(), rerun.iterations());
    }

    /**
     * Exhaustive sanity check on three beds / two CLEAN patients where every injective placement is hard-valid:
     * optimum must match SA with a seeded, budgeted run.
     */
    @Test
    void threeBeds_twoPatients_saMatchesPureBruteForce() {
        Instant t0 = Instant.parse("2026-06-01T10:00:00Z");

        List<Bed> beds = List.of(
                new Bed("B0", "R0", BedType.REGULAR, false),
                new Bed("B1", "R1", BedType.REGULAR, false),
                new Bed("B2", "R2", BedType.REGULAR, false));
        Room r0 = new Room("R0", "D3", 1, new ArrayList<>(List.of(beds.get(0))), 22.0, false);
        Room r1 = new Room("R1", "D3", 1, new ArrayList<>(List.of(beds.get(1))), 8.0, false);
        Room r2 = new Room("R2", "D3", 1, new ArrayList<>(List.of(beds.get(2))), 40.0, false);

        Patient pa = Stage3FixtureFactory.waiting("PA", RiskLevel.CLEAN, 40, t0);
        Patient pb = Stage3FixtureFactory.waiting("PB", RiskLevel.CLEAN, 35, t0.plusSeconds(30));

        Department department = new Department("D3", "Enumerate6", List.of(r0, r1, r2), List.of(pa, pb));
        Map<String, Patient> patientById = Stage3FixtureFactory.patientIndex(department);

        AlgorithmConfig config = new AlgorithmConfig();
        config.setRandomSeed(1003L);
        config.setIterationsPerTemperature(60);
        config.setNeighborSampleAttemptsPerIteration(90);
        config.setInitialTemperature(4_000.0);
        config.setCoolingRate(0.9);
        config.setMinTemperature(0.01);
        config.setMaxTotalIterations(45_000);

        HardConstraints hardConstraints = new HardConstraints(RiskMatrixFactory.fromConfig(config), department);
        CostCalculator calculator = new CostCalculator(RiskMatrixFactory.fromConfig(config), config);

        AssignmentState greedy = GreedyWarmStart.build(department, patientById, new AssignmentState(), hardConstraints);
        AssignmentState baseline = new AssignmentState(greedy);

        OptimalAssignment optimum = BruteForceLegalAssignmentSearch.findMinimumZ(
                department,
                patientById,
                List.of(pa, pb),
                List.of(r0.getBeds().get(0), r1.getBeds().get(0), r2.getBeds().get(0)),
                baseline,
                calculator,
                hardConstraints
        );

        assertTrue(optimum.foundAnyLegal());
        assertTrue(optimum.legalAssignmentCount() >= 1);

        SimulatedAnnealingEngine engine = new SimulatedAnnealingEngine();
        SaResult sa = engine.run(department, patientById, new AssignmentState(greedy), baseline, calculator,
                config, hardConstraints);

        assertEquals(optimum.bestZ(), sa.bestZ(), 2.0);
    }
}
