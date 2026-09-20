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
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

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
    public void readAndSaveStudentBook_widenedPhoneAndTag_roundTrips() throws Exception {
        // A phone number with a country code and a tag with a space are newly accepted.
        // Saving is only half the claim; they have to come back unchanged.
        Path filePath = testFolder.resolve("WidenedValues.json");
        Student student = new StudentBuilder().withName("Ravi s/o Kumaran")
                .withPhone("+65 9123 4567").withEmail("e0923841@u.nus.edu")
                .withAddress("Blk 30 Geylang Street 29")
                .withTags("Lab 3", "needs-followup").build();

        StudentBook original = new StudentBook();
        original.addStudent(student);

        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(original, filePath);
        ReadOnlyStudentBook readBack = storage.readStudentBook(filePath).get();

        assertEquals(original, new StudentBook(readBack));

        Student loaded = readBack.getStudentList().get(0);
        assertEquals("+65 9123 4567", loaded.getPhone().value);
        assertEquals("Ravi s/o Kumaran", loaded.getName().fullName);
        assertEquals(student.getTags(), loaded.getTags());
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
