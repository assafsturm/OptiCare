package Config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlgorithmConfigTest {

    @Test
    void defaultValues_areSet() {
        AlgorithmConfig config = new AlgorithmConfig();
        assertEquals(10_000.0, config.getInitialTemperature());
        assertEquals(0.995, config.getCoolingRate());
        assertEquals(0.01, config.getMinTemperature());
        assertEquals(100, config.getIterationsPerTemperature());
        assertEquals(100_000, config.getMaxTotalIterations());
        assertEquals(1_000_000.0, config.getBigM());
        assertEquals(5_000.0, config.getTransferPenaltyWeight());
        assertEquals(500.0, config.getPolicyPenaltyWeight());
        assertEquals(0L, config.getMaxTimeMillis());
        assertEquals(80, config.getNeighborSampleAttemptsPerIteration());
    }

    @Test
    void setters_updateValues() {
        AlgorithmConfig config = new AlgorithmConfig();
        config.setInitialTemperature(5000);
        config.setBigM(2_000_000);
        config.setTransferPenaltyWeight(10_000);
        assertEquals(5000, config.getInitialTemperature());
        assertEquals(2_000_000, config.getBigM());
        assertEquals(10_000, config.getTransferPenaltyWeight());
    }
}
