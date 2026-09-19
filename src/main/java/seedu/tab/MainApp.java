package seedu.tab;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;
import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.Logic;
import seedu.tab.logic.LogicManager;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.ReadOnlyUserPrefs;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.util.SampleDataUtil;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.Storage;
import seedu.tab.storage.StorageManager;
import seedu.tab.ui.Ui;
import seedu.tab.ui.UiManager;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final String VERSION = "V0.5.1";

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);
    private static final Path USER_PREFS_FILE_PATH = Paths.get("preferences.json");
    private static final Path STUDENT_BOOK_FILE_PATH = Paths.get("data", "tab.json");

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;

    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing StudentBook ]===========================");
        super.init();

        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(USER_PREFS_FILE_PATH);
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        JsonStudentBookStorage studentBookStorage = new JsonStudentBookStorage(STUDENT_BOOK_FILE_PATH);
        storage = new StorageManager(studentBookStorage, userPrefsStorage);

        model = initModelManager(storage, userPrefs);

        logic = new LogicManager(model, storage);

        ui = new UiManager(logic, storage.getStudentBookFilePath());
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s student book and {@code userPrefs}. <br>
     * The data from the sample student book will be used instead if {@code storage}'s student book is not found,
     * or an empty student book will be used instead if errors occur when reading {@code storage}'s student book.
     */
    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
        logger.info("Using data file : " + storage.getStudentBookFilePath());

        Optional<ReadOnlyStudentBook> studentBookOptional;
        ReadOnlyStudentBook initialData;
        try {
            studentBookOptional = storage.readStudentBook();
            if (studentBookOptional.isEmpty()) {
                logger.info("Creating a new data file " + storage.getStudentBookFilePath()
                        + " populated with a sample StudentBook.");
            }
            initialData = studentBookOptional.orElseGet(SampleDataUtil::getSampleStudentBook);
        } catch (DataLoadingException e) {
            logger.warning("Data file at " + storage.getStudentBookFilePath() + " could not be loaded."
                    + " Will be starting with an empty StudentBook.");
            initialData = new StudentBook();
        }

        return new ModelManager(initialData, userPrefs);
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(JsonUserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using preference file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            if (prefsOptional.isEmpty()) {
                logger.info("Creating new preference file " + prefsFilePath);
            }
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataLoadingException e) {
            logger.warning("Preference file at " + prefsFilePath + " could not be loaded."
                    + " Using default preferences.");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save preference file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting StudentBook " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping StudentBook ] =============================");
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
    }
}
