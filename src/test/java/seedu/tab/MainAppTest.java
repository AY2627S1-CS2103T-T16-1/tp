package seedu.tab;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.commons.core.DataLoadingErrorMessage;
import seedu.tab.commons.core.GuiSettings;
import seedu.tab.commons.core.LogsCenter;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.ReadOnlyUserPrefs;
import seedu.tab.model.StudentBook;
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

    private Optional<DataLoadingErrorMessage> getUiManagerLoadingError(MainApp initializedApp) throws Exception {
        var field = UiManager.class.getDeclaredField("studentBookLoadingError");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Optional<DataLoadingErrorMessage> loadingError =
                (Optional<DataLoadingErrorMessage>) field.get(initializedApp.ui);
        return loadingError;
    }

    @Test
    public void init_ambiguousJson_passesErrorToUiAndPreservesBytesOnShutdown() throws Exception {
        for (String json : new String[] {"{\"students\":[]}\n{}", "{\"students\":[],\"students\":[]}"}) {
            Path path = testFolder.resolve("ambiguous.json");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            Files.write(path, bytes);
            MainApp initializedApp = new MainApp(testFolder.resolve("prefs.json"), path);

            initializedApp.init();
            String error = getUiManagerLoadingError(initializedApp).orElseThrow().toDisplayText();
            assertTrue(error.contains(json.contains("\n") ? "Expected a single JSON value" : "Duplicate field"));
            assertTrue(initializedApp.model.getStudentBook().getStudentList().isEmpty());
            initializedApp.stop();
            assertArrayEquals(bytes, Files.readAllBytes(path));
        }
    }

    @Test
    public void init_numericFollowUpFlag_passesErrorToUiAndPreservesBytesOnShutdown() throws Exception {
        Path path = testFolder.resolve("numeric-flag.json");
        byte[] bytes = "{\"students\":[{\"name\":\"Alice Tan\",\"phone\":\"91234567\",\"flag\":2}]}"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes);
        MainApp initializedApp = new MainApp(testFolder.resolve("prefs.json"), path);

        initializedApp.init();

        String error = getUiManagerLoadingError(initializedApp).orElseThrow().toDisplayText();
        assertTrue(error.startsWith("Student record 1: "), error);
        assertTrue(error.contains("Invalid flag value 2. Use JSON true, false or null for the follow-up flag."));
        assertTrue(initializedApp.model.getStudentBook().getStudentList().isEmpty());
        initializedApp.stop();
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void init_invalidUtf8_passesEncodingErrorToUiAndPreservesBytesOnShutdown() throws Exception {
        Path path = testFolder.resolve("students.json");
        byte[] bytes = "{\"students\":[{\"name\":\"José Tan\",\"phone\":\"91234567\"}]}"
                .getBytes(Charset.forName("windows-1252"));
        Files.write(path, bytes);
        MainApp initializedApp = new MainApp(testFolder.resolve("prefs.json"), path);

        initializedApp.init();
        assertEquals("The file is not valid UTF-8. Reopen it with its original encoding and save it as UTF-8,"
                + " then restart TAB.",
                getUiManagerLoadingError(initializedApp).orElseThrow().description());
        assertTrue(initializedApp.model.getStudentBook().getStudentList().isEmpty());
        initializedApp.stop();
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void init_unknownStudentKey_passesErrorToUiAndPreservesBytesOnShutdown() throws Exception {
        Path path = testFolder.resolve("students.json");
        String json = """
                {"students":[{"name":"Bernice Yu","phone":"99272758","nusStudentID":"A0999999Z"}]}
                """;
        Files.writeString(path, json);
        MainApp initializedApp = new MainApp(testFolder.resolve("prefs.json"), path);

        initializedApp.init();
        String error = getUiManagerLoadingError(initializedApp).orElseThrow().description();
        assertTrue(error.contains("Student record 1: "));
        assertTrue(error.contains("Unknown field \"nusStudentID\" in student \"Bernice Yu\""));
        assertTrue(initializedApp.model.getStudentBook().getStudentList().isEmpty());
        initializedApp.stop();
        assertEquals(json, Files.readString(path));
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
        assertTrue(mainApp.getStudentBookLoadingError().orElseThrow().description()
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
        assertEquals("Student record 1 (\"Ravi\"): Phone \"12\" is not valid: "
                + "a phone number needs at least 3 digits",
                mainApp.getStudentBookLoadingError().orElseThrow().description());
        assertEquals(originalContents, Files.readString(invalidFilePath));
    }

    @Test
    public void init_nullStudentDocument_recordsErrorAndPreservesFile() throws Exception {
        Path studentsPath = testFolder.resolve("students.json");
        Files.writeString(studentsPath, "null");
        MainApp initializedApp = new MainApp(testFolder.resolve("prefs.json"), studentsPath);

        initializedApp.init();

        assertEquals("The JSON document must not be null.",
                getUiManagerLoadingError(initializedApp).orElseThrow().description());
        assertTrue(initializedApp.model.getStudentBook().getStudentList().isEmpty());
        assertEquals("null", Files.readString(studentsPath));
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
    public void initPrefs_saveDenied_warnsOnceAndUsesDefaultsForSession() {
        Path path = testFolder.resolve("prefs.json");
        JsonUserPrefsStorage storage = createFailingPrefsStorage(path, new AccessDeniedException(path.toString()));
        String output = captureMainAppLogs(() -> assertEquals(new UserPrefs(), mainApp.initPrefs(storage)));
        assertSingleWarning(output, "Could not save preferences to " + path
                + ": permission denied. Using preferences for this session only.");
        assertFalse(Files.exists(path));
    }

    @Test
    public void initPrefs_saveFails_retainsLoadedPreferencesAndFile() throws Exception {
        Path path = testFolder.resolve("prefs.json");
        UserPrefs saved = new UserPrefs();
        saved.setGuiSettings(new GuiSettings(1366, 768, 10, 20));
        new JsonUserPrefsStorage(path).saveUserPrefs(saved);
        String before = Files.readString(path);
        JsonUserPrefsStorage storage = createFailingPrefsStorage(path, new IOException("device unavailable"));
        String output = captureMainAppLogs(() -> assertEquals(saved, mainApp.initPrefs(storage)));
        assertSingleWarning(output, "Could not save preferences to " + path
                + ": device unavailable. Using preferences for this session only.");
        assertEquals(before, Files.readString(path));
    }

    @Test
    public void stop_writablePreferences_persistsWindowSettingsWithoutWarning() throws Exception {
        Path path = testFolder.resolve("prefs.json");
        JsonUserPrefsStorage prefsStorage = new JsonUserPrefsStorage(path);
        UserPrefs prefs = new UserPrefs();
        prefs.setGuiSettings(new GuiSettings(1366, 768, 10, 20));
        mainApp.storage = new StorageManager(new JsonStudentBookStorage(testFolder.resolve("students.json")),
                prefsStorage);
        mainApp.model = new ModelManager(new StudentBook(), prefs);

        String output = captureMainAppLogs(mainApp::stop);

        assertEquals(prefs, prefsStorage.readUserPrefs().orElseThrow());
        assertFalse(output.contains("WARNING"), output);
    }

    @Test
    public void stop_saveFails_warnsOnceWithoutStackTraceOrSevereLevel() {
        Path path = testFolder.resolve("prefs.json");
        IOException[] failures = {new AccessDeniedException(path.toString()),
            new FileSystemException(path.toString(), null, "Read-only file system"), new IOException()};
        String[] reasons = {"permission denied", "Read-only file system", "an I/O error occurred"};
        for (int i = 0; i < failures.length; i++) {
            mainApp.storage = new StorageManager(new JsonStudentBookStorage(testFolder.resolve("students.json")),
                    createFailingPrefsStorage(path, failures[i]));
            mainApp.model = new ModelManager(new StudentBook(), new UserPrefs());
            String output = captureMainAppLogs(mainApp::stop);
            assertSingleWarning(output, "Could not save window preferences to " + path + ": " + reasons[i]
                    + ". Window preferences will not be saved.");
            assertFalse(Files.exists(path));
        }
    }

    private JsonUserPrefsStorage createFailingPrefsStorage(Path path, IOException failure) {
        return new JsonUserPrefsStorage(path) {
            @Override
            public void saveUserPrefs(ReadOnlyUserPrefs prefs) throws IOException {
                throw failure;
            }
        };
    }

    private String captureMainAppLogs(Runnable action) {
        Logger logger = LogsCenter.getLogger(MainApp.class);
        boolean isUsingParentHandlers = logger.getUseParentHandlers();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        StreamHandler handler = new StreamHandler(bytes, new SimpleFormatter());
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            handler.flush();
            logger.removeHandler(handler);
            logger.setUseParentHandlers(isUsingParentHandlers);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private void assertSingleWarning(String output, String message) {
        assertEquals(1, output.lines().filter(line -> line.startsWith("WARNING:")).count(), output);
        assertTrue(output.contains("WARNING: " + message + System.lineSeparator()), output);
        assertFalse(output.contains("SEVERE"), output);
        assertFalse(output.contains("Exception"), output);
        assertFalse(output.contains("\tat "), output);
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
    public void init_nullGuiSettings_repairsPrefsAndPreservesStudentsAcrossLaunches() throws Exception {
        assertPrefsRecoveryPreservesStudentsAcrossLaunches("{\"guiSettings\": null}");
    }

    @Test
    public void init_nullPrefsDocument_repairsPrefsAndPreservesStudentsAcrossLaunches() throws Exception {
        assertPrefsRecoveryPreservesStudentsAcrossLaunches("null");
    }

    private void assertPrefsRecoveryPreservesStudentsAcrossLaunches(String prefsContents) throws Exception {
        Path prefsPath = testFolder.resolve("prefs.json");
        Files.writeString(prefsPath, prefsContents);
        JsonUserPrefsStorage prefsStorage = new JsonUserPrefsStorage(prefsPath);
        Path studentsPath = testFolder.resolve("students.json");
        new JsonStudentBookStorage(studentsPath).saveStudentBook(getTypicalStudentBook());
        String savedStudents = Files.readString(studentsPath);
        MainApp firstLaunch = new MainApp(prefsPath, studentsPath);

        firstLaunch.init();

        assertEquals(new GuiSettings(), firstLaunch.model.getGuiSettings());
        assertEquals(getTypicalStudentBook(), firstLaunch.model.getStudentBook());
        assertEquals(new UserPrefs(), prefsStorage.readUserPrefs().orElseThrow());

        MainApp secondLaunch = new MainApp(prefsPath, studentsPath);
        secondLaunch.init();

        assertEquals(new GuiSettings(), secondLaunch.model.getGuiSettings());
        assertEquals(getTypicalStudentBook(), secondLaunch.model.getStudentBook());
        assertEquals(new UserPrefs(), prefsStorage.readUserPrefs().orElseThrow());
        assertEquals(savedStudents, Files.readString(studentsPath));
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
