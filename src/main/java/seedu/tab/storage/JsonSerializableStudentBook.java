package seedu.tab.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import seedu.tab.commons.exceptions.IllegalValueException;
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
                throw new IllegalValueException(String.format("Student record %d is missing.", index + 1));
            }
            String recordDescription = getRecordDescription(index, jsonAdaptedStudent);
            Student student;
            try {
                student = jsonAdaptedStudent.toModelType();
            } catch (IllegalValueException e) {
                throw new IllegalValueException(recordDescription + ": " + e.getMessage(), e);
            }
            if (studentBook.hasStudent(student)) {
                int duplicateIndex = findDuplicateIndex(studentBook, student);
                Student duplicateStudent = studentBook.getStudentList().get(duplicateIndex);
                throw new IllegalValueException(recordDescription + ": "
                        + String.format(MESSAGE_DUPLICATE_STUDENT, student.getNusStudentId().orElseThrow(),
                                duplicateIndex + 1, duplicateStudent.getName()));
            }
            studentBook.addStudent(student);
        }
        return studentBook;
    }

    private static String getRecordDescription(int index, JsonAdaptedStudent student) {
        String name = student.getNameForErrorMessage();
        if (name == null) {
            return String.format("Student record %d", index + 1);
        }
        return String.format("Student record %d (\"%s\")", index + 1, name);
    }

    private static int findDuplicateIndex(StudentBook studentBook, Student student) {
        for (int index = 0; index < studentBook.getStudentList().size(); index++) {
            if (student.isSameStudent(studentBook.getStudentList().get(index))) {
                return index;
            }
        }
        throw new AssertionError("Expected a student with the same identity");
    }

}
