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



// a tiny tracing utility
//
public final class AlgorithmTrace {

    private static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("opticare.algorithm.logs", "true")// enables the algorithm logs
            // default is true
    );
    private static final boolean CONSOLE = Boolean.parseBoolean(
            System.getProperty("opticare.algorithm.log.console", "true")// enables the algorithm logs to be printed to the console
    );
    private static final boolean FILE_ENABLED = Boolean.parseBoolean(
            System.getProperty("opticare.algorithm.log.file.enabled", "true")// enables the algorithm logs to be written to a file
    );
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS"); // time format
    private static volatile boolean fileInitFailed; // if the file initialization failed
    private static Writer fileWriter; // the file writer
    private static Path fileWriterPath; // the file writer path

    private AlgorithmTrace() {
    }

    private static Path logFilePath() {// get the log file path
        String override = System.getProperty("opticare.algorithm.log.file.path");
        if (override != null && !override.isBlank()) {// if the override is not null and not blank, use the override
            return Path.of(override).toAbsolutePath();
        }
        return PersistencePaths.defaultAlgorithmLogPath();// get the default log file path
    }

    public static void log(String layer, String message) {// log the message
        if (!ENABLED) {
            return; // if the logs are not enabled, return
        }
        String safeLayer = layer == null ? "unknown" : layer;
        String safeMessage = message == null ? "" : message;
        String line = "[" + LocalTime.now().format(TIME_FMT) + "] [Algorithm][" + safeLayer + "] " + safeMessage;
        if (CONSOLE) {// if the logs are enabled to be printed to the console, print the line
            System.out.println(line);
        }
        if (FILE_ENABLED && !fileInitFailed) {
            appendLineToFile(line);// if the logs are enabled to be written to a file, append the line to the file
        }
    }

    private static synchronized void appendLineToFile(String line) {
        Path path = logFilePath();
        try {// try to append the line to the file
            if (fileWriter != null && !path.equals(fileWriterPath)) {
                closeQuietly(fileWriter); // close the file writer
                fileWriter = null;
                fileWriterPath = null;
            }
            if (fileWriter == null) {
                Path parent = path.getParent();// get the parent directory
                if (parent != null) {
                    Files.createDirectories(parent); // if the parent directory does not exist, create it
                }
                fileWriter = new BufferedWriter(new OutputStreamWriter(// create a new file writer
                        Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                        StandardCharsets.UTF_8));
                fileWriterPath = path; // set the file writer path
            }
            fileWriter.write(line); // write the line to the file
            fileWriter.write(System.lineSeparator());
            fileWriter.flush();
        } catch (IOException e) {// if the file writer fails, close the file writer
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
        try {// try to close the writer
            w.close();
        } catch (IOException ignored) {
            // ignore
        }
    }
}

