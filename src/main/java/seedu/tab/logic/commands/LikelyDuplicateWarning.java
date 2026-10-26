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

/** Formats non-blocking warnings about student records with matching details. */
final class LikelyDuplicateWarning {
    static final String MESSAGE_WARNING_SINGULAR =
            "WARNING: Possible duplicate student: %1$s has the same %2$s%3$s.";
    static final String MESSAGE_WARNING_PLURAL =
            "WARNING: Possible duplicate students: %1$s have the same %2$s%3$s.";
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

    private static String appendWarnings(String successMessage, Student changedStudent,
            Iterable<Student> existingStudents, Student studentToExclude) {
        requireNonNull(successMessage);
        requireNonNull(changedStudent);
        requireNonNull(existingStudents);

        List<Student> matchingNames = new ArrayList<>();
        List<Student> matchingPhones = new ArrayList<>();
        List<Student> matchingEmails = new ArrayList<>();
        for (Student existingStudent : existingStudents) {
            if (existingStudent == studentToExclude) {
                continue;
            }
            addMatches(matchingNames, matchingPhones, matchingEmails,
                    changedStudent, existingStudent, studentToExclude);
        }
        List<String> warnings = formatWarnings(changedStudent, matchingNames, matchingPhones, matchingEmails);
        return warnings.isEmpty()
                ? successMessage
                : successMessage + "\n\n" + String.join("\n", warnings) + "\n" + MESSAGE_ADVICE;
    }

    private static void addMatches(List<Student> matchingNames, List<Student> matchingPhones,
            List<Student> matchingEmails, Student changedStudent, Student existingStudent, Student originalStudent) {
        if (isChanged(changedStudent.getName(), originalStudent == null ? null : originalStudent.getName())
                && StringUtil.normalizeNameForComparison(changedStudent.getName().fullName)
                        .equals(StringUtil.normalizeNameForComparison(existingStudent.getName().fullName))) {
            matchingNames.add(existingStudent);
        }
        if (isChanged(changedStudent.getPhone(), originalStudent == null ? null : originalStudent.getPhone())
                && phonesMatch(changedStudent, existingStudent)) {
            matchingPhones.add(existingStudent);
        }
        if (isChanged(changedStudent.getEmail(), originalStudent == null ? null : originalStudent.getEmail())
                && emailsMatch(changedStudent, existingStudent)) {
            matchingEmails.add(existingStudent);
        }
    }

    private static List<String> formatWarnings(Student changedStudent, List<Student> matchingNames,
            List<Student> matchingPhones, List<Student> matchingEmails) {
        List<String> warnings = new ArrayList<>();
        addWarning(warnings, matchingNames, Name.FIELD_NAME, changedStudent.getName().fullName);
        changedStudent.getPhone().ifPresent(phone ->
                addWarning(warnings, matchingPhones, Phone.FIELD_NAME, phone.value));
        changedStudent.getEmail().ifPresent(email ->
                addWarning(warnings, matchingEmails, Email.FIELD_NAME, email.value));
        return warnings;
    }

    private static boolean isChanged(Object changedValue, Object originalValue) {
        return originalValue == null || !changedValue.equals(originalValue);
    }

    private static boolean phonesMatch(Student first, Student second) {
        return first.getPhone().flatMap(phone -> second.getPhone()
                .filter(otherPhone -> phone.getDigits().equals(otherPhone.getDigits()))).isPresent();
    }

    private static boolean emailsMatch(Student first, Student second) {
        return first.getEmail().flatMap(email -> second.getEmail()
                .filter(otherEmail -> matchesIgnoringCase(email.value, otherEmail.value))).isPresent();
    }

    private static boolean matchesIgnoringCase(String first, String second) {
        return first.toLowerCase(Locale.ROOT).equals(second.toLowerCase(Locale.ROOT));
    }

    private static void addWarning(List<String> warnings, List<Student> matchingStudents,
            String fieldName, String value) {
        if (matchingStudents.isEmpty()) {
            return;
        }
        String message = matchingStudents.size() == 1 ? MESSAGE_WARNING_SINGULAR : MESSAGE_WARNING_PLURAL;
        // Matching phone digits can have different formatting in every named record.
        String detail = fieldName.equals(Phone.FIELD_NAME) ? "" : " (" + value + ")";
        warnings.add(String.format(message, formatStudentNames(matchingStudents),
                fieldName.toLowerCase(Locale.ROOT), detail));
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
                + (remainingStudents == 1 ? "" : "s");
    }
}
