package Algorithm.determinism;

import Algorithm.AssignmentState;
import Model.entety.Bed;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Canonical deterministic hash of an assignment.
 */
public final class AssignmentStateHasher {

    private AssignmentStateHasher() {
    }

    /**
     * Sorts by patientId, then hashes "patientId=bedId" lines using SHA-256.
     */
    public static String sha256(AssignmentState state) {
        List<String> lines = new ArrayList<>();
        if (state != null) {
            for (Map.Entry<String, Bed> e : state.getAssignments().entrySet()) {
                String patientId = e.getKey();
                String bedId = e.getValue() != null ? e.getValue().getId() : "null";
                lines.add((patientId == null ? "null" : patientId) + "=" + bedId);
            }
        }
        lines.sort(Comparator.naturalOrder());
        String canonical = String.join("\n", lines);
        return sha256OfString(canonical);
    }

    private static String sha256OfString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
