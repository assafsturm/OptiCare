package Algorithm.neighborhood;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NeighborMoveTest {

    @Test
    void assign_buildsExpectedShape() {
        NeighborMove move = NeighborMove.assign("P1", "B2");
        assertEquals(MoveType.ASSIGN, move.getType());
        assertEquals("P1", move.getPatientIdA());
        assertEquals("B2", move.getToBedIdA());
        assertNull(move.getPatientIdB());
    }

    @Test
    void swap_buildsSymmetricUndoFields() {
        NeighborMove move = NeighborMove.swap("P1", "P2", "B1", "B3");
        assertEquals(MoveType.SWAP, move.getType());
        assertEquals("B1", move.getFromBedIdA());
        assertEquals("B3", move.getToBedIdA());
        assertEquals("B3", move.getFromBedIdB());
        assertEquals("B1", move.getToBedIdB());
    }
}
