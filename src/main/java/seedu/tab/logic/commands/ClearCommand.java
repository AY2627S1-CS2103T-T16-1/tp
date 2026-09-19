package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.tab.model.Model;
import seedu.tab.model.StudentBook;

/**
 * Clears the student book.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_SUCCESS = "Student book has been cleared!";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.setStudentBook(new StudentBook());
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
