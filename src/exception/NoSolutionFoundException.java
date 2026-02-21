package exception;

/**
 * Thrown when the optimization algorithm could not find a valid assignment
 * (e.g. more patients than beds, or no feasible bed for a patient under hard constraints).
 */
public class NoSolutionFoundException extends AssignmentException {

    public NoSolutionFoundException(String message) {
        super(message);
    }

    public NoSolutionFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
