package Algorithm.risk;

import Config.AlgorithmConfig;

/**
 * Single construction path for {@link RiskMatrix} from {@link AlgorithmConfig} (Big M source of truth).
 */
public final class RiskMatrixFactory {

    private RiskMatrixFactory() {
    }

    public static RiskMatrix fromConfig(AlgorithmConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        return new RiskMatrix(config.getBigM());
    }
}
