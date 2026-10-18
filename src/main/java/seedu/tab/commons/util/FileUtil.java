package seedu.tab.commons.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Writes and reads files
 */
public class FileUtil {

    private static final String CHARSET = "UTF-8";

    /**
     * Creates a file if it does not exist along with its missing parent directories.
     * @throws IOException if the file or directory cannot be created.
     */
    public static void createIfMissing(Path file) throws IOException {
        if (Files.exists(file)) {
            return;
        }

        createParentDirsOfFile(file);

        Files.createFile(file);
    }

    /**
     * Creates parent directories of file if it has a parent directory
     */
    private static void createParentDirsOfFile(Path file) throws IOException {
        Path parentDir = file.getParent();

        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
    }

    /**
     * Assumes file exists
     */
    public static String readFromFile(Path file) throws IOException {
        return new String(Files.readAllBytes(file), CHARSET);
    }

    /**
     * Writes the given string to a temporary file, then atomically replaces the target file.
     * Will create the file if it does not exist yet.
     * Preserves an existing file if writing the new contents fails.
     */
    public static void writeToFile(Path file, String content) throws IOException {
        writeToFile(file, content, FileUtil::writeAndSync);
    }

    static void writeToFile(Path file, String content, TemporaryFileWriter writer) throws IOException {
        Path absoluteFile = file.toAbsolutePath();
        Path temporaryFile = Files.createTempFile(absoluteFile.getParent(), ".tab-", ".tmp");

        try {
            writer.write(temporaryFile, content.getBytes(CHARSET));
            Files.move(temporaryFile, absoluteFile,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            deleteTemporaryFile(temporaryFile, exception);
            throw exception;
        }
    }

    private static void writeAndSync(Path file, byte[] content) throws IOException {
        Files.write(file, content);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private static void deleteTemporaryFile(Path temporaryFile, IOException originalException) {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    @FunctionalInterface
    interface TemporaryFileWriter {
        void write(Path file, byte[] content) throws IOException;
    }

}
