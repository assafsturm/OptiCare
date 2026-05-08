package Algorithm.queue;

import Model.entety.ClinicalData;
import Model.entety.Patient;
import Model.enums.RiskLevel;

import java.time.Instant;
import java.util.Comparator;


// deterministic waiting list ordering comparator
// risk priority, severity score (reversed), admitted at, patient id, in that order
//controls who gets considered first in queue
public final class WaitingListComparatorFactory { //O(|w| log |w|) w for number of waiting 
// utility factory 
    private WaitingListComparatorFactory() {
    }

    public static Comparator<Patient> forGlobalQueue() {
        return Comparator // method reference to the comparator (like lambda eaule p -> func(p))
                .comparingInt(WaitingListComparatorFactory::riskPriority)
                .thenComparing(Comparator.comparingInt(WaitingListComparatorFactory::severityScore).reversed())
                .thenComparing(WaitingListComparatorFactory::admittedAtOrMax)
                .thenComparing(WaitingListComparatorFactory::patientIdOrMax);
    } // returns comparator that sorts patients

    private static int riskPriority(Patient p) {
        ClinicalData cd = p != null ? p.getClinicalData() : null;
        RiskLevel level = (cd == null || cd.getRiskLevel() == null) ? RiskLevel.UNKNOWN : cd.getRiskLevel();
        return level.waitingQueuePriority(); // in enum returns int so can be compared 
    }

    private static int severityScore(Patient p) {
        ClinicalData cd = p != null ? p.getClinicalData() : null;
        return cd != null ? cd.getSeverityScore() : Integer.MIN_VALUE; // if null return min value so it is last
    }

    private static Instant admittedAtOrMax(Patient p) {
        return p != null && p.getAdmittedAt() != null ? p.getAdmittedAt() : Instant.MAX; // if null return max so it is last
    }

    private static String patientIdOrMax(Patient p) {
        return p != null && p.getId() != null ? p.getId() : "\uFFFF"; // if null return very big in hopes it is last
    }
}
