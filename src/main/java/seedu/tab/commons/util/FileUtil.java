package seedu.tab.commons.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;

/**
 * Writes and reads files
 */
public class FileUtil {

    private static final String CHARSET = "UTF-8";

    private FileUtil() {} // this class only holds helpers

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
     * Reads an existing UTF-8 file, ignoring an optional leading byte order mark.
     *
     * @throws IOException if the file cannot be read or contains malformed UTF-8.
     */
    public static String readFromFile(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    /**
     * Writes the given string to a temporary file, then atomically replaces the target file.
     * Creates missing parent directories and creates the target only after a successful write.
     * Preserves an existing file if writing the new contents fails.
     * Follows a symbolic link to its target, creating that target when the folder holding it
     * exists, and rejects a link pointing into a folder that is not there.
     */
    public static void writeToFile(Path file, String content) throws IOException {
        writeToFile(file, content, FileUtil::writeAndSync);
    }

    static void writeToFile(Path file, String content, TemporaryFileWriter writer) throws IOException {
        Path targetFile = resolveWriteTarget(file);
        createParentDirsOfFile(targetFile);
        ensureWritable(targetFile);
        Path temporaryFile = createTemporaryFile(targetFile);

        try {
            copyPosixPermissions(targetFile, temporaryFile);
            writer.write(temporaryFile, content.getBytes(CHARSET));
            Files.move(temporaryFile, targetFile,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            deleteTemporaryFile(temporaryFile, exception);
            throw exception;
        }
    }

    private static Path createTemporaryFile(Path targetFile) throws IOException {
        try {
            return Files.createTempFile(targetFile.getParent(), ".tab-", ".tmp");
        } catch (AccessDeniedException exception) {
            AccessDeniedException targetException = new AccessDeniedException(targetFile.toString());
            targetException.initCause(exception);
            throw targetException;
        }
    }

    private static Path resolveWriteTarget(Path file) throws IOException {
        Path absoluteFile = file.toAbsolutePath();
        if (!Files.exists(absoluteFile, LinkOption.NOFOLLOW_LINKS)) {
            return absoluteFile;
        }
        return Files.exists(absoluteFile) ? absoluteFile.toRealPath() : resolveMissingLinkTarget(absoluteFile);
    }

    /**
     * Returns the file a symbolic link points at, which a save creates when it is not there yet,
     * so a link set up before its file still receives the first save.
     *
     * @throws NoSuchFileException if the link points into a folder that is not there, which no
     *     save can create without guessing where the records were meant to live.
     */
    private static Path resolveMissingLinkTarget(Path link) throws IOException {
        if (!Files.isSymbolicLink(link)) {
            return link;
        }
        Path target = link.resolveSibling(Files.readSymbolicLink(link)).normalize();
        Path targetParent = target.getParent();
        if (targetParent != null && Files.notExists(targetParent)) {
            throw new NoSuchFileException(link.toString(), null,
                    "it is a link to " + target + ", and that folder does not exist");
        }
        return target;
    }

    /**
     * Throws if {@code file} is a symbolic link whose target cannot be created, so a caller that
     * reads a missing file as a first run does not do so for records it cannot reach.
     *
     * @throws IOException if the link points into a folder that is not there.
     */
    public static void requireResolvableLink(Path file) throws IOException {
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS) && Files.notExists(file)) {
            resolveMissingLinkTarget(file.toAbsolutePath());
        }
    }

    private static void ensureWritable(Path file) throws AccessDeniedException {
        if (Files.exists(file) && !Files.isWritable(file)) {
            throw new AccessDeniedException(file.toString());
        }
    }

    private static void copyPosixPermissions(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        PosixFileAttributeView sourceAttributes = Files.getFileAttributeView(source, PosixFileAttributeView.class);
        if (sourceAttributes != null) {
            Files.setPosixFilePermissions(target, sourceAttributes.readAttributes().permissions());
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
