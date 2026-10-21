package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import java.util.logging.Logger;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.logic.Messages;
import seedu.tab.model.Model;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;

/**
 * Finds and lists all students in the student book whose details match every supplied search condition.
 * Keyword matching is case insensitive and allows partial matches.
 */
public class FindCommand extends Command {

    public static final String COMMAND_WORD = "find";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Finds students matching every search term "
            + "(case-insensitive partial matches). Every term needs a field option.\n"
            + "Parameters: one or more field options: "
            + FLAG_NAME.getLabel() + ", " + FLAG_NUS_STUDENT_ID.getLabel() + ", " + FLAG_PHONE.getLabel() + ", "
            + FLAG_EMAIL.getLabel() + ", " + FLAG_TAG.getLabel() + ".\n"
            + "Each option takes one word or quoted phrase; repeat an option to require another term.\n"
            + "Example: " + COMMAND_WORD + " -n Jason -t lab -t tutorial";

    private final Logger logger = LogsCenter.getLogger(FindCommand.class);

    private final DetailsContainsKeywordsPredicate predicate;

    public FindCommand(DetailsContainsKeywordsPredicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredStudentList(predicate);
        logger.fine("Find matched " + model.getFilteredStudentList().size() + " of "
                + model.getStudentBook().getStudentList().size());
        return new CommandResult(
                Messages.getStudentsFoundOverview(model.getFilteredStudentList().size()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof FindCommand otherFindCommand)) {
            return false;
        }

        return predicate.equals(otherFindCommand.predicate);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("predicate", predicate)
                .toString();
    }
}
