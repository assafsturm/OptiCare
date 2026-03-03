package exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidAssignmentExceptionTest {

    @Test
    void isInstanceOfAssignmentException() {
        InvalidAssignmentException e = new InvalidAssignmentException("invalid");
        assertInstanceOf(AssignmentException.class, e);
    }

    @Test
    void message_isPreserved() {
        InvalidAssignmentException e = new InvalidAssignmentException("cohorting violation");
        assertEquals("cohorting violation", e.getMessage());
    }
}
