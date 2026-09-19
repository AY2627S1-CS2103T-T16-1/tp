package seedu.tab.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalStudents.ALICE;
import static seedu.tab.testutil.TypicalStudents.HOON;
import static seedu.tab.testutil.TypicalStudents.IDA;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;

public class JsonStudentBookStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonStudentBookStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readStudentBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readStudentBook(null));
    }

    private java.util.Optional<ReadOnlyStudentBook> readStudentBook(String filePath) throws Exception {
        return new JsonStudentBookStorage(Paths.get(filePath)).readStudentBook(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readStudentBook("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readStudentBook("notJsonFormatStudentBook.json"));
    }

    @Test
    public void readStudentBook_invalidStudentStudentBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readStudentBook("invalidStudentStudentBook.json"));
    }

    @Test
    public void readStudentBook_invalidAndValidStudentStudentBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readStudentBook("invalidAndValidStudentStudentBook.json"));
    }

    @Test
    public void readAndSaveStudentBook_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempStudentBook.json");
        StudentBook original = getTypicalStudentBook();
        JsonStudentBookStorage jsonStudentBookStorage = new JsonStudentBookStorage(filePath);

        // Save in new file and read back
        jsonStudentBookStorage.saveStudentBook(original, filePath);
        ReadOnlyStudentBook readBack = jsonStudentBookStorage.readStudentBook(filePath).get();
        assertEquals(original, new StudentBook(readBack));

        // Modify data, overwrite existing file, and read back
        original.addStudent(HOON);
        original.removeStudent(ALICE);
        jsonStudentBookStorage.saveStudentBook(original, filePath);
        readBack = jsonStudentBookStorage.readStudentBook(filePath).get();
        assertEquals(original, new StudentBook(readBack));

        // Save and read without specifying file path
        original.addStudent(IDA);
        jsonStudentBookStorage.saveStudentBook(original); // file path not specified
        readBack = jsonStudentBookStorage.readStudentBook().get(); // file path not specified
        assertEquals(original, new StudentBook(readBack));

    }

    @Test
    public void saveStudentBook_nullStudentBook_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveStudentBook(null, "SomeFile.json"));
    }

    /**
     * Saves {@code studentBook} at the specified {@code filePath}.
     */
    private void saveStudentBook(ReadOnlyStudentBook studentBook, String filePath) {
        try {
            new JsonStudentBookStorage(Paths.get(filePath))
                    .saveStudentBook(studentBook, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveStudentBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveStudentBook(new StudentBook(), null));
    }
}
