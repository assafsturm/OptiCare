package Algorithm.neighborhood;

import Model.entety.Bed;

import java.util.Objects;

/**
 * Two beds are equivalent for neighborhood symmetry filtering when they have the same
 * assignment-relevant signature in the same room (zero-information permutations).
 */
public final class BedEquivalence {

    private BedEquivalence() {
    }

    /** Same room, same type/ventilator, neither broken. */
    public static boolean areEquivalent(Bed a, Bed b) {
        if (a == null || b == null) return false;
        if (a.isBroken() || b.isBroken()) return false;
        return Objects.equals(a.getRoomId(), b.getRoomId())
                && a.getType() == b.getType()
                && a.isHasVentilator() == b.isHasVentilator();
    }
}
