package seedu.tab.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonMappingException;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.commons.exceptions.IllegalValueException;
import seedu.tab.commons.util.JsonUtil;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;

/**
 * A class to access StudentBook data stored as a JSON file on the hard disk.
 */
public class JsonStudentBookStorage {

    private static final Logger logger = LogsCenter.getLogger(JsonStudentBookStorage.class);

    private final Map<Path, Optional<StudentBook>> loadedBooks = new HashMap<>();
    private Path filePath;

    public JsonStudentBookStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getStudentBookFilePath() {
        return filePath;
    }

    /**
     * Returns StudentBook data as a {@link ReadOnlyStudentBook}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyStudentBook> readStudentBook() throws DataLoadingException {
        return readStudentBook(filePath);
    }

    /**
     * Similar to {@link #readStudentBook()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyStudentBook> readStudentBook(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);

        Optional<ReadOnlyStudentBook> book = loadStudentBook(filePath);
        loadedBooks.put(filePath.toAbsolutePath().normalize(), book.map(StudentBook::new));
        return book;
    }

    private Optional<ReadOnlyStudentBook> loadStudentBook(Path filePath) throws DataLoadingException {
        Optional<JsonSerializableStudentBook> jsonStudentBook = readJsonStudentBook(filePath);
        if (!jsonStudentBook.isPresent()) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonStudentBook.get().toModelType());
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataLoadingException(ive.getMessage(), ive);
        }
    }

    /**
     * Saves the given {@link ReadOnlyStudentBook} to the storage.
     * @param studentBook cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveStudentBook(ReadOnlyStudentBook studentBook) throws IOException {
        saveStudentBook(studentBook, filePath);
    }

    /**
     * Similar to {@link #saveStudentBook(ReadOnlyStudentBook)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public void saveStudentBook(ReadOnlyStudentBook studentBook, Path filePath) throws IOException {
        requireNonNull(studentBook);
        requireNonNull(filePath);

        Path absolutePath = filePath.toAbsolutePath().normalize();
        Files.createDirectories(absolutePath.getParent());
        Path target = Files.exists(absolutePath) ? absolutePath.toRealPath()
                : absolutePath.getParent().toRealPath().resolve(absolutePath.getFileName());
        Path lockPath = target.resolveSibling("." + target.getFileName() + ".lock");
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = channel.tryLock()) {
            if (lock == null) {
                throw new IOException(formatBusyMessage(filePath));
            }
            saveIfUnchanged(studentBook, filePath, absolutePath);
        } catch (OverlappingFileLockException e) {
            throw new IOException(formatBusyMessage(filePath), e);
        }
    }

    private void saveIfUnchanged(ReadOnlyStudentBook studentBook, Path filePath, Path key) throws IOException {
        Optional<StudentBook> diskBook;
        try {
            diskBook = loadStudentBook(filePath).map(StudentBook::new);
        } catch (DataLoadingException e) {
            throw new IOException("Cannot verify saved student data in " + filePath
                    + ". Close this window, check the file and restart TAB before retrying", e);
        }
        Optional<StudentBook> expectedBook = loadedBooks.getOrDefault(key, diskBook);
        if (!expectedBook.equals(diskBook)) {
            throw new IOException("Student data in " + filePath + " has changed since this window loaded it. "
                    + "Close this window and restart TAB to load the saved records before retrying");
        }
        writeStudentBookFile(studentBook, filePath);
        loadedBooks.put(key, Optional.of(new StudentBook(studentBook)));
    }

    private String formatBusyMessage(Path path) {
        return "Another TAB window is saving " + path
                + ". Wait for it to finish and retry; if the data changed, restart this window first";
    }

    private Optional<JsonSerializableStudentBook> readJsonStudentBook(Path filePath) throws DataLoadingException {
        try {
            return JsonUtil.readJsonFile(filePath, JsonSerializableStudentBook.class);
        } catch (DataLoadingException e) {
            if (e.getCause() instanceof JsonMappingException mappingException) {
                Optional<JsonMappingException.Reference> studentRecord = mappingException.getPath().stream()
                        .filter(reference -> reference.getIndex() >= 0).findFirst();
                if (studentRecord.isPresent()) {
                    throw new DataLoadingException("Student record " + (studentRecord.get().getIndex() + 1)
                            + ": " + e.getMessage(), e);
                }
            }
            throw e;
        }
    }

    /**
     * Similar to {@link #saveStudentBook(ReadOnlyStudentBook)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    private void writeStudentBookFile(ReadOnlyStudentBook studentBook, Path filePath) throws IOException {
        requireNonNull(studentBook);
        requireNonNull(filePath);

        writeStudentBook(studentBook, filePath);
    }

    /** Writes the book through a delegate that tests can override to simulate a write failure. */
    void writeStudentBook(ReadOnlyStudentBook studentBook, Path filePath) throws IOException {
        JsonUtil.saveJsonFile(new JsonSerializableStudentBook(studentBook), filePath);
    }

}
