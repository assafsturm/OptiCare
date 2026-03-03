package exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentExceptionTest {

    @Test
    void message_isPreserved() {
        AssignmentException e = new AssignmentException("test message");
        assertEquals("test message", e.getMessage());
    }

    @Test
    void cause_isPreserved() {
        Throwable cause = new RuntimeException("cause");
        AssignmentException e = new AssignmentException("wrapper", cause);
        assertEquals("wrapper", e.getMessage());
        assertSame(cause, e.getCause());
    }
}
