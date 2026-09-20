package seedu.tab.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.commons.core.GuiSettings;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.Student;

public class StorageManagerTest {

    @TempDir
    public Path testFolder;

    private StorageManager storageManager;

    @BeforeEach
    public void setUp() {
        JsonStudentBookStorage studentBookStorage = new JsonStudentBookStorage(getTempFilePath("ab"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(getTempFilePath("prefs"));
        storageManager = new StorageManager(studentBookStorage, userPrefsStorage);
    }

    private Path getTempFilePath(String fileName) {
        return testFolder.resolve(fileName);
    }

    @Test
    public void prefsReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonUserPrefsStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonUserPrefsStorageTest} class.
         */
        UserPrefs original = new UserPrefs();
        original.setGuiSettings(new GuiSettings(300, 600, 4, 6));
        storageManager.saveUserPrefs(original);
        UserPrefs retrieved = storageManager.readUserPrefs().get();
        assertEquals(original, retrieved);
    }

    @Test
    public void studentBookReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonStudentBookStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonStudentBookStorageTest} class.
         */
        StudentBook original = getTypicalStudentBook();
        storageManager.saveStudentBook(original);
        ReadOnlyStudentBook retrieved = storageManager.readStudentBook().get();
        assertEquals(original, new StudentBook(retrieved));
    }

    @Test
    public void getStudentBookFilePath() {
        assertNotNull(storageManager.getStudentBookFilePath());
    }

    @Test
    public void getUserPrefsFilePath() {
        assertNotNull(storageManager.getUserPrefsFilePath());
    }

    @Test
    public void studentBook_deleteAndPersistAcrossSessions_success() throws Exception {
        StudentBook sessionOneBook = getTypicalStudentBook();
        Student studentToDelete = sessionOneBook.getStudentList().get(0);
        sessionOneBook.removeStudent(studentToDelete);
        storageManager.saveStudentBook(sessionOneBook);

        StorageManager sessionTwoStorage = new StorageManager(
                new JsonStudentBookStorage(getTempFilePath("ab")),
                new JsonUserPrefsStorage(getTempFilePath("prefs")));
        ReadOnlyStudentBook sessionTwoBook = sessionTwoStorage.readStudentBook().get();
        assertFalse(sessionTwoBook.getStudentList().contains(studentToDelete));
        assertEquals(sessionOneBook.getStudentList().size(), sessionTwoBook.getStudentList().size());

        StudentBook sessionTwoMutableBook = new StudentBook(sessionTwoBook);
        Student nextStudentToDelete = sessionTwoMutableBook.getStudentList().get(0);
        sessionTwoMutableBook.removeStudent(nextStudentToDelete);
        sessionTwoStorage.saveStudentBook(sessionTwoMutableBook);

        StorageManager sessionThreeStorage = new StorageManager(
                new JsonStudentBookStorage(getTempFilePath("ab")),
                new JsonUserPrefsStorage(getTempFilePath("prefs")));
        ReadOnlyStudentBook sessionThreeBook = sessionThreeStorage.readStudentBook().get();
        assertFalse(sessionThreeBook.getStudentList().contains(studentToDelete));
        assertFalse(sessionThreeBook.getStudentList().contains(nextStudentToDelete));
        assertEquals(sessionTwoMutableBook.getStudentList().size(), sessionThreeBook.getStudentList().size());
    }

    @Test
    public void userPrefs_guiSettingsPersistAcrossSessions_success() throws Exception {
        UserPrefs sessionOnePrefs = new UserPrefs();
        GuiSettings customGuiSettings = new GuiSettings(1440, 900, 50, 100);
        sessionOnePrefs.setGuiSettings(customGuiSettings);
        storageManager.saveUserPrefs(sessionOnePrefs);

        StorageManager sessionTwoStorage = new StorageManager(
                new JsonStudentBookStorage(getTempFilePath("ab")),
                new JsonUserPrefsStorage(getTempFilePath("prefs")));
        UserPrefs sessionTwoPrefs = sessionTwoStorage.readUserPrefs().get();
        assertEquals(customGuiSettings, sessionTwoPrefs.getGuiSettings());
    }
}
