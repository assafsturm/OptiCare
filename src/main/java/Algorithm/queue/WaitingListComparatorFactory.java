package Algorithm.queue;

import Model.entety.ClinicalData;
import Model.entety.Patient;
import Model.enums.RiskLevel;

import java.time.Instant;
import java.util.Comparator;

/**
 * Deterministic waiting-list ordering comparator as specified in PLAN.
 * Tuple: (riskPriority, -severityScore, admittedAt, patientId).
 */
public final class WaitingListComparatorFactory {

    private WaitingListComparatorFactory() {
    }

    public static Comparator<Patient> forGlobalQueue() {
        return Comparator
                .comparingInt(WaitingListComparatorFactory::riskPriority)
                .thenComparing(Comparator.comparingInt(WaitingListComparatorFactory::severityScore).reversed())
                .thenComparing(WaitingListComparatorFactory::admittedAtOrMax)
                .thenComparing(WaitingListComparatorFactory::patientIdOrMax);
    }

    private static int riskPriority(Patient p) {
        ClinicalData cd = p != null ? p.getClinicalData() : null;
        RiskLevel level = (cd == null || cd.getRiskLevel() == null) ? RiskLevel.UNKNOWN : cd.getRiskLevel();
        return level.waitingQueuePriority();
    }

    private static int severityScore(Patient p) {
        ClinicalData cd = p != null ? p.getClinicalData() : null;
        return cd != null ? cd.getSeverityScore() : Integer.MIN_VALUE;
    }

    private static Instant admittedAtOrMax(Patient p) {
        return p != null && p.getAdmittedAt() != null ? p.getAdmittedAt() : Instant.MAX;
    }

    private static String patientIdOrMax(Patient p) {
        return p != null && p.getId() != null ? p.getId() : "\uFFFF";
    }
}
