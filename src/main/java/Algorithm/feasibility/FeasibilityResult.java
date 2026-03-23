package Algorithm.feasibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a feasibility check: whether a valid assignment is possible,
 * and optional list of reasons why it is not.
 */
public class FeasibilityResult {

    private final boolean feasible;
    private final List<String> violations;

    public FeasibilityResult(boolean feasible, List<String> violations) {
        this.feasible = feasible;
        this.violations = violations != null ? new ArrayList<>(violations) : new ArrayList<>();
    }

    public static FeasibilityResult feasible() {
        return new FeasibilityResult(true, List.of());
    }

    public static FeasibilityResult infeasible(String... reasons) {
        return new FeasibilityResult(false, List.of(reasons));
    }

    public boolean isFeasible() {
        return feasible;
    }

    public List<String> getViolations() {
        return Collections.unmodifiableList(violations);
    }
}
