package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;

/**
 * Formats non-blocking warnings about student records with matching details.
 */
final class LikelyDuplicateWarning {
    static final String MESSAGE_WARNING_SINGULAR =
            "WARNING: Possible duplicate student: %1$s has the same %2$s.";
    static final String MESSAGE_WARNING_PLURAL =
            "WARNING: Possible duplicate students: %1$s have the same %2$s.";
    static final String MESSAGE_ADVICE =
            "Review the records and delete duplicates only if they are the same person.";

    private static final int MAX_NAMED_STUDENTS = 2;

    private LikelyDuplicateWarning() {
    }

    static String appendTo(String successMessage, Student changedStudent,
            Iterable<Student> existingStudents) {
        return appendWarnings(successMessage, changedStudent, existingStudents, null);
    }

    static String appendChangedFieldsTo(String successMessage, Student originalStudent,
            Student changedStudent, Iterable<Student> existingStudents) {
        requireNonNull(originalStudent);
        return appendWarnings(successMessage, changedStudent, existingStudents, originalStudent);
    }

    /**
     * Returns {@code successMessage} with a warning for each field of {@code changedStudent} that
     * another record holds. A null {@code originalStudent} names a student being added, so every
     * field is new; an edit instead carries the record as it stands, whose own fields are no
     * evidence of a duplicate.
     */
    private static String appendWarnings(String successMessage, Student changedStudent,
            Iterable<Student> existingStudents, Student originalStudent) {
        requireNonNull(successMessage);
        requireNonNull(changedStudent);
        requireNonNull(existingStudents);

        List<String> warnings = collectWarnings(changedStudent, existingStudents, originalStudent);
        return warnings.isEmpty()
                ? successMessage
                : successMessage + "\n\n" + String.join("\n", warnings) + "\n" + MESSAGE_ADVICE;
    }

    private static List<String> collectWarnings(Student changedStudent,
            Iterable<Student> existingStudents, Student originalStudent) {
        List<Student> otherStudents = listOtherStudents(existingStudents, originalStudent);
        List<String> warnings = new ArrayList<>();
        addWarning(warnings, findNameMatches(changedStudent, originalStudent, otherStudents), Name.FIELD_NAME);
        addWarning(warnings, findPhoneMatches(changedStudent, originalStudent, otherStudents), Phone.FIELD_NAME);
        addWarning(warnings, findEmailMatches(changedStudent, originalStudent, otherStudents), Email.FIELD_NAME);
        return warnings;
    }

    /** Returns every existing student apart from the record being edited, in display order. */
    private static List<Student> listOtherStudents(Iterable<Student> existingStudents, Student originalStudent) {
        List<Student> otherStudents = new ArrayList<>();
        for (Student existingStudent : existingStudents) {
            if (existingStudent != originalStudent) {
                otherStudents.add(existingStudent);
            }
        }
        return otherStudents;
    }

    /** Returns the students holding the changed student's name, or none when the name stands. */
    private static List<Student> findNameMatches(Student changedStudent, Student originalStudent,
            List<Student> otherStudents) {
        if (originalStudent != null && changedStudent.getName().equals(originalStudent.getName())) {
            return List.of();
        }
        return otherStudents.stream()
                .filter(existingStudent -> hasMatchingName(changedStudent, existingStudent))
                .toList();
    }

    /** Returns the students holding the changed student's phone, or none when the phone stands. */
    private static List<Student> findPhoneMatches(Student changedStudent, Student originalStudent,
            List<Student> otherStudents) {
        if (originalStudent != null && changedStudent.getPhone().equals(originalStudent.getPhone())) {
            return List.of();
        }
        return otherStudents.stream()
                .filter(existingStudent -> hasMatchingPhone(changedStudent, existingStudent))
                .toList();
    }

    /** Returns the students holding the changed student's email, or none when the email stands. */
    private static List<Student> findEmailMatches(Student changedStudent, Student originalStudent,
            List<Student> otherStudents) {
        if (originalStudent != null && changedStudent.getEmail().equals(originalStudent.getEmail())) {
            return List.of();
        }
        return otherStudents.stream()
                .filter(existingStudent -> hasMatchingEmail(changedStudent, existingStudent))
                .toList();
    }

    private static boolean hasMatchingName(Student first, Student second) {
        return StringUtil.normalizeNameForComparison(first.getName().getFullName())
                .equals(StringUtil.normalizeNameForComparison(second.getName().getFullName()));
    }

    private static boolean hasMatchingPhone(Student first, Student second) {
        return first.getPhone().flatMap(phone -> second.getPhone()
                .filter(otherPhone -> phone.getDigits().equals(otherPhone.getDigits()))).isPresent();
    }

    private static boolean hasMatchingEmail(Student first, Student second) {
        return first.getEmail().flatMap(email -> second.getEmail()
                .filter(otherEmail -> matchesIgnoringCase(email.getValue(), otherEmail.getValue()))).isPresent();
    }

    private static boolean matchesIgnoringCase(String first, String second) {
        return first.toLowerCase(Locale.ROOT).equals(second.toLowerCase(Locale.ROOT));
    }

    private static void addWarning(List<String> warnings, List<Student> matchingStudents,
            String fieldName) {
        if (matchingStudents.isEmpty()) {
            return;
        }
        String message = matchingStudents.size() == 1 ? MESSAGE_WARNING_SINGULAR : MESSAGE_WARNING_PLURAL;
        warnings.add(String.format(message, formatStudentNames(matchingStudents),
                fieldName.toLowerCase(Locale.ROOT)));
    }

    private static String formatStudentNames(List<Student> matchingStudents) {
        String firstName = matchingStudents.get(0).getName().toString();
        if (matchingStudents.size() == 1) {
            return firstName;
        }
        int remainingStudents = matchingStudents.size() - MAX_NAMED_STUDENTS;
        if (remainingStudents == 0) {
            return firstName + " and " + matchingStudents.get(1).getName();
        }
        String firstTwoNames = firstName + ", " + matchingStudents.get(1).getName();
        return firstTwoNames + " and " + remainingStudents + " other"
                + StringUtil.getPluralSuffix(remainingStudents);
    }
}
