package seedu.tab.logic;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.tab.logic.parser.Flag;
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
    public static final String MESSAGE_INVALID_VALUE = "%1$s \"%2$s\" is not valid: %3$s";
    public static final String MESSAGE_UNKNOWN_FLAG = "There is no %1$s option.";
    public static final String MESSAGE_FLAG_WITHOUT_VALUE =
            "%1$s needs a value after it. A value opening with a hyphen goes in double quotes.";
    public static final String MESSAGE_VALUE_AFTER_FLAGS =
            "\"%1$s\" does not belong to any option. The name comes before the options. Options "
            + "that take a value take one token, and a value holding spaces goes in double quotes.";

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        // distinct rather than a set, so that the fields are named in the order they were given
        String duplicateFields = Stream.of(duplicatePrefixes)
                .map(Prefix::toString)
                .distinct()
                .collect(Collectors.joining(" "));

        return MESSAGE_DUPLICATE_FIELDS + duplicateFields;
    }

    /**
     * Returns an error message indicating the flags that were given more than once.
     */
    public static String getErrorMessageForDuplicateFlags(Flag... duplicateFlags) {
        assert duplicateFlags.length > 0;

        String duplicateFields = Stream.of(duplicateFlags)
                .map(Flag::toString)
                .distinct()
                .collect(Collectors.joining(" "));

        return MESSAGE_DUPLICATE_FIELDS + duplicateFields;
    }

    /**
     * Returns an error message naming the fields that the command left out. The fields are
     * given as labels rather than flags, because a command may take one without a flag.
     */
    public static String getErrorMessageForMissingFields(String... missingFields) {
        assert missingFields.length > 0;

        return String.format(MESSAGE_MISSING_FIELDS, String.join(", ", missingFields));
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
     * Returns an error message quoting the value that was rejected and saying what is wrong
     * with it, rather than restating the whole rule for the field.
     */
    public static String getErrorMessageForInvalidValue(String fieldName, String value, String reason) {
        return String.format(MESSAGE_INVALID_VALUE, fieldName, value, reason);
    }

    /**
     * Formats the {@code student} for display to the user.
     */
    public static String format(Student student) {
        final StringBuilder builder = new StringBuilder();
        builder.append(student.getName())
                .append("; Phone: ")
                .append(student.getPhone());
        student.getEmail().ifPresent(email -> builder.append("; Email: ").append(email));
        builder.append("; Tags: ");
        student.getTags().forEach(builder::append);
        if (student.isFlagged()) {
            builder.append("; Needs follow-up");
        }
        return builder.toString();
    }

}
