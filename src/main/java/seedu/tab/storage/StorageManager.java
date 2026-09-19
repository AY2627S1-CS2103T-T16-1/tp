package seedu.tab.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.ReadOnlyUserPrefs;
import seedu.tab.model.UserPrefs;

/**
 * Manages storage of StudentBook data in local storage.
 */
public class StorageManager implements Storage {

    private static final Logger logger = LogsCenter.getLogger(StorageManager.class);
    private JsonStudentBookStorage studentBookStorage;
    private JsonUserPrefsStorage userPrefsStorage;

    /**
     * Creates a {@code StorageManager} with the given student book and user prefs storage.
     */
    public StorageManager(JsonStudentBookStorage studentBookStorage, JsonUserPrefsStorage userPrefsStorage) {
        this.studentBookStorage = studentBookStorage;
        this.userPrefsStorage = userPrefsStorage;
    }

    // ================ UserPrefs methods ==============================

    @Override
    public Path getUserPrefsFilePath() {
        return userPrefsStorage.getUserPrefsFilePath();
    }

    @Override
    public Optional<UserPrefs> readUserPrefs() throws DataLoadingException {
        return userPrefsStorage.readUserPrefs();
    }

    @Override
    public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException {
        userPrefsStorage.saveUserPrefs(userPrefs);
    }


    // ================ StudentBook methods ==============================

    @Override
    public Path getStudentBookFilePath() {
        return studentBookStorage.getStudentBookFilePath();
    }

    @Override
    public Optional<ReadOnlyStudentBook> readStudentBook() throws DataLoadingException {
        logger.fine("Attempting to read data from file: " + studentBookStorage.getStudentBookFilePath());
        return studentBookStorage.readStudentBook();
    }

    @Override
    public void saveStudentBook(ReadOnlyStudentBook studentBook) throws IOException {
        logger.fine("Attempting to write to data file: " + studentBookStorage.getStudentBookFilePath());
        studentBookStorage.saveStudentBook(studentBook);
    }

}
