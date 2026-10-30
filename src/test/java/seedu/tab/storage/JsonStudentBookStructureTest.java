package seedu.tab.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.model.ReadOnlyStudentBook;

/**
 * Tests the diagnostics a data file gets when its structure, rather than one of its field
 * values, is the mistake.
 */
public class JsonStudentBookStructureTest {

    private static final String VALID_BOOK = "{\n  \"students\" : [ {\n    \"name\" : \"Alex Yeoh\"\n  }, {\n"
            + "    \"name\" : \"Bernice Yu\"\n  } ]\n}\n";

    @TempDir
    public Path testFolder;

    @Test
    public void readStudentBook_rootIsArray_namesExpectedStudentBookShape() throws Exception {
        DataLoadingException exception = assertReadFails("[ { \"name\": \"Bob\" } ]\n");

        assertTrue(exception.getMessage().contains("The saved data must be a JSON object holding a students array,"
                + " such as {\"students\": [{\"name\": \"Alex Yeoh\"}]}."), exception.getMessage());
        assertLocationIsShown(exception, "at line 1, column 1");
    }

    @Test
    public void readStudentBook_rootIsBareValue_namesExpectedStudentBookShape() throws Exception {
        for (String json : List.of("\"Bob\"\n", "42\n", "true\n")) {
            DataLoadingException exception = assertReadFails(json);

            assertTrue(exception.getMessage().contains("The saved data must be a JSON object holding a students"
                    + " array"), exception.getMessage());
            assertLocationIsShown(exception, "at line 1, column 1");
        }
    }

    @Test
    public void readStudentBook_studentListIsNotArray_namesStudentsFieldAndItsShape() throws Exception {
        DataLoadingException exception = assertReadFails("{\n  \"students\" : { \"name\" : \"Bob\" }\n}\n");

        assertTrue(exception.getMessage().contains("Invalid students value: an object."
                + " Use a JSON array of student records, such as [{\"name\": \"Alex Yeoh\"}]."),
                exception.getMessage());
        assertLocationIsShown(exception, "at line 2, column 16");
    }

    @Test
    public void readStudentBook_studentListIsText_namesStudentsFieldAndItsShape() throws Exception {
        DataLoadingException exception = assertReadFails("{\n  \"students\" : \"Bob\"\n}\n");

        assertTrue(exception.getMessage().contains("Invalid students value: \"Bob\"."
                + " Use a JSON array of student records, such as [{\"name\": \"Alex Yeoh\"}]."),
                exception.getMessage());
    }

    @Test
    public void readStudentBook_studentListEntryIsNotObject_namesEntryAndItsShape() throws Exception {
        DataLoadingException exception = assertReadFails("{\n  \"students\" : [ \"Bob\" ]\n}\n");

        assertTrue(exception.getMessage().contains("Invalid students entry 1 value: \"Bob\"."
                + " Each student record must be a JSON object, such as {\"name\": \"Alex Yeoh\"}."),
                exception.getMessage());
        assertLocationIsShown(exception, "at line 2, column 18");
    }

    @Test
    public void readStudentBook_laterStudentListEntryIsNotObject_namesThatEntry() throws Exception {
        DataLoadingException exception =
                assertReadFails("{\n  \"students\" : [ { \"name\" : \"Bob\" }, 5 ]\n}\n");

        assertTrue(exception.getMessage().contains("Invalid students entry 2 value: 5."),
                exception.getMessage());
    }

    @Test
    public void readStudentBook_validFile_loadsEveryRecord() throws Exception {
        Path path = testFolder.resolve("tab.json");
        byte[] bytes = VALID_BOOK.getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes);

        ReadOnlyStudentBook book = new JsonStudentBookStorage(path).readStudentBook().orElseThrow();

        assertEquals(List.of("Alex Yeoh", "Bernice Yu"),
                book.getStudentList().stream().map(student -> student.getName().getFullName()).toList());
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void readStudentBook_fieldMistakeInRecord_keepsRecordNumberAndFieldDiagnostic() throws Exception {
        DataLoadingException tagsFailure =
                assertReadFails("{\n  \"students\" : [ { \"name\" : \"Bob\", \"tags\" : \"friends\" } ]\n}\n");
        DataLoadingException emailFailure = assertReadFails("{\n  \"students\" : [ { \"name\" : \"Bob\","
                + " \"email\" : [ \"bob@example.com\" ] } ]\n}\n");

        assertTrue(tagsFailure.getMessage().startsWith("Student record 1: "), tagsFailure.getMessage());
        assertTrue(tagsFailure.getMessage().contains("Invalid tags value: \"friends\"."
                + " Use a JSON array of tag strings, such as [\"Lab 3\"]."), tagsFailure.getMessage());
        assertTrue(emailFailure.getMessage().startsWith("Student record 1: "), emailFailure.getMessage());
        assertTrue(emailFailure.getMessage().contains("email must be a single text value, not an array."),
                emailFailure.getMessage());
    }

    @Test
    public void readStudentBook_studentListMissingOrNull_reportsMissingStudentList() throws Exception {
        for (String json : List.of("{}\n", "{ \"_comment\" : \"CS2103T T16\" }\n", "{ \"students\" : null }\n")) {
            DataLoadingException exception = assertReadFails(json);

            assertEquals(JsonSerializableStudentBook.MESSAGE_MISSING_STUDENT_LIST, exception.getMessage());
        }
    }

    @Test
    public void readStudentBook_studentListEntryIsNull_reportsMissingRecord() throws Exception {
        DataLoadingException exception = assertReadFails("{\n  \"students\" : [ null ]\n}\n");

        assertEquals("Student record 1 is missing.", exception.getMessage());
    }

    /**
     * Returns the failure reading {@code json} as a data file gives, having confirmed that the
     * file is left as it was found.
     */
    private DataLoadingException assertReadFails(String json) throws IOException {
        Path path = testFolder.resolve("tab.json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes);

        DataLoadingException exception = assertThrows(
                DataLoadingException.class, () -> new JsonStudentBookStorage(path).readStudentBook());

        assertArrayEquals(bytes, Files.readAllBytes(path));
        return exception;
    }

    private static void assertLocationIsShown(DataLoadingException exception, String location) {
        assertTrue(exception.getMessage().contains(location), exception.getMessage());
        assertTrue(exception.getMessage().contains("Affected source:"), exception.getMessage());
    }
}
