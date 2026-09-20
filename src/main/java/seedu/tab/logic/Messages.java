package seedu.tab.logic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.tab.logic.parser.Prefix;
import seedu.tab.model.student.Student;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command.";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format!\n%1$s";
    public static final String MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX = "The student index provided is invalid.";
    public static final String MESSAGE_STUDENTS_LISTED_OVERVIEW = "%1$d student(s) listed!";
    public static final String MESSAGE_DUPLICATE_FIELDS =
                "Multiple values specified for the following single-valued field(s): ";
    public static final String MESSAGE_MISSING_FIELDS = "Missing required field(s): %1$s";

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        Set<String> duplicateFields =
                Stream.of(duplicatePrefixes).map(Prefix::toString).collect(Collectors.toSet());

        return MESSAGE_DUPLICATE_FIELDS + String.join(" ", duplicateFields);
    }

    /**
     * Returns an error message naming the fields that the command left out.
     */
    public static String getErrorMessageForMissingPrefixes(Prefix... missingPrefixes) {
        assert missingPrefixes.length > 0;

        String missingFields = Stream.of(missingPrefixes)
                .map(Prefix::getLabel)
                .collect(Collectors.joining(", "));

        return String.format(MESSAGE_MISSING_FIELDS, missingFields);
    }

    /**
     * Formats the {@code student} for display to the user.
     */
    public static String format(Student student) {
        final StringBuilder builder = new StringBuilder();
        builder.append(student.getName())
                .append("; Phone: ")
                .append(student.getPhone())
                .append("; Email: ")
                .append(student.getEmail())
                .append("; Address: ")
                .append(student.getAddress())
                .append("; Tags: ");
        student.getTags().forEach(builder::append);
        return builder.toString();
    }

}
