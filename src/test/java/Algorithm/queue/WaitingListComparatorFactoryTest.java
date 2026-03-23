package Algorithm.queue;

import Algorithm.fixtures.Stage3FixtureFactory;
import Model.entety.Patient;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaitingListComparatorFactoryTest {

    @Test
    void globalQueue_usesRiskSeverityAdmittedAtAndIdOrder() {
        Patient p1 = Stage3FixtureFactory.waiting("P1", RiskLevel.RESPIRATORY, 7, Instant.parse("2026-03-01T11:00:00Z"));
        Patient p2 = Stage3FixtureFactory.waiting("P2", RiskLevel.IMMUNO_COMPROMISED, 3, Instant.parse("2026-03-01T12:00:00Z"));
        Patient p3 = Stage3FixtureFactory.waiting("P3", RiskLevel.IMMUNO_COMPROMISED, 9, Instant.parse("2026-03-01T10:00:00Z"));
        Patient p4 = Stage3FixtureFactory.waiting("P4", null, 10, Instant.parse("2026-03-01T09:00:00Z"));
        Patient p5 = Stage3FixtureFactory.waiting("P5", RiskLevel.RESPIRATORY, 7, Instant.parse("2026-03-01T11:00:00Z"));

        PriorityQueue<Patient> q = new PriorityQueue<>(WaitingListComparatorFactory.forGlobalQueue());
        q.addAll(List.of(p1, p2, p3, p4, p5));

        List<String> order = new ArrayList<>();
        while (!q.isEmpty()) {
            order.add(q.poll().getId());
        }

        assertEquals(List.of("P3", "P2", "P1", "P5", "P4"), order);
    }
}
