package seedu.tab.logic;

import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NO_FOLLOW_UP;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.commons.util.ValueMessages;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND =
            "Unknown command \"%1$s\". Enter help to see every command.";
    public static final String MESSAGE_MISTYPED_COMMAND = "Unknown command \"%1$s\". Did you mean %2$s?";
    public static final String MESSAGE_UNEXPECTED_ARGUMENTS =
            "%1$s takes no arguments. Unexpected input: \"%2$s\".";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format!\n%1$s";
    public static final String MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX =
            "There is no student at index \"%1$s\" in the displayed list (%2$d student%3$s). "
                    + "Use list to show all students.";
    public static final String MESSAGE_NO_STUDENT_WITH_NUS_ID = "No student has NUS Student ID %1$s.";
    // Keep this template free of numeric conversions: callers format it without an explicit locale.
    public static final String MESSAGE_DUPLICATE_STUDENT =
            "Another student already has NUS Student ID %1$s (%2$s).";
    public static final String MESSAGE_STUDENTS_FOUND_OVERVIEW = "Found %1$d student%2$s.";
    public static final String MESSAGE_DUPLICATE_OPTIONS = "Options given more than once: ";
    public static final String MESSAGE_MISSING_FIELDS = "Missing required field(s): %1$s";
    public static final String MESSAGE_UNKNOWN_FLAG = "There is no %1$s option.";
    public static final String MESSAGE_FLAG_TAKES_NO_VALUE =
            "%1$s takes no value. Use the option on its own.";
    public static final String MESSAGE_FLAG_WITHOUT_VALUE =
            "%1$s needs a value after it. A value opening with a hyphen goes in double quotes.";
    public static final String MESSAGE_VALUE_AFTER_FLAGS =
            "\"%1$s\" does not belong to any option. Options that take a value take one token, "
            + "and a value holding spaces goes in double quotes.";
    public static final String MESSAGE_PREAMBLE_BEFORE_OPTIONS =
            "If the command takes a name or index without an option, put it before all options.";
    public static final String MESSAGE_CONFLICTING_FOLLOW_UP = "Options " + FLAG_FOLLOW_UP.getMarkerWithAliases()
            + " and " + FLAG_NO_FOLLOW_UP.getMarkerWithAliases() + " cannot be used together. Choose one.";

    private Messages() {} // this class only holds the messages

    /** Returns the rejected index and the size of the list it addresses. */
    public static String getErrorMessageForInvalidIndex(String index, int displayedCount) {
        return String.format(Locale.ROOT, MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX, shorten(index),
                displayedCount, StringUtil.getPluralSuffix(displayedCount));
    }

    /**
     * Returns the explicit ID form when a target may have been intended as an ID. The command
     * supplies {@code idTargetCommand} itself, because the option that names an ID target
     * differs between commands: {@code edit} spells it {@code --target-id}, its {@code --id}
     * being the field it sets.
     */
    public static String getIdTargetHint(String idTargetCommand, String value) {
        return "If \"" + value + "\" is an NUS Student ID, use " + idTargetCommand + " " + value + ".";
    }

    /**
     * Returns an error message indicating the flags that were given more than once.
     */
    public static String getErrorMessageForDuplicateFlags(String... duplicateFlags) {
        assert duplicateFlags.length > 0;

        String duplicateOptions = Stream.of(duplicateFlags)
                .distinct()
                .collect(Collectors.joining(" "));

        return MESSAGE_DUPLICATE_OPTIONS + duplicateOptions + ". Give each listed option only once.";
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
     * Returns an error message naming the value that follows the options and belongs to none
     * of them. The placement hint is offered only when nothing precedes the options, because a
     * command that already read a value there refuses a second one, so moving the value to the
     * front would earn the user another refusal.
     */
    public static String getErrorMessageForValueAfterFlags(String value, boolean hasPreamble) {
        String message = String.format(MESSAGE_VALUE_AFTER_FLAGS, value);
        return hasPreamble ? message : message + " " + MESSAGE_PREAMBLE_BEFORE_OPTIONS;
    }

    /**
     * Returns an error message quoting the value that was rejected and saying what is wrong
     * with it, rather than restating the whole rule for the field. A value too long to read is
     * shortened, so that the reason stays visible rather than being pushed out of sight.
     */
    public static String getErrorMessageForInvalidValue(String fieldName, String value, String reason) {
        return ValueMessages.getErrorMessageForInvalidValue(fieldName, value, reason);
    }

    /**
     * Returns feedback naming the word that is no command of TAB's, and where to find the ones
     * that are. A word too long to read is shortened, so that the pointer stays in sight.
     */
    public static String getErrorMessageForUnknownCommand(String commandWord) {
        return String.format(MESSAGE_UNKNOWN_COMMAND, shorten(commandWord));
    }

    /**
     * Returns feedback naming the word that is no command of TAB's and the commands it was
     * likely meant to be. Every equally likely command is named, because choosing between
     * them is the User's to make. They are shown rather than run, so that a wrong reading
     * costs the User nothing but the retyping they were facing anyway.
     */
    public static String getErrorMessageForMistypedCommand(String commandWord, List<String> suggestions) {
        assert !suggestions.isEmpty();

        return String.format(MESSAGE_MISTYPED_COMMAND, shorten(commandWord), listAlternatives(suggestions));
    }

    /**
     * Returns {@code alternatives} as a sentence reads them: "list", "find or list", or
     * "edit, find or list".
     */
    private static String listAlternatives(List<String> alternatives) {
        int lastIndex = alternatives.size() - 1;
        if (lastIndex == 0) {
            return alternatives.get(0);
        }
        return String.join(", ", alternatives.subList(0, lastIndex)) + " or " + alternatives.get(lastIndex);
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
        return String.format(Locale.ROOT, MESSAGE_STUDENTS_FOUND_OVERVIEW, count, StringUtil.getPluralSuffix(count));
    }

    /**
     * Returns {@code value} itself, or its opening characters followed by an ellipsis when it is
     * longer than a message can show. The length is not reported here, because the field that
     * rejected the value says whatever its own rule makes relevant.
     */
    public static String shorten(String value) {
        return ValueMessages.shorten(value);
    }

    /**
     * Formats the {@code student} for display to the user.
     */
    public static String format(Student student) {
        final StringBuilder builder = new StringBuilder();
        builder.append(student.getName());
        student.getNusStudentId().ifPresent(nusStudentId -> builder.append("; NUS Student ID: ").append(nusStudentId));
        student.getPhone().ifPresent(phone -> builder.append("; Phone: ").append(phone));
        student.getEmail().ifPresent(email -> builder.append("; Email: ").append(email));
        if (!student.getTags().isEmpty()) {
            builder.append("; Tags: ");
            student.getTags().stream()
                    .sorted(Tag.DISPLAY_ORDER)
                    .forEach(builder::append);
        }
        student.getFlag().ifPresent(flag -> builder.append("; ").append(flag.getTagName()));
        return builder.toString();
    }

}
