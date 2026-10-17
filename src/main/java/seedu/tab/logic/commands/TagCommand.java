package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.tab.logic.parser.CliFlags.FLAG_ADD_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_REMOVE_TAG;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import seedu.tab.commons.core.index.Index;
import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * Adds or removes one tag without replacing the student's other tags or fields.
 */
public class TagCommand extends Command {

    /** The change to make to the student's tags. */
    public enum Action { ADD, REMOVE }

    public static final String COMMAND_WORD = "tag";
    public static final String FIELD_INDEX = "INDEX";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Adds or removes one tag at the displayed index, preserving all other fields.\n"
            + "Parameters: " + FIELD_INDEX + " (a positive integer) " + FLAG_ADD_TAG.getLabel()
            + " or " + FLAG_REMOVE_TAG.getLabel() + "\n"
            + "Use double quotes for a tag containing spaces. Accepted options: "
            + FLAG_ADD_TAG.getMarkerSummary() + " or " + FLAG_REMOVE_TAG.getMarkerSummary() + ".\n"
            + "Example: " + COMMAND_WORD + " 1 " + FLAG_ADD_TAG + " \"Lab 3\"";
    public static final String MESSAGE_ADD_SUCCESS = "Added tag [%1$s]: %2$s";
    public static final String MESSAGE_ALREADY_PRESENT = "This student already has tag [%1$s]. No changes made.";
    public static final String MESSAGE_REMOVE_SUCCESS = "Removed tag [%1$s]: %2$s";
    public static final String MESSAGE_NOT_PRESENT = "This student does not have tag [%1$s]. No changes made.";
    public static final String MESSAGE_CONFLICTING_ACTIONS = "Options " + FLAG_ADD_TAG.getMarkerSummary()
            + " and " + FLAG_REMOVE_TAG.getMarkerSummary() + " cannot be used together.";
    public static final String MESSAGE_ACTION_GUIDANCE =
            "To add a tag, use " + String.join(" or ", FLAG_ADD_TAG.getMarkers())
                    + ", e.g. " + COMMAND_WORD + " 1 " + FLAG_ADD_TAG + " \"Lab 3\"."
                    + " To remove a tag, use " + String.join(" or ", FLAG_REMOVE_TAG.getMarkers())
                    + ", e.g. " + COMMAND_WORD + " 1 " + FLAG_REMOVE_TAG + " \"Lab 3\".";

    private final Index targetIndex;
    private final Tag tag;
    private final Action action;

    /**
     * Creates a command to add {@code tag} to the student at {@code targetIndex}.
     */
    public TagCommand(Index targetIndex, Tag tag) {
        this(targetIndex, tag, Action.ADD);
    }

    /**
     * Creates a command to perform {@code action} on {@code tag} at {@code targetIndex}.
     */
    public TagCommand(Index targetIndex, Tag tag, Action action) {
        requireAllNonNull(targetIndex, tag, action);
        this.targetIndex = targetIndex;
        this.tag = tag;
        this.action = action;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Student> shownStudents = model.getFilteredStudentList();
        if (targetIndex.getZeroBased() >= shownStudents.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        }
        Student student = shownStudents.get(targetIndex.getZeroBased());
        Optional<Tag> existingTag = student.getTags().stream()
                .filter(this::matchesTag)
                .min(Comparator.comparing(existing -> existing.tagName));
        if (action == Action.ADD && existingTag.isPresent()) {
            throw new CommandException(String.format(MESSAGE_ALREADY_PRESENT,
                    Messages.shorten(existingTag.get().tagName)));
        }
        if (action == Action.REMOVE && existingTag.isEmpty()) {
            throw new CommandException(String.format(MESSAGE_NOT_PRESENT, Messages.shorten(tag.tagName)));
        }
        Student updatedStudent = createUpdatedStudent(student);
        model.setStudent(student, updatedStudent);
        String message = action == Action.ADD ? MESSAGE_ADD_SUCCESS : MESSAGE_REMOVE_SUCCESS;
        Tag changedTag = action == Action.ADD ? tag : existingTag.orElseThrow();
        return new CommandResult(String.format(message, Messages.shorten(changedTag.tagName),
                Messages.format(updatedStudent)));
    }

    /** Returns whether a stored tag denotes the requested tag, ignoring case. */
    private boolean matchesTag(Tag existing) {
        return existing.tagName.equalsIgnoreCase(tag.tagName);
    }

    /** Returns a replacement student with only the requested tag changed. */
    private Student createUpdatedStudent(Student student) {
        Set<Tag> updatedTags = new HashSet<>(student.getTags());
        if (action == Action.ADD) {
            updatedTags.add(tag);
        } else {
            updatedTags.removeIf(this::matchesTag);
        }
        return new Student(student.getName(), student.getPhone(), student.getEmail().orElse(null),
                student.getNusStudentId().orElse(null), updatedTags, student.isFlagged());
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof TagCommand otherCommand
                && targetIndex.equals(otherCommand.targetIndex)
                && tag.equals(otherCommand.tag)
                && action == otherCommand.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetIndex, tag, action);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("targetIndex", targetIndex).add("tag", tag)
                .add("action", action).toString();
    }
}
