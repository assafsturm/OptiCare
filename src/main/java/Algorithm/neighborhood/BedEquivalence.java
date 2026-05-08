package Algorithm.neighborhood;

import java.util.Objects;

import Model.entety.Bed;

// two beds are equivalent for neighborhood symmetry filtering
// when they have the same assignment relevant signature in the same room
public final class BedEquivalence {

    private BedEquivalence() {
    }

    // same room, same type/ventilator, neither broken
    public static boolean areEquivalent(Bed a, Bed b) {
        if (a == null || b == null) return false;
        if (a.isBroken() || b.isBroken()) return false;
        return Objects.equals(a.getRoomId(), b.getRoomId())
                && a.getType() == b.getType()
                && a.isHasVentilator() == b.isHasVentilator();
    }
}
