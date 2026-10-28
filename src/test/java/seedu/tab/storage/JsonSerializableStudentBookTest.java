package seedu.tab.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.testutil.Assert.assertThrows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.exceptions.IllegalValueException;
import seedu.tab.commons.util.JsonUtil;
import seedu.tab.model.StudentBook;
import seedu.tab.testutil.TypicalStudents;

public class JsonSerializableStudentBookTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonSerializableStudentBookTest");
    private static final Path TYPICAL_STUDENTS_FILE = TEST_DATA_FOLDER.resolve("typicalStudentsStudentBook.json");
    private static final Path INVALID_STUDENT_FILE = TEST_DATA_FOLDER.resolve("invalidStudentStudentBook.json");
    private static final Path DUPLICATE_STUDENT_FILE = TEST_DATA_FOLDER.resolve("duplicateStudentStudentBook.json");
    private static final Path SAME_NAME_STUDENTS_FILE = TEST_DATA_FOLDER.resolve("sameNameStudentsStudentBook.json");

    @Test
    public void toModelType_typicalStudentsFile_success() throws Exception {
        JsonSerializableStudentBook dataFromFile = JsonUtil.readJsonFile(TYPICAL_STUDENTS_FILE,
                JsonSerializableStudentBook.class).get();
        StudentBook studentBookFromFile = dataFromFile.toModelType();
        StudentBook typicalStudentsStudentBook = TypicalStudents.getTypicalStudentBook();
        assertEquals(studentBookFromFile, typicalStudentsStudentBook);
    }

    @Test
    public void toModelType_invalidStudentFile_throwsIllegalValueException() throws Exception {
        JsonSerializableStudentBook dataFromFile = JsonUtil.readJsonFile(INVALID_STUDENT_FILE,
                JsonSerializableStudentBook.class).get();
        assertThrows(IllegalValueException.class,
                "Student record 1 (\"Hans Muster\"): Email \"invalid@email!3e\" is not valid: "
                        + "the domain may hold ASCII letters and digits in labels separated by dots, "
                        + "with hyphens allowed inside a label", dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicateStudents_throwsIllegalValueException() throws Exception {
        JsonSerializableStudentBook dataFromFile = JsonUtil.readJsonFile(DUPLICATE_STUDENT_FILE,
                JsonSerializableStudentBook.class).get();
        String expectedMessage = "Student record 2 (\"Bob Lim\"): "
                + String.format(JsonSerializableStudentBook.MESSAGE_DUPLICATE_STUDENT,
                        "A01234567X", 1, "Alice Pauline");
        assertThrows(IllegalValueException.class, expectedMessage, dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicateMatchesLaterStudent_reportsMatchingRecord() {
        JsonAdaptedStudent firstStudent = new JsonAdaptedStudent(
                "Alice Pauline", "94351253", "alice@example.com", "A01234567X", List.of(), false);
        JsonAdaptedStudent matchingStudent = new JsonAdaptedStudent(
                "Carol Elle", "98765432", "carol@example.com", "A12345678C", List.of(), false);
        JsonAdaptedStudent duplicateStudent = new JsonAdaptedStudent(
                "Bob Lim", "91234567", "bob@example.com", "a12345678c", List.of(), false);
        JsonSerializableStudentBook dataFromFile = new JsonSerializableStudentBook(
                null, List.of(firstStudent, matchingStudent, duplicateStudent));
        String expectedMessage = "Student record 3 (\"Bob Lim\"): "
                + String.format(JsonSerializableStudentBook.MESSAGE_DUPLICATE_STUDENT,
                        "A12345678C", 2, "Carol Elle");

        assertThrows(IllegalValueException.class, expectedMessage, dataFromFile::toModelType);
    }

    @Test
    public void toModelType_sameNameWithDifferentOrAbsentNusStudentIds_success() throws Exception {
        JsonSerializableStudentBook dataFromFile = JsonUtil.readJsonFile(SAME_NAME_STUDENTS_FILE,
                JsonSerializableStudentBook.class).get();

        assertEquals(3, dataFromFile.toModelType().getStudentList().size());
    }

    @Test
    public void toModelType_nullStudentRecord_throwsIllegalValueException() {
        JsonSerializableStudentBook dataFromFile = new JsonSerializableStudentBook(
                null, Arrays.asList((JsonAdaptedStudent) null));

        assertThrows(IllegalValueException.class, "Student record 1 is missing.", dataFromFile::toModelType);
    }

    @Test
    public void toModelType_missingStudentList_throwsIllegalValueException() {
        JsonSerializableStudentBook dataFromFile =
                new JsonSerializableStudentBook(null, (List<JsonAdaptedStudent>) null);

        assertThrows(IllegalValueException.class, JsonSerializableStudentBook.MESSAGE_MISSING_STUDENT_LIST,
                dataFromFile::toModelType);
    }

    @Test
    public void toModelType_studentWithoutName_reportsRecordNumberWithoutName() {
        JsonAdaptedStudent studentWithoutName = new JsonAdaptedStudent(
                null, "91234567", null, List.of(), false);
        JsonSerializableStudentBook dataFromFile = new JsonSerializableStudentBook(null, List.of(studentWithoutName));

        assertThrows(IllegalValueException.class, "Student record 1: Name field is missing.",
                dataFromFile::toModelType);
    }

}
