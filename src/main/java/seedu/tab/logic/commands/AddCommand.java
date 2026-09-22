package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.student.Student;

/**
 * Adds a student to the student book.
 */
public class AddCommand extends Command {

    public static final String COMMAND_WORD = "add";

    /** The name is given before any option rather than marked by one. */
    public static final String FIELD_NAME = "NAME";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds a student to the student book. "
            + "Parameters: " + FIELD_NAME + " "
            + FLAG_PHONE.getLabel() + " "
            + "[" + FLAG_EMAIL.getLabel() + "] "
            + "[" + FLAG_TAG.getLabel() + "]... "
            + "[" + FLAG_FOLLOW_UP.getLabel() + "]\n"
            + "The name comes first and may hold spaces as it is. Any option value holding "
            + "spaces, and any value opening with a hyphen, goes in double quotes. "
            + FLAG_FOLLOW_UP + " takes no value and marks the student as needing follow-up.\n"
            + "Example: " + COMMAND_WORD + " \"Siti Nur-Aisyah\" "
            + FLAG_PHONE + " 98765432 "
            + FLAG_EMAIL + " e1147203@u.nus.edu "
            + FLAG_TAG + " T1 "
            + FLAG_TAG + " \"Lab 3\"";

    public static final String MESSAGE_SUCCESS = "New student added: %1$s";
    public static final String MESSAGE_DUPLICATE_STUDENT = "This student already exists in the student book.";

    private final Student toAdd;

    /**
     * Creates an AddCommand to add the specified {@code Student}
     */
    public AddCommand(Student student) {
        requireNonNull(student);
        toAdd = student;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        if (model.hasStudent(toAdd)) {
            throw new CommandException(MESSAGE_DUPLICATE_STUDENT);
        }

        model.addStudent(toAdd);
        return new CommandResult(String.format(MESSAGE_SUCCESS, Messages.format(toAdd)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddCommand otherAddCommand)) {
            return false;
        }

        return toAdd.equals(otherAddCommand.toAdd);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("toAdd", toAdd)
                .toString();
    }
}
