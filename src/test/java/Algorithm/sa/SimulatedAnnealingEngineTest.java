package Algorithm.sa;

import Algorithm.AssignmentState;
import Algorithm.CostCalculator;
import Algorithm.feasibility.HardConstraints;
import Algorithm.fixtures.Stage3FixtureFactory;
import Algorithm.greedy.GreedyWarmStart;
import Algorithm.risk.RiskMatrixFactory;
import Config.AlgorithmConfig;
import Model.entety.Department;
import Model.entety.Patient;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedAnnealingEngineTest {

    @Test
    void sa_runsDeterministically_withSeed_producesStableBestZ() {
        AlgorithmConfig config = new AlgorithmConfig();
        config.setRandomSeed(42L);
        config.setMaxTotalIterations(200);
        config.setIterationsPerTemperature(20);
        config.setInitialTemperature(1000.0);
        config.setMinTemperature(0.1);
        config.setCoolingRate(0.9);
        config.setNeighborSampleAttemptsPerIteration(120);

        Department dept = Stage3FixtureFactory.mediumDepartment();
        Map<String, Patient> byId = new LinkedHashMap<>(Stage3FixtureFactory.patientIndex(dept));
        HardConstraints hc = new HardConstraints(RiskMatrixFactory.fromConfig(config), dept);
        AssignmentState warm = GreedyWarmStart.build(dept, byId, new AssignmentState(), hc);
        AssignmentState baseline = new AssignmentState(warm);
        CostCalculator calc = new CostCalculator(RiskMatrixFactory.fromConfig(config), config);

        SimulatedAnnealingEngine engine = new SimulatedAnnealingEngine();
        SaResult r1 = engine.run(dept, byId, new AssignmentState(warm), baseline, calc, config, hc);
        SaResult r2 = engine.run(dept, byId, new AssignmentState(warm), baseline, calc, config, hc);

        assertNotNull(r1.bestState());
        assertEquals(r1.bestZ(), r2.bestZ(), 1e-6);
        assertEquals(r1.iterations(), r2.iterations());
        assertTrue(r1.bestZ() >= 0);
    }
}
