package Algorithm.risk;

import Config.AlgorithmConfig;


// Factory for the risk matrix from the algorithm config
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
