package seedu.tab.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LogsCenterTest {

    private static final int PROCESS_TIMEOUT_SECONDS = 30;

    @TempDir
    public Path testFolder;

    @Test
    public void initialize_writableLogFile_logsToConsoleAndFile() throws Exception {
        String output = runLoggingProbe();

        assertTrue(output.contains("Logging probe reached main."), output);
        assertTrue(Files.readString(testFolder.resolve("tab.log.0")).contains("Logging probe reached main."));
        assertFalse(output.contains("Continuing with console logging only."), output);
    }

    @Test
    public void initialize_unwritableLogFile_warnsAndContinuesLoggingToConsole() throws Exception {
        // A directory at the log file path makes FileHandler fail on every supported OS,
        // including when the tests run with permission to write to read-only folders.
        Path blockedLog = Files.createDirectory(testFolder.resolve("tab.log.0"));

        String output = runLoggingProbe();

        assertTrue(output.contains("Unable to write log file tab.log."), output);
        assertTrue(output.contains("Continuing with console logging only."), output);
        assertTrue(output.contains("Logging probe reached main."), output);
        assertTrue(Files.isDirectory(blockedLog));
    }

    private String runLoggingProbe() throws Exception {
        Path consoleOutput = testFolder.resolve("console.txt");
        String classPath = Path.of(LogsCenterTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator
                + Path.of(LogsCenter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        // A fresh JVM prevents another test's successful initialization from hiding this failure.
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classPath, LoggingProbe.class.getName())
                .directory(testFolder.toFile())
                .redirectErrorStream(true)
                .redirectOutput(consoleOutput.toFile())
                .start();
        try {
            assertTrue(process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Logger initialization timed out");
            String output = Files.readString(consoleOutput);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            process.destroyForcibly();
        }
    }

    /**
     * Initializes logging in a fresh JVM to exercise failures during class initialization.
     */
    public static class LoggingProbe {
        public static void main(String[] args) {
            LogsCenter.getLogger(LoggingProbe.class).info("Logging probe reached main.");
        }
    }
}
