package Algorithm.greedy;

import Algorithm.AssignmentState;
import Algorithm.CostCalculator;
import Algorithm.feasibility.HardConstraints;
import Algorithm.fixtures.Stage3FixtureFactory;
import Algorithm.risk.RiskMatrixFactory;
import Config.AlgorithmConfig;
import Model.entety.Department;
import Model.entety.Patient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreedyWarmStartTest {

    @Test
    void greedy_placesEligibleWaiting_patientsOnBeds() {
        AlgorithmConfig config = new AlgorithmConfig();
        Department dept = Stage3FixtureFactory.mediumDepartment();
        Map<String, Patient> byId = Stage3FixtureFactory.patientIndex(dept);
        HardConstraints hc = new HardConstraints(RiskMatrixFactory.fromConfig(config), dept);
        AssignmentState baseline = new AssignmentState();
        AssignmentState warm = GreedyWarmStart.build(dept, byId, baseline, hc);
        CostCalculator calc = new CostCalculator(RiskMatrixFactory.fromConfig(config), config);
        double z = calc.computeZ(warm, dept, byId, warm);
        assertTrue(z >= 0);
        assertTrue(warm.size() >= 1);
        for (String pid : byId.keySet()) {
            if (pid.startsWith("W")) {
                assertNotNull(warm.getBed(pid), "expected greedy to assign " + pid);
            }
        }
    }
}
