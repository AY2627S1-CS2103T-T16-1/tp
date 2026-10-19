package seedu.tab.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalStudents.ALICE;
import static seedu.tab.testutil.TypicalStudents.HOON;
import static seedu.tab.testutil.TypicalStudents.IDA;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.student.Email;
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
        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, () -> readStudentBook("notJsonFormatStudentBook.json"));

        assertTrue(exception.getMessage().contains("The data is not in the expected JSON format"));
        assertTrue(exception.getMessage().contains("line 1, column 4"));
    }

    @Test
    public void readStudentBook_nullDocument_throwsDataLoadingException() {
        assertThrows(DataLoadingException.class, "The JSON document must not be null.", () ->
                readStudentBook("nullStudentBook.json"));
    }

    @Test
    public void readStudentBook_invalidStudentStudentBook_throwDataLoadingException() {
        String expectedMessage = "Student record 1 (\"---\"): Name \"---\" is not valid: "
                + "it holds no letter or number";
        assertThrows(DataLoadingException.class, expectedMessage, () ->
                readStudentBook("invalidStudentStudentBook.json"));
    }

    @Test
    public void readStudentBook_invalidAndValidStudentStudentBook_throwDataLoadingException() {
        String expectedMessage = "Student record 2 (\"Student With Invalid Phone Field\"): "
                + "Phone \"12\" is not valid: a phone number needs at least 3 digits";
        assertThrows(DataLoadingException.class, expectedMessage, () ->
                readStudentBook("invalidAndValidStudentStudentBook.json"));
    }

    @Test
    public void readStudentBook_missingStudentList_reportsErrorAndPreservesFile() throws Exception {
        Path filePath = testFolder.resolve("MissingStudentList.json");
        String originalContents = "{}";
        Files.writeString(filePath, originalContents);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, storage::readStudentBook);

        assertEquals(JsonSerializableStudentBook.MESSAGE_MISSING_STUDENT_LIST, exception.getMessage());
        assertEquals(originalContents, Files.readString(filePath));
    }

    @Test
    public void readStudentBook_extremelyLongEmail_reportsRecordAndPreservesFile() throws Exception {
        Path filePath = testFolder.resolve("LongEmail.json");
        String longEmail = "a.".repeat(300) + "a@example.com";
        String originalContents = String.format("""
                {
                  "students": [ {
                    "name": "Ravi",
                    "phone": "91234567",
                    "email": "%s"
                  } ]
                }
                """, longEmail);
        Files.writeString(filePath, originalContents);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        DataLoadingException exception = org.junit.jupiter.api.Assertions.assertThrows(
                DataLoadingException.class, storage::readStudentBook);

        assertTrue(exception.getMessage().contains("Student record 1 (\"Ravi\"): Email field"));
        assertTrue(exception.getMessage().contains(String.valueOf(longEmail.length())));
        assertEquals(originalContents, Files.readString(filePath));
    }

    @Test
    public void readStudentBook_invalidNusStudentIdStudentBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readStudentBook("invalidNusStudentIdStudentBook.json"));
    }

    @Test
    public void readStudentBook_duplicateNusStudentId_reportsNormalizedId() {
        String expectedMessage = "Student record 2 (\"Lee Wei Ming\"): NUS Student ID A01234567X "
                + "is already used by student record 1 (\"Tan Wei Ming\").";

        assertThrows(DataLoadingException.class, expectedMessage, () ->
                readStudentBook("duplicateNusStudentIdStudentBook.json"));
    }

    @Test
    public void readStudentBook_fileHoldingTheRemovedAddressField_loadsWithoutIt() throws Exception {
        // Every data file written before the address field was removed still holds an
        // "address" key. Those files have to keep loading, with the key ignored.
        ReadOnlyStudentBook loaded = readStudentBook("withAddressFieldStudentBook.json").get();

        Student expected = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withEmail("alice@example.com").withTags("friends").build();

        assertEquals(1, loaded.getStudentList().size());
        assertEquals(expected, loaded.getStudentList().get(0));
    }

    @Test
    public void readStudentBook_handEditedNullEmail_loadsWithoutOne() throws Exception {
        // a file edited by hand may spell an absent email either way, and both have to load
        ReadOnlyStudentBook loaded = readStudentBook("nullEmailStudentBook.json").get();

        Student explicitNull = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withTags("friends").withoutEmail().build();
        Student keyLeftOut = new StudentBuilder().withName("Benson Meier").withPhone("98765432")
                .withTags("owesMoney").withoutEmail().build();

        assertEquals(2, loaded.getStudentList().size());
        assertEquals(explicitNull, loaded.getStudentList().get(0));
        assertEquals(keyLeftOut, loaded.getStudentList().get(1));
    }

    @Test
    public void readStudentBook_handEditedEmailWhitespace_loadsAndPreservesAllStudents() throws Exception {
        Path filePath = testFolder.resolve("EmailWhitespace.json");
        Files.writeString(filePath, """
                {
                  "students": [ {
                    "name": "Alex Yeoh",
                    "phone": "87438807",
                    "email": ""
                  }, {
                    "name": "Bernice Yu",
                    "phone": "99272758",
                    "email": "berniceyu@example.com "
                  }, {
                    "name": "Charlotte Oliveiro",
                    "phone": "93210283"
                  } ]
                }
                """);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        ReadOnlyStudentBook loaded = storage.readStudentBook().get();

        assertEquals(3, loaded.getStudentList().size());
        assertEquals(Optional.empty(), loaded.getStudentList().get(0).getEmail());
        assertEquals(Optional.of(new Email("berniceyu@example.com")), loaded.getStudentList().get(1).getEmail());
        assertEquals(Optional.empty(), loaded.getStudentList().get(2).getEmail());

        StudentBook updated = new StudentBook(loaded);
        updated.addStudent(new StudentBuilder().withName("David Li").withPhone("91031282").build());
        storage.saveStudentBook(updated);

        assertEquals(updated, new StudentBook(storage.readStudentBook().get()));
    }

    @Test
    public void readStudentBook_handEditedNullNusStudentId_loadsWithoutOne() throws Exception {
        ReadOnlyStudentBook loaded = readStudentBook("nullNusStudentIdStudentBook.json").get();

        Student explicitNull = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withTags("friends").withoutEmail().withoutNusStudentId().build();
        Student keyLeftOut = new StudentBuilder().withName("Benson Meier").withPhone("98765432")
                .withTags("owesMoney").withoutEmail().withoutNusStudentId().build();

        assertEquals(2, loaded.getStudentList().size());
        assertEquals(explicitNull, loaded.getStudentList().get(0));
        assertEquals(keyLeftOut, loaded.getStudentList().get(1));
    }

    @Test
    public void readStudentBook_handEditedFlagValues_loadWithCompatibleDefaults() throws Exception {
        ReadOnlyStudentBook loaded = readStudentBook("followUpFlagStudentBook.json").get();

        assertEquals(4, loaded.getStudentList().size());
        assertTrue(loaded.getStudentList().get(0).isFlagged());
        assertFalse(loaded.getStudentList().get(1).isFlagged());
        assertFalse(loaded.getStudentList().get(2).isFlagged());
        assertFalse(loaded.getStudentList().get(3).isFlagged());
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
    public void readAndSaveStudentBook_studentWithoutEmail_roundTripsAndLeavesTheKeyOut() throws Exception {
        Path filePath = testFolder.resolve("NoEmail.json");
        Student student = new StudentBuilder().withName("Wong Mei Ling").withPhone("91234567")
                .withTags("Lab 3").withoutEmail().build();

        StudentBook original = new StudentBook();
        original.addStudent(student);

        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(original, filePath);

        // the file is meant to stay editable by hand, so an absent email is an absent key
        assertFalse(Files.readString(filePath).contains("email"));

        ReadOnlyStudentBook readBack = storage.readStudentBook(filePath).get();
        assertEquals(original, new StudentBook(readBack));
        assertEquals(Optional.empty(), readBack.getStudentList().get(0).getEmail());
    }

    @Test
    public void readAndSaveStudentBook_optionalNusStudentId_roundTripsAndLeavesAbsentKeyOut() throws Exception {
        Path filePath = testFolder.resolve("OptionalNusStudentId.json");
        Student withNusStudentId = new StudentBuilder().withName("Wong Mei Ling").withPhone("91234567")
                .withNusStudentId("A01234567X").build();
        Student withoutNusStudentId = new StudentBuilder().withName("Ravi Kumar").withPhone("91234568").build();
        StudentBook original = new StudentBook();
        original.addStudent(withNusStudentId);
        original.addStudent(withoutNusStudentId);

        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(original, filePath);

        String savedJson = Files.readString(filePath);
        assertTrue(savedJson.contains("\"nusStudentId\" : \"A01234567X\""));
        assertEquals(1, savedJson.split("\"nusStudentId\"", -1).length - 1);
        assertEquals(original, new StudentBook(storage.readStudentBook(filePath).get()));
    }

    @Test
    public void readAndSaveStudentBook_sameNameWithDifferentOrAbsentNusStudentIds_roundTrips() throws Exception {
        Path filePath = testFolder.resolve("StudentsWithSameName.json");
        Student first = new StudentBuilder().withName("Tan Wei Ming").withPhone("91234567")
                .withNusStudentId("A01234567X").build();
        Student second = new StudentBuilder(first).withPhone("92345678")
                .withNusStudentId("A76543210Y").build();
        Student withoutNusStudentId = new StudentBuilder(first).withPhone("93456789")
                .withoutNusStudentId().build();
        StudentBook original = new StudentBook();
        original.addStudent(first);
        original.addStudent(second);
        original.addStudent(withoutNusStudentId);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        storage.saveStudentBook(original, filePath);
        ReadOnlyStudentBook readBack = storage.readStudentBook(filePath).get();

        assertEquals(original, new StudentBook(readBack));
        assertEquals(3, readBack.getStudentList().size());
    }

    @Test
    public void readAndSaveStudentBook_identicalStudentsWithoutNusStudentIds_preservesCountAndOrder()
            throws Exception {
        Path filePath = testFolder.resolve("IdenticalStudentsWithoutNusStudentIds.json");
        Student first = new StudentBuilder().withName("Tan Wei Ming").withPhone("91234567")
                .withoutNusStudentId().build();
        Student middle = new StudentBuilder().withName("Alice Tan").withPhone("92345678")
                .withoutNusStudentId().build();
        Student second = new StudentBuilder(first).build();
        StudentBook original = new StudentBook();
        original.addStudent(first);
        original.addStudent(middle);
        original.addStudent(second);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);

        storage.saveStudentBook(original, filePath);
        ReadOnlyStudentBook readBack = storage.readStudentBook(filePath).orElseThrow();

        assertEquals(3, readBack.getStudentList().size());
        assertEquals(List.of(first, middle, second), readBack.getStudentList());
    }

    @Test
    public void readAndSaveStudentBook_followUpFlag_roundTripsAndOmitsFalse() throws Exception {
        Path filePath = testFolder.resolve("FollowUpFlag.json");
        Student flaggedStudent = new StudentBuilder().withName("Wong Mei Ling").withPhone("91234567")
                .withFlag(true).build();
        Student unflaggedStudent = new StudentBuilder().withName("Ravi Kumar").withPhone("91234568")
                .withFlag(false).build();
        StudentBook original = new StudentBook();
        original.addStudent(flaggedStudent);
        original.addStudent(unflaggedStudent);

        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(original, filePath);

        String savedJson = Files.readString(filePath);
        assertTrue(savedJson.contains("\"flag\" : true"));
        assertEquals(1, savedJson.split("\"flag\"", -1).length - 1);
        assertEquals(original, new StudentBook(storage.readStudentBook(filePath).get()));
    }

    @Test
    public void readAndSaveStudentBook_clearedOptionalDetails_omitsKeysAndPreservesOtherFields() throws Exception {
        Path filePath = testFolder.resolve("ClearedDetails.json");
        Student student = new StudentBuilder(ALICE).withFlag(true).build();
        StudentBook book = new StudentBook();
        book.addStudent(student);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(filePath);
        storage.saveStudentBook(book);
        Student loaded = storage.readStudentBook().orElseThrow().getStudentList().get(0);
        assertEquals(student, loaded);

        Student cleared = new StudentBuilder(loaded).withoutEmail().withoutNusStudentId().build();
        book.setStudent(student, cleared);
        storage.saveStudentBook(book);

        String savedJson = Files.readString(filePath);
        assertFalse(savedJson.contains("\"email\""));
        assertFalse(savedJson.contains("\"nusStudentId\""));
        assertTrue(savedJson.contains("\"flag\" : true"));
        assertEquals(book, new StudentBook(storage.readStudentBook().orElseThrow()));
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
