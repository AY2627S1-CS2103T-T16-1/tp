package seedu.tab.logic;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command.";
    public static final String MESSAGE_UNEXPECTED_ARGUMENTS =
            "%1$s takes no arguments. Unexpected input: \"%2$s\".";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format!\n%1$s";
    public static final String MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX = "The student index provided is invalid.";
    public static final String MESSAGE_STUDENTS_FOUND_OVERVIEW = "Found %1$d student%2$s.";
    public static final String MESSAGE_DUPLICATE_FIELDS =
                "Multiple values specified for the following single-valued field(s): ";
    public static final String MESSAGE_MISSING_FIELDS = "Missing required field(s): %1$s";
    public static final String MESSAGE_INVALID_VALUE = "%1$s \"%2$s\" is not valid: %3$s";
    /** How much of a rejected value is quoted back, so that a long one cannot hide the reason. */
    public static final int MAX_QUOTED_VALUE_LENGTH = 60;
    public static final String MESSAGE_UNKNOWN_FLAG = "There is no %1$s option.";
    public static final String MESSAGE_FLAG_WITHOUT_VALUE =
            "%1$s needs a value after it. A value opening with a hyphen goes in double quotes.";
    public static final String MESSAGE_VALUE_AFTER_FLAGS =
            "\"%1$s\" does not belong to any option. Only the first value may be given without "
            + "an option. Options that take a value take one token, and a value holding spaces "
            + "goes in double quotes.";

    /**
     * Returns an error message indicating the flags that were given more than once.
     */
    public static String getErrorMessageForDuplicateFlags(String... duplicateFlags) {
        assert duplicateFlags.length > 0;

        String duplicateFields = Stream.of(duplicateFlags)
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
     * Returns an error message quoting the value that was rejected and saying what is wrong
     * with it, rather than restating the whole rule for the field. A value too long to read is
     * shortened, so that the reason stays visible rather than being pushed out of sight.
     */
    public static String getErrorMessageForInvalidValue(String fieldName, String value, String reason) {
        return String.format(MESSAGE_INVALID_VALUE, fieldName, shorten(value), reason);
    }

    /**
     * Returns feedback naming the command and unexpected input, shortening long input for readability.
     */
    public static String getErrorMessageForUnexpectedArguments(String commandWord, String arguments) {
        return String.format(MESSAGE_UNEXPECTED_ARGUMENTS, commandWord, shorten(arguments));
    }

    /**
     * Returns a summary message reporting the number of students found.
     *
     * @param count The number of students found.
     * @return The formatted summary message.
     */
    public static String getStudentsFoundOverview(int count) {
        return String.format(MESSAGE_STUDENTS_FOUND_OVERVIEW, count, StringUtil.getPluralSuffix(count));
    }

    /**
     * Returns {@code value} itself, or its opening characters followed by an ellipsis when it is
     * longer than a message can show. The length is not reported here, because the field that
     * rejected the value says whatever its own rule makes relevant.
     */
    public static String shorten(String value) {
        if (value.codePointCount(0, value.length()) <= MAX_QUOTED_VALUE_LENGTH) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, MAX_QUOTED_VALUE_LENGTH);
        return value.substring(0, endIndex) + "…";
    }

    /**
     * Formats the {@code student} for display to the user.
     */
    public static String format(Student student) {
        final StringBuilder builder = new StringBuilder();
        builder.append(student.getName());
        student.getNusStudentId().ifPresent(nusStudentId -> builder.append("; NUS Student ID: ").append(nusStudentId));
        builder.append("; Phone: ")
                .append(student.getPhone());
        student.getEmail().ifPresent(email -> builder.append("; Email: ").append(email));
        if (!student.getTags().isEmpty()) {
            builder.append("; Tags: ");
            student.getTags().stream()
                    .sorted(Comparator.<Tag, String>comparing(tag -> tag.tagName, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(tag -> tag.tagName))
                    .forEach(builder::append);
        }
        student.getFlag().ifPresent(flag -> builder.append("; ").append(flag.tagName));
        return builder.toString();
    }

}
