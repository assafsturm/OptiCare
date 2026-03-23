package Algorithm;

import Algorithm.risk.RiskMatrix;
import Algorithm.risk.RiskMatrixFactory;
import Config.AlgorithmConfig;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskMatrixFactoryTest {

    @Test
    void fromConfig_usesBigMFromConfig() {
        AlgorithmConfig cfg = new AlgorithmConfig();
        cfg.setBigM(123_456);
        RiskMatrix m = RiskMatrixFactory.fromConfig(cfg);
        assertEquals(123_456, m.getPenalty(RiskLevel.INFECTIOUS, RiskLevel.CLEAN));
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void fromConfig_nullConfig_throws() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> RiskMatrixFactory.fromConfig(null));
        assertNotNull(ex);
    }
}
