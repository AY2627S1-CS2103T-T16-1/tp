package seedu.tab.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.testutil.Assert.assertThrows;

import java.nio.file.Path;
import java.nio.file.Paths;

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
        assertThrows(IllegalValueException.class, JsonSerializableStudentBook.MESSAGE_DUPLICATE_STUDENT,
                dataFromFile::toModelType);
    }

}
