package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
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
    public void writeToFile_symbolicLink_updatesTargetAndPreservesLink() throws Exception {
        Path target = testFolder.resolve("real-data.json");
        Path link = testFolder.resolve("tab.json");
        Files.writeString(target, "old contents");
        createSymbolicLinkOrSkip(link, target);

        FileUtil.writeToFile(link, "new contents");

        assertTrue(Files.isSymbolicLink(link));
        assertEquals("new contents", Files.readString(target));
        assertEquals("new contents", Files.readString(link));
    }

    @Test
    public void writeToFile_existingPosixPermissions_preservesPermissions() throws Exception {
        Path file = testFolder.resolve("tab.json");
        Files.writeString(file, "old contents");
        assumePosixPermissions(file);
        Set<PosixFilePermission> permissions = Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ);
        Files.setPosixFilePermissions(file, permissions);

        FileUtil.writeToFile(file, "new contents");

        assertEquals(permissions, Files.getPosixFilePermissions(file));
        assertEquals("new contents", Files.readString(file));
    }

    @Test
    public void writeToFile_existingNonPosixFile_replacesContents() throws Exception {
        Path archive = testFolder.resolve("data.zip");
        URI archiveUri = URI.create("jar:" + archive.toUri());
        try (FileSystem fileSystem = FileSystems.newFileSystem(archiveUri, Map.of("create", "true"))) {
            Path file = fileSystem.getPath("tab.json");
            Files.writeString(file, "old contents");
            assertNull(Files.getFileAttributeView(file, PosixFileAttributeView.class));

            FileUtil.writeToFile(file, "new contents");

            assertEquals("new contents", Files.readString(file));
        }
    }

    @Test
    public void writeToFile_readOnlyPosixFile_throwsAndPreservesOriginal() throws Exception {
        Path file = testFolder.resolve("tab.json");
        String originalContents = "last valid student data";
        Files.writeString(file, originalContents);
        assumePosixPermissions(file);
        Set<PosixFilePermission> permissions = Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ);
        Files.setPosixFilePermissions(file, permissions);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> FileUtil.writeToFile(
                file, "replacement student data"));

        assertEquals(file.getFileName(), Path.of(exception.getFile()).getFileName());
        assertEquals(originalContents, Files.readString(file));
        assertEquals(permissions, Files.getPosixFilePermissions(file));
        assertEquals(List.of(file), listFilesInTestFolder());
    }

    @Test
    public void writeToFile_readOnlyParentDirectory_reportsTargetFile() throws Exception {
        Path directory = Files.createDirectory(testFolder.resolve("data"));
        Path file = directory.resolve("tab.json");
        Files.writeString(file, "old contents");
        assumePosixPermissions(directory);
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(directory);
        Files.setPosixFilePermissions(directory, Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));

        try {
            Assumptions.assumeFalse(Files.isWritable(directory), "The test account can still write to the directory.");
            AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> FileUtil.writeToFile(
                    file, "new contents"));

            assertEquals(file.getFileName(), Path.of(exception.getFile()).getFileName());
            assertInstanceOf(AccessDeniedException.class, exception.getCause());
        } finally {
            Files.setPosixFilePermissions(directory, originalPermissions);
        }
        assertEquals("old contents", Files.readString(file));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(file), files.toList());
        }
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

    @Test
    public void writeToFile_writeAndCleanupFail_reportsBothErrorsAndPreservesOriginal() throws Exception {
        Path file = testFolder.resolve("tab.json");
        String originalContents = "last valid student data";
        Files.writeString(file, originalContents);

        IOException exception = assertThrows(IOException.class, () -> FileUtil.writeToFile(
                file, "replacement student data", (temporaryFile, ignoredContent) -> {
                    Files.delete(temporaryFile);
                    Files.createDirectory(temporaryFile);
                    Files.writeString(temporaryFile.resolve("undeletable-child"), "partial");
                    throw new IOException("No space left on device");
                }));

        assertEquals("No space left on device", exception.getMessage());
        assertEquals(1, exception.getSuppressed().length);
        assertInstanceOf(DirectoryNotEmptyException.class, exception.getSuppressed()[0]);
        assertEquals(originalContents, Files.readString(file));
    }

    private List<Path> listFilesInTestFolder() throws IOException {
        try (var files = Files.list(testFolder)) {
            return files.toList();
        }
    }

    private void createSymbolicLinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | FileSystemException exception) {
            Assumptions.assumeTrue(false, "Symbolic links are unavailable: " + exception.getMessage());
        } catch (IOException exception) {
            throw new AssertionError("The symbolic link should be created.", exception);
        }
    }

    private void assumePosixPermissions(Path file) throws IOException {
        Assumptions.assumeTrue(Files.getFileStore(file).supportsFileAttributeView("posix"),
                "POSIX permissions are unavailable.");
    }
}
