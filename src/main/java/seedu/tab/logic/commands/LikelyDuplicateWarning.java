package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;

/** Formats non-blocking warnings about student records with matching details. */
final class LikelyDuplicateWarning {
    static final String MESSAGE_WARNING =
            "WARNING: Possible duplicate student: %1$s has the same %2$s (%3$s). "
                    + "Delete one if they are the same person.";

    private LikelyDuplicateWarning() {
    }

    static String appendTo(String successMessage, Student changedStudent,
            Iterable<Student> existingStudents) {
        return appendWarnings(successMessage, changedStudent, existingStudents, null);
    }

    static String appendToExcluding(String successMessage, Student changedStudent,
            Iterable<Student> existingStudents, Student studentToExclude) {
        requireNonNull(studentToExclude);
        return appendWarnings(successMessage, changedStudent, existingStudents, studentToExclude);
    }

    private static String appendWarnings(String successMessage, Student changedStudent,
            Iterable<Student> existingStudents, Student studentToExclude) {
        requireNonNull(successMessage);
        requireNonNull(changedStudent);
        requireNonNull(existingStudents);

        List<String> warnings = new ArrayList<>();
        for (Student existingStudent : existingStudents) {
            if (existingStudent == studentToExclude) {
                continue;
            }
            addMatchingFieldWarnings(warnings, changedStudent, existingStudent);
        }
        return warnings.isEmpty() ? successMessage : successMessage + "\n\n" + String.join("\n", warnings);
    }

    private static void addMatchingFieldWarnings(List<String> warnings, Student changedStudent,
            Student existingStudent) {
        if (matchesIgnoringCase(changedStudent.getName().fullName, existingStudent.getName().fullName)) {
            addWarning(warnings, existingStudent, Name.FIELD_NAME, changedStudent.getName().fullName);
        }
        if (phoneWithoutSpaces(changedStudent).equals(phoneWithoutSpaces(existingStudent))) {
            addWarning(warnings, existingStudent, Phone.FIELD_NAME, changedStudent.getPhone().value);
        }
        changedStudent.getEmail().ifPresent(email -> existingStudent.getEmail()
                .filter(existingEmail -> matchesIgnoringCase(email.value, existingEmail.value))
                .ifPresent(unused -> addWarning(warnings, existingStudent, Email.FIELD_NAME, email.value)));
    }

    private static boolean matchesIgnoringCase(String first, String second) {
        return first.toLowerCase(Locale.ROOT).equals(second.toLowerCase(Locale.ROOT));
    }

    private static String phoneWithoutSpaces(Student student) {
        return student.getPhone().value.replace(" ", "");
    }

    private static void addWarning(List<String> warnings, Student existingStudent, String fieldName, String value) {
        warnings.add(String.format(MESSAGE_WARNING, existingStudent.getName(),
                fieldName.toLowerCase(Locale.ROOT), value));
    }
}
