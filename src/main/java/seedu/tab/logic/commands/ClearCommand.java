package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.tab.model.Model;
import seedu.tab.model.StudentBook;

/**
 * Requests confirmation before clearing the student book.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_CONFIRMATION = "Clear all %1$d student(s), including any hidden by a search?\n"
            + "This cannot be undone. Type yes to delete all records or no to cancel, then press Enter.";
    public static final String MESSAGE_CANCELLED = "Clear cancelled. No student records were changed.";
    public static final String MESSAGE_EMPTY = "The student book is already empty. Nothing to clear.";
    public static final String MESSAGE_INVALID_REPLY = "Please type yes to confirm or no to cancel.";
    public static final String MESSAGE_SUCCESS = "Student book has been cleared!";


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        int studentCount = model.getStudentBook().getStudentList().size();
        return new CommandResult(studentCount == 0 ? MESSAGE_EMPTY
                : String.format(MESSAGE_CONFIRMATION, studentCount));
    }

    /**
     * Clears the student book after the user explicitly confirms the request.
     */
    public CommandResult executeConfirmed(Model model) {
        requireNonNull(model);
        model.setStudentBook(new StudentBook());
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
