package Persistence;

import java.nio.file.Path;
import java.nio.file.Paths;

// all file paths locations
public final class PersistencePaths {// utility class

    private PersistencePaths() {
    }

    // returns the default ward state json path
    public static Path defaultWardStateJsonPath() {
        return Paths.get(System.getProperty("user.home"), ".opticare", "ward-state.json").toAbsolutePath();
    }

    // returns the default users json path
    public static Path defaultUsersJsonPath() {
        return Paths.get(System.getProperty("user.home"), ".opticare", "users.json").toAbsolutePath();
    }

    // returns the default algorithm log path
    public static Path defaultAlgorithmLogPath() {
        return Paths.get(System.getProperty("user.home"), ".opticare", "algorithm.log").toAbsolutePath();
    }
}
