package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileUtilTest {

    @TempDir
    public Path testFolder;

    @Test
    public void writeToFile_existingFile_replacesContents() throws Exception {
        Path file = testFolder.resolve("tab.json");
        Files.writeString(file, "old contents");

        FileUtil.writeToFile(file, "new contents");

        assertEquals("new contents", Files.readString(file));
        assertEquals(List.of(file), listFilesInTestFolder());
    }

    @Test
    public void writeToFile_partialTemporaryWriteFails_preservesOriginalAndRemovesTemporaryFile() throws Exception {
        Path file = testFolder.resolve("tab.json");
        String originalContents = "last valid student data";
        Files.writeString(file, originalContents);
        byte[] originalBytes = Files.readAllBytes(file);

        IOException exception = assertThrows(IOException.class, () -> FileUtil.writeToFile(
                file, "replacement student data", (temporaryFile, ignoredContent) -> {
                    Files.write(temporaryFile, "partial".getBytes(StandardCharsets.UTF_8));
                    throw new IOException("No space left on device");
                }));

        assertEquals("No space left on device", exception.getMessage());
        assertArrayEquals(originalBytes, Files.readAllBytes(file));
        assertEquals(List.of(file), listFilesInTestFolder());
    }

    private List<Path> listFilesInTestFolder() throws IOException {
        try (var files = Files.list(testFolder)) {
            return files.toList();
        }
    }
}
