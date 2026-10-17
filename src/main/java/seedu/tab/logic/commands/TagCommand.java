package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.CollectionUtil.requireAllNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import seedu.tab.commons.core.index.Index;
import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * Adds one tag to a student without replacing their existing tags or other fields.
 */
public class TagCommand extends Command {

    public static final String COMMAND_WORD = "tag";
    public static final String FIELD_INDEX = "INDEX";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Adds one tag to the student at the displayed index, preserving all other fields.\n"
            + "Parameters: INDEX (a positive integer) -a TAG (or --add TAG)\n"
            + "Use double quotes for a tag containing spaces.\n"
            + "Example: " + COMMAND_WORD + " 1 -a \"Lab 3\"";
    public static final String MESSAGE_SUCCESS = "Added tag [%1$s]: %2$s";
    public static final String MESSAGE_ALREADY_PRESENT = "This student already has tag [%1$s]. No changes made.";

    private final Index targetIndex;
    private final Tag tag;

    /**
     * Creates a command to add {@code tag} to the student at {@code targetIndex}.
     */
    public TagCommand(Index targetIndex, Tag tag) {
        requireAllNonNull(targetIndex, tag);
        this.targetIndex = targetIndex;
        this.tag = tag;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Student> shownStudents = model.getFilteredStudentList();
        if (targetIndex.getZeroBased() >= shownStudents.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        }
        Student student = shownStudents.get(targetIndex.getZeroBased());
        if (student.getTags().stream().anyMatch(existing -> existing.tagName.equalsIgnoreCase(tag.tagName))) {
            throw new CommandException(String.format(MESSAGE_ALREADY_PRESENT, Messages.shorten(tag.tagName)));
        }

        Set<Tag> updatedTags = new HashSet<>(student.getTags());
        updatedTags.add(tag);
        Student updatedStudent = new Student(student.getName(), student.getPhone(), student.getEmail().orElse(null),
                student.getNusStudentId().orElse(null), updatedTags, student.isFlagged());
        model.setStudent(student, updatedStudent);
        return new CommandResult(String.format(MESSAGE_SUCCESS, Messages.shorten(tag.tagName),
                Messages.format(updatedStudent)));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof TagCommand otherCommand
                && targetIndex.equals(otherCommand.targetIndex)
                && tag.equals(otherCommand.tag);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("targetIndex", targetIndex).add("tag", tag).toString();
    }
}
