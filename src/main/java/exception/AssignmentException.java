package exception;

/**
 * Base exception for assignment-related errors in the OptiCare optimization system.
 */
public class AssignmentException extends RuntimeException {

    public AssignmentException(String message) {
        super(message);
    }

    public AssignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
