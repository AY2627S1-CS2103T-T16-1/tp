package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.tab.model.Model.PREDICATE_SHOW_ALL_STUDENTS;

import seedu.tab.model.Model;

/**
 * Lists all students in the student book to the user.
 */
public class ListCommand extends Command {

    public static final String COMMAND_WORD = "list";

    public static final String MESSAGE_SUCCESS = "Listed all %1$d students.";
    public static final String MESSAGE_SINGLE_STUDENT = "Listed 1 student.";
    public static final String MESSAGE_EMPTY_LIST = "Listed 0 students.";

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
        if (count == 0) {
            return MESSAGE_EMPTY_LIST;
        }
        if (count == 1) {
            return MESSAGE_SINGLE_STUDENT;
        }
        return String.format(MESSAGE_SUCCESS, count);
    }
}
