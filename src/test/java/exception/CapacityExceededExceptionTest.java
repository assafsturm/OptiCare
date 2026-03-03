package exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CapacityExceededExceptionTest {

    @Test
    void isInstanceOfAssignmentException() {
        CapacityExceededException e = new CapacityExceededException("full");
        assertInstanceOf(AssignmentException.class, e);
    }

    @Test
    void message_isPreserved() {
        CapacityExceededException e = new CapacityExceededException("room full");
        assertEquals("room full", e.getMessage());
    }
}
