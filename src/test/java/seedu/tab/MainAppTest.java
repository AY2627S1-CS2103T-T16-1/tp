package seedu.tab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.commons.core.GuiSettings;
import seedu.tab.model.Model;
import seedu.tab.model.ReadOnlyUserPrefs;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.util.SampleDataUtil;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.Storage;
import seedu.tab.storage.StorageManager;
import seedu.tab.ui.UiManager;

public class MainAppTest {

    @TempDir
    public Path testFolder;

    private MainApp mainApp;

    @BeforeEach
    public void setUp() {
        mainApp = new MainApp();
    }

    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) throws Exception {
        Method method = MainApp.class.getDeclaredMethod("initModelManager", Storage.class, ReadOnlyUserPrefs.class);
        method.setAccessible(true);
        return (Model) method.invoke(mainApp, storage, userPrefs);
    }

    private Optional<String> getUiManagerLoadingError(MainApp initializedApp) throws Exception {
        var field = UiManager.class.getDeclaredField("studentBookLoadingError");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Optional<String> loadingError = (Optional<String>) field.get(initializedApp.ui);
        return loadingError;
    }

    @Test
    public void init_invalidStudentFile_passesLoadingErrorToUiManager() throws Exception {
        Path invalidFilePath = testFolder.resolve("invalidStudentBook.json");
        Files.writeString(invalidFilePath, "Invalid JSON content {{{");
        MainApp initializedApp = new MainApp(testFolder.resolve("prefs.json"), invalidFilePath);

        initializedApp.init();

        assertTrue(initializedApp.ui instanceof UiManager);
        assertEquals(initializedApp.getStudentBookLoadingError(), getUiManagerLoadingError(initializedApp));
    }

    @Test
    public void initModelManager_missingFile_initializesWithSampleData() throws Exception {
        Path missingFilePath = testFolder.resolve("missingStudentBook.json");
        StorageManager storage = new StorageManager(
                new JsonStudentBookStorage(missingFilePath),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));

        Model model = initModelManager(storage, new UserPrefs());
        assertEquals(SampleDataUtil.getSampleStudentBook(), model.getStudentBook());
        assertTrue(mainApp.getStudentBookLoadingError().isEmpty());
    }

    @Test
    public void initModelManager_corruptedFile_recordsErrorAndPreservesFile() throws Exception {
        Path corruptedFilePath = testFolder.resolve("corruptedStudentBook.json");
        String originalContents = "Invalid JSON content {{{";
        Files.writeString(corruptedFilePath, originalContents);
        StorageManager storage = new StorageManager(
                new JsonStudentBookStorage(corruptedFilePath),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));

        Model model = initModelManager(storage, new UserPrefs());
        assertTrue(model.getStudentBook().getStudentList().isEmpty());
        assertTrue(mainApp.getStudentBookLoadingError().orElseThrow()
                .contains("The data is not in the expected JSON format"));
        assertEquals(originalContents, Files.readString(corruptedFilePath));
    }

    @Test
    public void initModelManager_invalidStudent_recordsSpecificErrorAndPreservesFile() throws Exception {
        Path invalidFilePath = testFolder.resolve("invalidStudentBook.json");
        String originalContents = """
                {
                  "students": [ {
                    "name": "Ravi",
                    "phone": "12"
                  } ]
                }
                """;
        Files.writeString(invalidFilePath, originalContents);
        StorageManager storage = new StorageManager(
                new JsonStudentBookStorage(invalidFilePath),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));

        Model model = initModelManager(storage, new UserPrefs());

        assertTrue(model.getStudentBook().getStudentList().isEmpty());
        assertEquals("Student record 1 (\"Ravi\"): Phone \"12\" is invalid: "
                + "a phone number needs at least 3 digits", mainApp.getStudentBookLoadingError().orElseThrow());
        assertEquals(originalContents, Files.readString(invalidFilePath));
    }

    @Test
    public void initModelManager_validFile_initializesWithSavedData() throws Exception {
        Path validFilePath = testFolder.resolve("validStudentBook.json");
        JsonStudentBookStorage studentBookStorage = new JsonStudentBookStorage(validFilePath);
        studentBookStorage.saveStudentBook(getTypicalStudentBook());

        StorageManager storage = new StorageManager(
                studentBookStorage,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));

        Model model = initModelManager(storage, new UserPrefs());
        assertEquals(getTypicalStudentBook(), model.getStudentBook());
        assertFalse(mainApp.getStudentBookLoadingError().isPresent());
    }

    @Test
    public void initPrefs_missingFile_initializesWithDefaultPrefs() {
        Path missingPrefsPath = testFolder.resolve("missingPrefs.json");
        JsonUserPrefsStorage prefsStorage = new JsonUserPrefsStorage(missingPrefsPath);

        UserPrefs initializedPrefs = mainApp.initPrefs(prefsStorage);
        assertEquals(new UserPrefs(), initializedPrefs);
    }

    @Test
    public void initPrefs_corruptedFile_initializesWithDefaultPrefs() throws Exception {
        Path corruptedPrefsPath = testFolder.resolve("corruptedPrefs.json");
        Files.writeString(corruptedPrefsPath, "Not a json file");
        JsonUserPrefsStorage prefsStorage = new JsonUserPrefsStorage(corruptedPrefsPath);

        UserPrefs initializedPrefs = mainApp.initPrefs(prefsStorage);
        assertEquals(new UserPrefs(), initializedPrefs);
    }

    @Test
    public void initPrefs_validFile_initializesWithSavedPrefs() throws Exception {
        Path validPrefsPath = testFolder.resolve("validPrefs.json");
        JsonUserPrefsStorage prefsStorage = new JsonUserPrefsStorage(validPrefsPath);
        UserPrefs savedPrefs = new UserPrefs();
        savedPrefs.setGuiSettings(new GuiSettings(1366, 768, 10, 20));
        prefsStorage.saveUserPrefs(savedPrefs);

        UserPrefs initializedPrefs = mainApp.initPrefs(prefsStorage);
        assertEquals(savedPrefs, initializedPrefs);
    }
}
