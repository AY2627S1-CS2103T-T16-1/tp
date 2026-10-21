package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;

import java.util.logging.Logger;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.core.index.Index;
import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.student.Student;

/**
 * Deletes a student identified using their displayed index or their NUS Student ID.
 */
public class DeleteCommand extends Command {

    public static final String COMMAND_WORD = "delete";

    public static final String FIELD_INDEX = "INDEX";

    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Deletes the student identified by the index number used in the displayed student list,\n"
            + "or by the NUS Student ID they hold anywhere in the book.\n"
            + "Parameters: " + FIELD_INDEX + " (must be a positive integer) or NUS_STUDENT_ID\n"
            + "   or " + FLAG_NUS_STUDENT_ID.getMarkerSummary()
            + " NUS_STUDENT_ID, which reads an ID starting with a digit as an ID\n"
            + "Example: " + COMMAND_WORD + " 1\n"
            + "Example: " + COMMAND_WORD + " A0123456X";

    public static final String MESSAGE_DELETE_STUDENT_SUCCESS = "Deleted student: %1$s";

    private final Logger logger = LogsCenter.getLogger(DeleteCommand.class);

    private final StudentTarget target;

    public DeleteCommand(Index targetIndex) {
        this(StudentTarget.byIndex(targetIndex));
    }

    public DeleteCommand(StudentTarget target) {
        this.target = target;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Student studentToDelete = target.resolve(model);
        model.deleteStudent(studentToDelete);
        logger.info("Deleted the student " + target.describe()
                + "; the book now holds " + model.getStudentBook().getStudentList().size());
        return new CommandResult(String.format(MESSAGE_DELETE_STUDENT_SUCCESS, Messages.format(studentToDelete)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof DeleteCommand otherDeleteCommand)) {
            return false;
        }

        return target.equals(otherDeleteCommand.target);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("target", target)
                .toString();
    }
}
