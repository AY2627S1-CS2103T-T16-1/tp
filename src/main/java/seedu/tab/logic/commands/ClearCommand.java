package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.logging.Logger;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.model.Model;
import seedu.tab.model.StudentBook;

/**
 * Requests confirmation before clearing the student book.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_CONFIRMATION = "Clear all %1$d student%2$s? This cannot be undone. [y/N]";
    public static final String MESSAGE_CANCELLED = "Clear cancelled. No student records were changed.";
    public static final String MESSAGE_EMPTY = "The student book is already empty. Nothing to clear.";
    public static final String MESSAGE_INVALID_REPLY = "Invalid reply. Enter y/yes or n/no (Enter cancels).";
    public static final String MESSAGE_NO_PENDING_CONFIRMATION =
            "No clear confirmation is pending. Enter clear to request one.";
    public static final String MESSAGE_SUCCESS = "Student book has been cleared!";

    private static final Logger logger = LogsCenter.getLogger(ClearCommand.class);


    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        int studentCount = model.getStudentBook().getStudentList().size();
        return new CommandResult(studentCount == 0 ? MESSAGE_EMPTY
                : String.format(Locale.ROOT, MESSAGE_CONFIRMATION, studentCount,
                        StringUtil.getPluralSuffix(studentCount)));
    }

    /**
     * Clears the student book after the user explicitly confirms the request.
     */
    public CommandResult executeConfirmed(Model model) {
        requireNonNull(model);
        int cleared = model.getStudentBook().getStudentList().size();
        model.setStudentBook(new StudentBook());
        logger.info("Cleared " + cleared + " student" + StringUtil.getPluralSuffix(cleared));
        return new CommandResult(MESSAGE_SUCCESS);
    }
}
