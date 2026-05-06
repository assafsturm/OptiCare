package Persistence;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Canonical storage locations for file-backed persistence (Stage 6). */
public final class PersistencePaths {

    private PersistencePaths() {
    }

    /** User-scoped ward snapshot (JSON). Same directory suits future multi-file repos. */
    public static Path defaultWardStateJsonPath() {
        return Paths.get(System.getProperty("user.home"), ".opticare", "ward-state.json").toAbsolutePath();
    }

    /** Plaintext demo users for Stage 7 minimal login */
    public static Path defaultUsersJsonPath() {
        return Paths.get(System.getProperty("user.home"), ".opticare", "users.json").toAbsolutePath();
    }

    /** Append-only algorithm trace log filename under {@code .opticare/}. */
    public static Path defaultAlgorithmLogPath() {
        return Paths.get(System.getProperty("user.home"), ".opticare", "algorithm.log").toAbsolutePath();
    }
}
