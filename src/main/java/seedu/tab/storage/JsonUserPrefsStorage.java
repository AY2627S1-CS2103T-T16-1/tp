package seedu.tab.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.commons.exceptions.IllegalValueException;
import seedu.tab.commons.util.JsonUtil;
import seedu.tab.model.ReadOnlyUserPrefs;
import seedu.tab.model.UserPrefs;

/**
 * A class to access UserPrefs stored on the hard disk as a JSON file
 */
public class JsonUserPrefsStorage {

    private static final String MESSAGE_NULL_SETTINGS = "GUI settings must not be null.";

    private final Path filePath;

    /**
     * Creates storage for preferences at the given path.
     */
    public JsonUserPrefsStorage(Path filePath) {
        this.filePath = filePath;
    }

    public Path getUserPrefsFilePath() {
        return filePath;
    }

    /**
     * Returns UserPrefs data from storage.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if the loading of data from preference file failed.
     */
    public Optional<UserPrefs> readUserPrefs() throws DataLoadingException {
        return readUserPrefs(filePath);
    }

    /**
     * Reads preferences from the given path.
     *
     * @param prefsFilePath location of the data. Cannot be null.
     * @throws DataLoadingException if the file format is not as expected or GUI settings are null.
     */
    public Optional<UserPrefs> readUserPrefs(Path prefsFilePath) throws DataLoadingException {
        Optional<UserPrefs> userPrefs = JsonUtil.readJsonFile(prefsFilePath, UserPrefs.class);
        if (userPrefs.isPresent() && userPrefs.get().getGuiSettings() == null) {
            IllegalValueException cause = new IllegalValueException(MESSAGE_NULL_SETTINGS);
            throw new DataLoadingException(cause.getMessage(), cause);
        }
        return userPrefs;
    }

    /**
     * Saves the given {@link seedu.tab.model.ReadOnlyUserPrefs} to the storage.
     *
     * @param userPrefs cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException {
        JsonUtil.saveJsonFile(userPrefs, filePath);
    }

}
