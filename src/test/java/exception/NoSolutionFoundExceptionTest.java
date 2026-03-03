package exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoSolutionFoundExceptionTest {

    @Test
    void isInstanceOfAssignmentException() {
        NoSolutionFoundException e = new NoSolutionFoundException("no solution");
        assertInstanceOf(AssignmentException.class, e);
    }

    @Test
    void message_isPreserved() {
        NoSolutionFoundException e = new NoSolutionFoundException("no beds");
        assertEquals("no beds", e.getMessage());
    }
}
