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
        assertThrows(IllegalValueException.class, dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicateStudents_throwsIllegalValueException() throws Exception {
        JsonSerializableStudentBook dataFromFile = JsonUtil.readJsonFile(DUPLICATE_STUDENT_FILE,
                JsonSerializableStudentBook.class).get();
        String expectedMessage = "Student record 2 (\"Bob Lim\"): "
                + String.format(JsonSerializableStudentBook.MESSAGE_DUPLICATE_STUDENT, "A01234567X");
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
                Arrays.asList((JsonAdaptedStudent) null));

        assertThrows(IllegalValueException.class, "Student record 1 is missing.", dataFromFile::toModelType);
    }

    @Test
    public void toModelType_missingStudentList_throwsIllegalValueException() {
        JsonSerializableStudentBook dataFromFile =
                new JsonSerializableStudentBook((List<JsonAdaptedStudent>) null);

        assertThrows(IllegalValueException.class, JsonSerializableStudentBook.MESSAGE_MISSING_STUDENT_LIST,
                dataFromFile::toModelType);
    }

    @Test
    public void toModelType_studentWithoutName_reportsRecordNumberWithoutName() {
        JsonAdaptedStudent studentWithoutName = new JsonAdaptedStudent(
                null, "91234567", null, List.of(), false);
        JsonSerializableStudentBook dataFromFile = new JsonSerializableStudentBook(List.of(studentWithoutName));

        assertThrows(IllegalValueException.class, "Student record 1: Name field is missing.",
                dataFromFile::toModelType);
    }

}
