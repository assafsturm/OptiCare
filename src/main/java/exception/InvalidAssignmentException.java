package exception;

/**
 * Thrown when an assignment violates constraints (e.g. cohorting, equipment mismatch).
 */
public class InvalidAssignmentException extends AssignmentException {

    public InvalidAssignmentException(String message) {
        super(message);
    }

    public InvalidAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
