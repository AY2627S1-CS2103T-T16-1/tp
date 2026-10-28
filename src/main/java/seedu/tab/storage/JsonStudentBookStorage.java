package seedu.tab.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonMappingException;

import seedu.tab.commons.core.DataLoadingErrorMessage;
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
    private static final String MESSAGE_FOLDER_BLOCKED = "Cannot create data folder %s"
            + " because a file already occupies that path. Close TAB and move or rename that file before restarting";
    private static final String MESSAGE_LOCK_IS_FOLDER = "Cannot open save lock %s because it is a folder."
            + " Close all TAB windows and move or rename that folder before retrying";
    private static final String MESSAGE_LOCK_UNWRITABLE = "Cannot open save lock %s: %s."
            + " Close all TAB windows and check that this path is a writable file before retrying";
    private static final String MESSAGE_CANNOT_VERIFY = "Cannot verify saved student data in %s."
            + " Close this window, check the file and restart TAB before retrying";
    private static final String MESSAGE_STALE_DATA = "Student data in %s has changed since this window loaded it. "
            + "Close this window and restart TAB to load the saved records before retrying";
    private static final String MESSAGE_SAVE_BUSY = "Another TAB window is saving %s."
            + " Wait for it to finish and retry; if the data changed, restart this window first";
    private static final String MESSAGE_STUDENT_CONTEXT = "Student record %d: %s";

    private static final String SAVE_LOCK_SUFFIX = ".lock";

    private static final Logger logger = LogsCenter.getLogger(JsonStudentBookStorage.class);

    private final Map<Path, Optional<StudentBook>> loadedBooks = new HashMap<>();
    private final Path filePath;

    /**
     * Creates storage for the student book at the given path.
     */
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
     * Reads the student book from the given path.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyStudentBook> readStudentBook(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);

        Optional<LoadedBook> loaded = loadStudentBook(filePath);
        Optional<ReadOnlyStudentBook> book = loaded.map(LoadedBook::book);
        loadedBooks.put(filePath.toAbsolutePath().normalize(), book.map(StudentBook::new));
        return book;
    }

    /**
     * Loads and validates all records without changing the remembered stale-save baseline.
     */
    private Optional<LoadedBook> loadStudentBook(Path filePath) throws DataLoadingException {
        Optional<JsonSerializableStudentBook> jsonStudentBook = readJsonStudentBook(filePath);
        if (!jsonStudentBook.isPresent()) {
            return Optional.empty();
        }

        JsonSerializableStudentBook jsonBook = jsonStudentBook.get();
        try {
            return Optional.of(new LoadedBook(jsonBook.toModelType(), jsonBook.getComment()));
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataLoadingException(ive.getMessage(), ive);
        }
    }

    /**
     * Saves the given {@link ReadOnlyStudentBook} to the storage.
     *
     * @param studentBook cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveStudentBook(ReadOnlyStudentBook studentBook) throws IOException {
        saveStudentBook(studentBook, filePath);
    }

    /**
     * Saves the student book to the given path.
     *
     * @param studentBook The student book to save. Cannot be null.
     * @param filePath Location of the data. Cannot be null.
     */
    public void saveStudentBook(ReadOnlyStudentBook studentBook, Path filePath) throws IOException {
        requireNonNull(studentBook);
        requireNonNull(filePath);

        Path absolutePath = filePath.toAbsolutePath().normalize();
        createDataDirectory(absolutePath.getParent());
        Path target = Files.exists(absolutePath) ? absolutePath.toRealPath()
                : absolutePath.getParent().toRealPath().resolve(absolutePath.getFileName());
        Path lockPath = target.resolveSibling("." + target.getFileName() + SAVE_LOCK_SUFFIX);
        try (FileChannel channel = openSaveLock(lockPath);
                FileLock lock = channel.tryLock()) {
            if (lock == null) {
                throw new IOException(formatBusyMessage(filePath));
            }
            saveIfUnchanged(studentBook, filePath, absolutePath);
        } catch (OverlappingFileLockException e) {
            throw new IOException(formatBusyMessage(filePath), e);
        }
    }

    /**
     * Creates missing data directories and explains a file blocking the intended directory.
     */
    private static void createDataDirectory(Path directory) throws IOException {
        try {
            Files.createDirectories(directory);
        } catch (FileAlreadyExistsException e) {
            throw new IOException(String.format(MESSAGE_FOLDER_BLOCKED, e.getFile()), e);
        }
    }

    /**
     * Opens the shared save lock and reports recovery guidance when its path is unusable.
     */
    private static FileChannel openSaveLock(Path lockPath) throws IOException {
        if (Files.isDirectory(lockPath)) {
            throw new IOException(String.format(MESSAGE_LOCK_IS_FOLDER, lockPath));
        }
        try {
            return FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (AccessDeniedException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException(String.format(MESSAGE_LOCK_UNWRITABLE, lockPath, e.getMessage()), e);
        }
    }

    /**
     * Rejects stale writes before replacing the file, then remembers the successfully saved book.
     *
     * <p>The description is taken from the file about to be replaced, so a description added or
     * edited by hand after this window started survives the save.
     */
    private void saveIfUnchanged(ReadOnlyStudentBook studentBook, Path filePath, Path key) throws IOException {
        Optional<LoadedBook> loadedDiskBook;
        try {
            loadedDiskBook = loadStudentBook(filePath);
        } catch (DataLoadingException e) {
            throw new IOException(String.format(MESSAGE_CANNOT_VERIFY, filePath), e);
        }
        Optional<StudentBook> diskBook = loadedDiskBook.map(LoadedBook::book);
        Optional<StudentBook> expectedBook = loadedBooks.getOrDefault(key, diskBook);
        if (!expectedBook.equals(diskBook)) {
            throw new IOException(String.format(MESSAGE_STALE_DATA, filePath));
        }
        writeStudentBook(studentBook, filePath, loadedDiskBook.map(LoadedBook::comment).orElse(null));
        loadedBooks.put(key, Optional.of(new StudentBook(studentBook)));
    }

    /**
     * Returns recovery guidance when another window holds the save lock.
     */
    private String formatBusyMessage(Path path) {
        return String.format(MESSAGE_SAVE_BUSY, path);
    }

    /**
     * Reads the JSON book, adding the offending student-record index when available.
     */
    private Optional<JsonSerializableStudentBook> readJsonStudentBook(Path filePath) throws DataLoadingException {
        try {
            return JsonUtil.readJsonFile(filePath, JsonSerializableStudentBook.class);
        } catch (DataLoadingException e) {
            throw addStudentRecordContext(e);
        }
    }

    /**
     * Adds the first affected array index when Jackson can identify a student record.
     */
    private static DataLoadingException addStudentRecordContext(DataLoadingException failure) {
        if (!(failure.getCause() instanceof JsonMappingException mappingException)) {
            return failure;
        }
        return mappingException.getPath().stream()
                .filter(reference -> reference.getIndex() >= 0)
                .findFirst()
                .map(reference -> addStudentRecordNumber(failure, reference.getIndex() + 1))
                .orElse(failure);
    }

    /**
     * Returns {@code failure} with {@code recordNumber} named in its explanation, leaving the
     * part of the file it quotes as it is.
     */
    private static DataLoadingException addStudentRecordNumber(DataLoadingException failure, int recordNumber) {
        DataLoadingErrorMessage errorMessage = failure.getErrorMessage();
        return new DataLoadingException(errorMessage.withDescription(String.format(Locale.ROOT,
                MESSAGE_STUDENT_CONTEXT, recordNumber, errorMessage.description())), failure);
    }

    /** Writes the book through a delegate that tests can override to simulate a write failure. */
    void writeStudentBook(ReadOnlyStudentBook studentBook, Path filePath, String comment) throws IOException {
        JsonUtil.saveJsonFile(new JsonSerializableStudentBook(studentBook, comment), filePath);
    }

    /** A student book read from a file, with the description that file carries. */
    private record LoadedBook(StudentBook book, String comment) {}

}
