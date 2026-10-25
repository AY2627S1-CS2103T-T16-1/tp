package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.tab.model.Model.PREDICATE_SHOW_ALL_STUDENTS;

import java.util.Locale;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.model.Model;

/**
 * Lists all students in the student book to the user.
 */
public class ListCommand extends Command {

    public static final String COMMAND_WORD = "list";

    public static final String MESSAGE_SUCCESS = "Listed %1$d student%2$s.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredStudentList(PREDICATE_SHOW_ALL_STUDENTS);
        return new CommandResult(formatSuccessMessage(model.getFilteredStudentList().size()));
    }

    /**
     * Returns the success message for the given number of displayed students.
     *
     * @param count The number of displayed students.
     * @return The formatted success message.
     */
    public static String formatSuccessMessage(int count) {
        return String.format(Locale.ROOT, MESSAGE_SUCCESS, count, StringUtil.getPluralSuffix(count));
    }
}
