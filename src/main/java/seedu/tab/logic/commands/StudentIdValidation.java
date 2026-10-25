package seedu.tab.logic.commands;

import java.util.Optional;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.student.Student;

/** Validates student IDs before add and edit commands change the model. */
final class StudentIdValidation {

    private StudentIdValidation() {
    }

    /**
     * Rejects a candidate whose student ID belongs to another record. An edit may keep
     * the ID of {@code allowedSame}; an add passes null because no record is being replaced.
     */
    static void requireNoDuplicate(Model model, Student candidate, Student allowedSame)
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
