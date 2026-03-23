package Algorithm;

import Algorithm.risk.RiskMatrix;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskMatrixTest {

    private static final double BIG_M = 1_000_000;

    @Test
    void forbiddenPairs_returnBigM() {
        RiskMatrix matrix = new RiskMatrix(BIG_M);
        assertEquals(BIG_M, matrix.getPenalty(RiskLevel.INFECTIOUS, RiskLevel.IMMUNO_COMPROMISED));
        assertEquals(BIG_M, matrix.getPenalty(RiskLevel.IMMUNO_COMPROMISED, RiskLevel.INFECTIOUS));
        assertEquals(BIG_M, matrix.getPenalty(RiskLevel.INFECTIOUS, RiskLevel.CLEAN));
        assertEquals(BIG_M, matrix.getPenalty(RiskLevel.CLEAN, RiskLevel.INFECTIOUS));
        assertEquals(BIG_M, matrix.getPenalty(RiskLevel.RESPIRATORY, RiskLevel.IMMUNO_COMPROMISED));
        assertEquals(BIG_M, matrix.getPenalty(RiskLevel.IMMUNO_COMPROMISED, RiskLevel.RESPIRATORY));
    }

    @Test
    void softPenalty_respiratoryClean() {
        RiskMatrix matrix = new RiskMatrix(BIG_M);
        assertEquals(500, matrix.getPenalty(RiskLevel.RESPIRATORY, RiskLevel.CLEAN));
        assertEquals(500, matrix.getPenalty(RiskLevel.CLEAN, RiskLevel.RESPIRATORY));
    }

    @Test
    void safePairs_returnZero() {
        RiskMatrix matrix = new RiskMatrix(BIG_M);
        assertEquals(0, matrix.getPenalty(RiskLevel.CLEAN, RiskLevel.CLEAN));
        assertEquals(0, matrix.getPenalty(RiskLevel.RESPIRATORY, RiskLevel.RESPIRATORY));
    }

    @Test
    void unknownPairs_areFiniteNotBigM() {
        RiskMatrix matrix = new RiskMatrix(BIG_M);
        assertTrue(matrix.getPenalty(RiskLevel.UNKNOWN, RiskLevel.CLEAN) < BIG_M);
        assertTrue(matrix.getPenalty(RiskLevel.UNKNOWN, RiskLevel.IMMUNO_COMPROMISED) < BIG_M);
        assertEquals(5_000, matrix.getPenalty(RiskLevel.UNKNOWN, RiskLevel.UNKNOWN));
    }

    @Test
    void setPenalty_overridesDefault() {
        RiskMatrix matrix = new RiskMatrix(BIG_M);
        matrix.setPenalty(RiskLevel.CLEAN, RiskLevel.CLEAN, 100);
        assertEquals(100, matrix.getPenalty(RiskLevel.CLEAN, RiskLevel.CLEAN));
    }

    @Test
    void getPenalty_withNull_returnsZero() {
        RiskMatrix matrix = new RiskMatrix(BIG_M);
        assertEquals(0, matrix.getPenalty(null, RiskLevel.CLEAN));
        assertEquals(0, matrix.getPenalty(RiskLevel.CLEAN, null));
    }
}
