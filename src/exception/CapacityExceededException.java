package exception;

/**
 * Thrown when room or global capacity is exceeded.
 */
public class CapacityExceededException extends AssignmentException {

    public CapacityExceededException(String message) {
        super(message);
    }

    public CapacityExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
