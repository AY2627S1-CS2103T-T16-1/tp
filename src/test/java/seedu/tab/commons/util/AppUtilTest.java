package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static seedu.tab.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AppUtilTest {

    @TempDir
    public Path testFolder;

    @Test
    public void getApplicationDirectory_jarCodeLocation_returnsJarParent() throws Exception {
        Path jarFile = testFolder.resolve("folder with spaces").resolve("tab.jar");
        Files.createDirectories(jarFile.getParent());
        Files.createFile(jarFile);

        Path applicationDirectory = AppUtil.getApplicationDirectory(
                jarFile.toUri().toURL(), testFolder.resolve("working-directory"));

        assertEquals(jarFile.getParent(), applicationDirectory);
    }

    @Test
    public void getApplicationDirectory_explodedCodeLocation_returnsWorkingDirectory() throws Exception {
        Path workingDirectory = testFolder.resolve("working-directory");

        Path applicationDirectory = AppUtil.getApplicationDirectory(
                testFolder.toUri().toURL(), workingDirectory);

        assertEquals(workingDirectory, applicationDirectory);
    }

    @Test
    public void getImage_exitingImage() {
        assertNotNull(AppUtil.getImage("/images/address_book_32.png"));
    }

    @Test
    public void getImage_nullGiven_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> AppUtil.getImage(null));
    }

    @Test
    public void checkArgument_true_nothingHappens() {
        AppUtil.checkArgument(true);
        AppUtil.checkArgument(true, "");
    }

    @Test
    public void checkArgument_falseWithoutErrorMessage_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> AppUtil.checkArgument(false));
    }

    @Test
    public void checkArgument_falseWithErrorMessage_throwsIllegalArgumentException() {
        String errorMessage = "error message";
        assertThrows(IllegalArgumentException.class, errorMessage, () -> AppUtil.checkArgument(false, errorMessage));
    }
}
