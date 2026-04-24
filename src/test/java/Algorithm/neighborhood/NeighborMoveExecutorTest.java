package Algorithm.neighborhood;

import Algorithm.AssignmentState;
import Model.entety.Bed;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.PatientStatus;
import Model.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NeighborMoveExecutorTest {

    @Test
    void assign_undo_roundTrip() {
        Department dept = deptOneBed();
        Map<String, Patient> byId = index(dept);
        AssignmentState state = new AssignmentState();
        NeighborMoveExecutor ex = new NeighborMoveExecutor();
        NeighborMove m = NeighborMove.assign("P1", "B1");
        NeighborMoveExecutor.UndoToken u = ex.apply(m, state, dept, byId);
        assertEquals("B1", state.getBed("P1").getId());
        ex.undo(u, state, dept, byId);
        assertNull(state.getBed("P1"));
    }

    @Test
    void move_undo_roundTrip() {
        Department dept = deptTwoBeds();
        Map<String, Patient> byId = index(dept);
        AssignmentState state = new AssignmentState();
        state.assign(byId.get("P1"), dept.findBedById("B1"));
        NeighborMoveExecutor ex = new NeighborMoveExecutor();
        NeighborMove m = NeighborMove.move("P1", "B1", "B2");
        NeighborMoveExecutor.UndoToken u = ex.apply(m, state, dept, byId);
        assertEquals("B2", state.getBed("P1").getId());
        ex.undo(u, state, dept, byId);
        assertEquals("B1", state.getBed("P1").getId());
    }

    @Test
    void swap_undo_roundTrip() {
        Department dept = deptTwoBeds();
        Map<String, Patient> byId = index(dept);
        AssignmentState state = new AssignmentState();
        state.assign(byId.get("P1"), dept.findBedById("B1"));
        state.assign(byId.get("P2"), dept.findBedById("B2"));
        NeighborMoveExecutor ex = new NeighborMoveExecutor();
        NeighborMove m = NeighborMove.swap("P1", "P2", "B1", "B2");
        NeighborMoveExecutor.UndoToken u = ex.apply(m, state, dept, byId);
        assertEquals("B2", state.getBed("P1").getId());
        assertEquals("B1", state.getBed("P2").getId());
        ex.undo(u, state, dept, byId);
        assertEquals("B1", state.getBed("P1").getId());
        assertEquals("B2", state.getBed("P2").getId());
    }

    private static Department deptOneBed() {
        Room r = new Room("R1", "D1", 1, new ArrayList<>(), 0, false, false);
        r.addBed(new Bed("B1", "R1", BedType.REGULAR, false, false));
        Department d = new Department("D1", "x", new ArrayList<>(List.of(r)), new ArrayList<>());
        Patient p = new Patient("P1", null, new Model.entety.ClinicalData(RiskLevel.CLEAN, 0, false, null));
        p.setStatus(PatientStatus.WAITING);
        d.getWaitingList().add(p);
        return d;
    }

    private static Department deptTwoBeds() {
        Room r = new Room("R1", "D1", 2, new ArrayList<>(), 0, false, false);
        r.addBed(new Bed("B1", "R1", BedType.REGULAR, false, false));
        r.addBed(new Bed("B2", "R1", BedType.REGULAR, false, false));
        Department d = new Department("D1", "x", new ArrayList<>(List.of(r)), new ArrayList<>());
        Patient p1 = new Patient("P1", null, new Model.entety.ClinicalData(RiskLevel.CLEAN, 0, false, null));
        Patient p2 = new Patient("P2", null, new Model.entety.ClinicalData(RiskLevel.CLEAN, 0, false, null));
        p1.setStatus(PatientStatus.ASSIGNED);
        p2.setStatus(PatientStatus.ASSIGNED);
        d.getWaitingList().add(p1);
        d.getWaitingList().add(p2);
        return d;
    }

    private static Map<String, Patient> index(Department d) {
        Map<String, Patient> m = new LinkedHashMap<>();
        for (Patient p : d.getWaitingList()) {
            m.put(p.getId(), p);
        }
        return m;
    }
}
