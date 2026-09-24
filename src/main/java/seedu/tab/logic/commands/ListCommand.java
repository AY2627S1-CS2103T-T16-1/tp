package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.tab.model.Model.PREDICATE_SHOW_ALL_STUDENTS;

import seedu.tab.model.Model;

/**
 * Lists all students in the student book to the user.
 */
public class ListCommand extends Command {

    public static final String COMMAND_WORD = "list";

    public static final String MESSAGE_SUCCESS = "Listed all students.";
    public static final String MESSAGE_EMPTY_LIST = "There is no student in your list.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredStudentList(PREDICATE_SHOW_ALL_STUDENTS);
        if (model.getFilteredStudentList().isEmpty()) {
            return new CommandResult(MESSAGE_EMPTY_LIST);
        }
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
