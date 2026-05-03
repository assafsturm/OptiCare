package Algorithm;

import Algorithm.feasibility.FeasibilityChecker;
import Algorithm.feasibility.FeasibilityResult;
import Algorithm.feasibility.HardConstraints;
import Algorithm.fixtures.Stage3FixtureFactory;
import Algorithm.greedy.GreedyWarmStart;
import Algorithm.risk.RiskMatrixFactory;
import Algorithm.sa.SaResult;
import Algorithm.sa.SimulatedAnnealingEngine;
import Algorithm.topology.RoomTopologyGraph;
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
 * Tiered smoke tests for larger-but-budgeted runs (full 50×100-style stress is impractical for default CI timeouts).
 */
class ScaledAlgorithmSmokeTest {

    @Test
    void mediumDepartment_greedyAndSa_respectsIterationCapAndStaysDeterministic() {
        int roomCount = 8;
        int bedsPerRoom = 6;
        int waitingPatients = 28;

        List<Room> rooms = new ArrayList<>();
        for (int r = 0; r < roomCount; r++) {
            String roomId = "SR" + r;
            List<Bed> beds = new ArrayList<>();
            for (int b = 0; b < bedsPerRoom; b++) {
                String bedId = "SB_" + r + "_" + b;
                beds.add(new Bed(bedId, roomId, BedType.REGULAR, false, false));
            }
            rooms.add(new Room(roomId, "D_SCALED", bedsPerRoom, beds, 3.0 + r * 0.5, false, true));
        }

        Instant base = Instant.parse("2026-04-01T08:00:00Z");
        List<Patient> waiting = new ArrayList<>();
        for (int i = 0; i < waitingPatients; i++) {
            waiting.add(Stage3FixtureFactory.waiting("SCALE_P" + i, RiskLevel.CLEAN, 2, base.plusSeconds(i)));
        }
        Department department = new Department("D_SCALED", "ScaledSmoke", rooms, waiting);

        Map<String, Patient> patientById = new LinkedHashMap<>();
        for (Patient p : department.getWaitingList()) {
            patientById.put(p.getId(), p);
        }

        AlgorithmConfig config = new AlgorithmConfig();
        config.setRandomSeed(991L);
        config.setMaxTotalIterations(900);
        config.setIterationsPerTemperature(60);
        config.setNeighborSampleAttemptsPerIteration(40);
        config.setInitialTemperature(5_000.0);
        config.setCoolingRate(0.92);
        config.setMinTemperature(0.05);

        FeasibilityChecker feasibilityChecker = new FeasibilityChecker(config);
        FeasibilityResult feasibilityResult = feasibilityChecker.check(department, patientById, new AssignmentState());
        assertTrue(feasibilityResult.isFeasible(), feasibilityResult.getViolations()::toString);

        HardConstraints hardConstraints = new HardConstraints(RiskMatrixFactory.fromConfig(config), department);
        AssignmentState warm = GreedyWarmStart.build(department, patientById, new AssignmentState(), hardConstraints);

        AssignmentState baseline = new AssignmentState(warm);
        CostCalculator calculator = new CostCalculator(RiskMatrixFactory.fromConfig(config), config);

        SimulatedAnnealingEngine engine = new SimulatedAnnealingEngine();
        SaResult once = engine.run(department, patientById, new AssignmentState(warm), baseline, calculator,
                config, hardConstraints);

        SaResult repeat = engine.run(department, patientById, new AssignmentState(warm), baseline, calculator,
                config, hardConstraints);

        assertNotNull(once.bestState());
        assertTrue(once.iterations() > 0);
        assertTrue(once.iterations() <= config.getMaxTotalIterations());
        assertEquals(once.bestZ(), repeat.bestZ(), 1e-6);
        assertEquals(once.iterations(), repeat.iterations());
    }

    @Test
    void mediumTopologyGraph_floydWarshall_precomputeCompletes() {
        int nodes = 32;
        RoomTopologyGraph graph = new RoomTopologyGraph();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
            String id = "NR" + i;
            ids.add(id);
            graph.addRoom(id);
        }
        for (int i = 0; i < nodes; i++) {
            String from = ids.get(i);
            String to = ids.get((i + 1) % nodes);
            graph.addEdge(from, to, 1.0);
            graph.addEdge(to, from, 1.0);
        }
        graph.precomputeAllPairsShortestPaths();

        double dist = graph.getShortestPathDistance(ids.get(0), ids.get(nodes - 1));
        assertTrue(Double.isFinite(dist));
        assertTrue(dist >= 0.0);
    }
}
