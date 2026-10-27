package seedu.tab.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import seedu.tab.commons.exceptions.IllegalValueException;
import seedu.tab.commons.util.ValueMessages;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.student.Student;

/**
 * An Immutable StudentBook that is serializable to JSON format.
 */
@JsonRootName(value = "studentbook")
class JsonSerializableStudentBook {

    public static final String MESSAGE_DUPLICATE_STUDENT =
            "NUS Student ID %1$s is already used by student record %2$d (\"%3$s\").";
    public static final String MESSAGE_MISSING_STUDENT_LIST = "Student list is missing.";

    private static final String COMMENT_FIELD = "_comment";
    private static final String MESSAGE_UNKNOWN_FIELD = "Unknown field \"%s\" in the student book. "
            + "Expected top-level fields: students, _comment (optional description).";
    private static final String MESSAGE_MISSING_RECORD = "Student record %d is missing.";
    private static final String RECORD_DESCRIPTION = "Student record %d";
    private static final String NAMED_RECORD_DESCRIPTION = "Student record %d (\"%s\")";

    private final List<JsonAdaptedStudent> students;

    /**
     * Constructs a {@code JsonSerializableStudentBook} with the given students.
     */
    @JsonCreator
    public JsonSerializableStudentBook(@JsonProperty("students") List<JsonAdaptedStudent> students) {
        this.students = students == null ? null : new ArrayList<>(students);
    }

    /**
     * Converts a given {@code ReadOnlyStudentBook} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableStudentBook}.
     */
    public JsonSerializableStudentBook(ReadOnlyStudentBook source) {
        students = source.getStudentList().stream().map(JsonAdaptedStudent::new).collect(Collectors.toList());
    }

    /**
     * Rejects unknown top-level keys instead of silently dropping their contents.
     */
    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        // Existing hand-editable fixtures use this key for a file description.
        if (field.equals(COMMENT_FIELD)) {
            return;
        }
        throw new IllegalArgumentException(String.format(MESSAGE_UNKNOWN_FIELD, ValueMessages.shorten(field)));
    }

    /**
     * Converts this student book into the model's {@code StudentBook} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public StudentBook toModelType() throws IllegalValueException {
        if (students == null) {
            throw new IllegalValueException(MESSAGE_MISSING_STUDENT_LIST);
        }

        StudentBook studentBook = new StudentBook();
        for (int index = 0; index < students.size(); index++) {
            JsonAdaptedStudent jsonAdaptedStudent = students.get(index);
            if (jsonAdaptedStudent == null) {
                throw new IllegalValueException(String.format(Locale.ROOT, MESSAGE_MISSING_RECORD, index + 1));
            }
            String recordDescription = getRecordDescription(index, jsonAdaptedStudent);
            Student student;
            try {
                student = jsonAdaptedStudent.toModelType();
            } catch (IllegalValueException e) {
                throw new IllegalValueException(recordDescription + ": " + e.getMessage(), e);
            }
            int duplicateIndex = findDuplicateIndex(studentBook, student);
            if (duplicateIndex >= 0) {
                Student duplicateStudent = studentBook.getStudentList().get(duplicateIndex);
                throw new IllegalValueException(recordDescription + ": "
                        + String.format(Locale.ROOT, MESSAGE_DUPLICATE_STUDENT, student.getNusStudentId().orElseThrow(),
                                duplicateIndex + 1, duplicateStudent.getName()));
            }
            studentBook.addStudent(student);
        }
        return studentBook;
    }

    /**
     * Returns a one-based record description with its saved name when available.
     */
    private static String getRecordDescription(int index, JsonAdaptedStudent student) {
        String name = student.getNameForErrorMessage();
        if (name == null) {
            return String.format(Locale.ROOT, RECORD_DESCRIPTION, index + 1);
        }
        return String.format(Locale.ROOT, NAMED_RECORD_DESCRIPTION, index + 1, name);
    }

    /**
     * Returns the existing record with the same identity, or -1 when this identity is new.
     */
    private static int findDuplicateIndex(StudentBook studentBook, Student student) {
        for (int index = 0; index < studentBook.getStudentList().size(); index++) {
            if (student.isSameStudent(studentBook.getStudentList().get(index))) {
                return index;
            }
        }
        return -1;
    }

}
