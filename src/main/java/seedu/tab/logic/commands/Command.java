package seedu.tab.logic.commands;

import java.util.Optional;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.student.Student;

/**
 * Represents a command with hidden internal logic and the ability to be executed.
 */
public abstract class Command {

    /**
     * Executes the command and returns the result message.
     *
     * @param model {@code Model} which the command should operate on.
     * @return feedback message of the operation result for display
     * @throws CommandException If an error occurs during command execution.
     */
    public abstract CommandResult execute(Model model) throws CommandException;

    /**
     * Rejects a candidate whose student ID belongs to another record. An edit may keep
     * the ID of {@code allowedSame}; an add passes null because no record is being replaced.
     */
    protected static void requireNoDuplicate(Model model, Student candidate, Student allowedSame)
            throws CommandException {
        if (allowedSame != null && allowedSame.isSameStudent(candidate)) {
            return;
        }
        Optional<Student> duplicateStudent = model.findSameStudent(candidate);
        if (duplicateStudent.isPresent()) {
            throw new CommandException(String.format(Messages.MESSAGE_DUPLICATE_STUDENT,
                    candidate.getNusStudentId().orElseThrow(), duplicateStudent.orElseThrow().getName()));
        }
    }

}
