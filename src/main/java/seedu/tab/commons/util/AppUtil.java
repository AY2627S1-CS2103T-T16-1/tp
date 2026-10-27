package seedu.tab.commons.util;

import static java.util.Objects.requireNonNull;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.scene.image.Image;
import seedu.tab.MainApp;

/**
 * A container for app-specific utility functions
 */
public class AppUtil {

    private AppUtil() {} // this class only holds helpers

    /**
     * Returns the directory that application files should be stored in.
     * When the application is packaged, this is the directory containing the JAR file.
     * Otherwise, this is the working directory used by the development environment.
     */
    public static Path getApplicationDirectory() {
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        URL codeLocation = AppUtil.class.getProtectionDomain().getCodeSource().getLocation();
        return getApplicationDirectory(codeLocation, workingDirectory);
    }

    static Path getApplicationDirectory(URL codeLocation, Path workingDirectory) {
        Path normalizedWorkingDirectory = workingDirectory.toAbsolutePath().normalize();
        try {
            Path codePath = Paths.get(codeLocation.toURI()).toAbsolutePath().normalize();
            return Files.isRegularFile(codePath) ? codePath.getParent() : normalizedWorkingDirectory;
        } catch (URISyntaxException | FileSystemNotFoundException
                | IllegalArgumentException | SecurityException e) {
            return normalizedWorkingDirectory;
        }
    }

    /**
     * Gets an {@code Image} from the specified path.
     */
    public static Image getImage(String imagePath) {
        requireNonNull(imagePath);
        return new Image(MainApp.class.getResourceAsStream(imagePath));
    }

    /**
     * Checks that {@code isConditionSatisfied} is true. Used for validating arguments to methods.
     *
     * @throws IllegalArgumentException if {@code isConditionSatisfied} is false.
     */
    public static void checkArgument(boolean isConditionSatisfied) {
        if (!isConditionSatisfied) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Checks that {@code isConditionSatisfied} is true. Used for validating arguments to methods.
     *
     * @throws IllegalArgumentException with {@code errorMessage} if {@code isConditionSatisfied} is false.
     */
    public static void checkArgument(boolean isConditionSatisfied, String errorMessage) {
        if (!isConditionSatisfied) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
