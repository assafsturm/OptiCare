package Algorithm;

import Persistence.PersistencePaths;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight tracing for algorithm runtime flow (console and optional append-only file).
 * Master switch: {@code -Dopticare.algorithm.logs=false}.
 * File: {@code %USERHOME%/.opticare/algorithm.log} unless overridden by
 * {@code -Dopticare.algorithm.log.file.path=...}; disable file with
 * {@code -Dopticare.algorithm.log.file.enabled=false}.
 * Console: {@code -Dopticare.algorithm.log.console=false} to suppress stdout only.
 */
public final class AlgorithmTrace {

    private static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("opticare.algorithm.logs", "true")
    );
    private static final boolean CONSOLE = Boolean.parseBoolean(
            System.getProperty("opticare.algorithm.log.console", "true")
    );
    private static final boolean FILE_ENABLED = Boolean.parseBoolean(
            System.getProperty("opticare.algorithm.log.file.enabled", "true")
    );
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static volatile boolean fileInitFailed;
    private static Writer fileWriter;
    private static Path fileWriterPath;

    private AlgorithmTrace() {
    }

    private static Path logFilePath() {
        String override = System.getProperty("opticare.algorithm.log.file.path");
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath();
        }
        return PersistencePaths.defaultAlgorithmLogPath();
    }

    public static void log(String layer, String message) {
        if (!ENABLED) {
            return;
        }
        String safeLayer = layer == null ? "unknown" : layer;
        String safeMessage = message == null ? "" : message;
        String line = "[" + LocalTime.now().format(TIME_FMT) + "] [Algorithm][" + safeLayer + "] " + safeMessage;
        if (CONSOLE) {
            System.out.println(line);
        }
        if (FILE_ENABLED && !fileInitFailed) {
            appendLineToFile(line);
        }
    }

    private static synchronized void appendLineToFile(String line) {
        Path path = logFilePath();
        try {
            if (fileWriter != null && !path.equals(fileWriterPath)) {
                closeQuietly(fileWriter);
                fileWriter = null;
                fileWriterPath = null;
            }
            if (fileWriter == null) {
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                fileWriter = new BufferedWriter(new OutputStreamWriter(
                        Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                        StandardCharsets.UTF_8));
                fileWriterPath = path;
            }
            fileWriter.write(line);
            fileWriter.write(System.lineSeparator());
            fileWriter.flush();
        } catch (IOException e) {
            closeQuietly(fileWriter);
            fileWriter = null;
            fileWriterPath = null;
            fileInitFailed = true;
            System.err.println("[AlgorithmTrace] Could not write log file " + path + ": " + e.getMessage());
        }
    }

    private static void closeQuietly(Writer w) {
        if (w == null) {
            return;
        }
        try {
            w.close();
        } catch (IOException ignored) {
            // ignore
        }
    }
}

