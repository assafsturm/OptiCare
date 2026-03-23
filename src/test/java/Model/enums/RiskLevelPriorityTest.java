package Model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskLevelPriorityTest {

    @Test
    void waitingQueuePriority_matchesPlan() {
        assertEquals(0, RiskLevel.IMMUNO_COMPROMISED.waitingQueuePriority());
        assertEquals(1, RiskLevel.INFECTIOUS.waitingQueuePriority());
        assertEquals(2, RiskLevel.RESPIRATORY.waitingQueuePriority());
        assertEquals(3, RiskLevel.CLEAN.waitingQueuePriority());
        assertEquals(4, RiskLevel.UNKNOWN.waitingQueuePriority());
    }
}
