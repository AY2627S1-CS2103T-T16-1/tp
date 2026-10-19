package seedu.tab.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

        String warningStart = "Unable to write log file ";
        String warningEnd = ". Continuing with console logging only.";
        int pathStart = output.indexOf(warningStart) + warningStart.length();
        int pathEnd = output.indexOf(warningEnd, pathStart);
        assertTrue(pathStart >= warningStart.length() && pathEnd >= 0, output);
        Path reportedLogFile = Path.of(output.substring(pathStart, pathEnd));
        assertEquals("tab.log", reportedLogFile.getFileName().toString());
        assertTrue(Files.isSameFile(testFolder, reportedLogFile.getParent()), output);
        assertTrue(output.contains("Logging probe reached main."), output);
        assertTrue(Files.isDirectory(blockedLog));
    }

    private String runLoggingProbe() throws Exception {
        Path consoleOutput = testFolder.resolve("console.txt");
        // A fresh JVM prevents another test's successful initialization from hiding this failure.
        Process process = new ProcessBuilder(getLoggingProbeCommand())
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

    private static List<String> getLoggingProbeCommand() throws URISyntaxException {
        String classPath = Path.of(LogsCenterTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator
                + Path.of(LogsCenter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        // Keep subprocess coverage in the parent report despite the probe's different working directory.
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(argument -> argument.startsWith("-javaagent:") && argument.contains("jacoco"))
                .map(LogsCenterTest::resolveCoverageDestination)
                .forEach(command::add);
        command.addAll(List.of("-cp", classPath, LoggingProbe.class.getName()));
        return command;
    }

    private static String resolveCoverageDestination(String agentArgument) {
        String destinationOption = "destfile=";
        int optionStart = agentArgument.indexOf(destinationOption);
        if (optionStart < 0) {
            return agentArgument + (agentArgument.contains("=") ? "," : "=")
                    + destinationOption + Path.of("jacoco.exec").toAbsolutePath();
        }
        int pathStart = optionStart + destinationOption.length();
        int pathEnd = agentArgument.indexOf(',', pathStart);
        if (pathEnd < 0) {
            pathEnd = agentArgument.length();
        }
        Path destination = Path.of(agentArgument.substring(pathStart, pathEnd)).toAbsolutePath();
        return agentArgument.substring(0, pathStart) + destination + agentArgument.substring(pathEnd);
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
