package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;

import seedu.tab.commons.core.index.Index;
import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.model.Model;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Student;

/**
 * The address of a single student as a command reads it: either a position in the displayed
 * student list, or an NUS Student ID held anywhere in the book. The parser builds a target
 * from what the User typed, and the command resolves it when it runs, so an NUS Student ID
 * names its student even while a search is hiding them.
 */
public final class StudentTarget {

    private final Index index;
    private final NusStudentId nusStudentId;
    private final String indexInput;

    private StudentTarget(Index index, NusStudentId nusStudentId, String indexInput) {
        this.index = index;
        this.nusStudentId = nusStudentId;
        this.indexInput = indexInput;
    }

    /**
     * Returns a target naming the student shown at {@code index} in the displayed student list.
     */
    public static StudentTarget byIndex(Index index) {
        requireNonNull(index);
        return byIndex(index, String.valueOf(index.getOneBased()));
    }

    /** Returns an index target retaining its input spelling for diagnostics and ID recovery. */
    public static StudentTarget byIndex(Index index, String input) {
        requireNonNull(index);
        requireNonNull(input);
        return new StudentTarget(index, null, input);
    }

    /**
     * Returns a target naming the student who holds {@code nusStudentId} in the student book.
     */
    public static StudentTarget byNusStudentId(NusStudentId nusStudentId) {
        requireNonNull(nusStudentId);
        return new StudentTarget(null, nusStudentId, null);
    }

    /**
     * Returns the student this target names: an index is looked up in the list {@code model}
     * displays, an NUS Student ID among every student the book holds. NusStudentId normalizes
     * its value, so a case variant of an ID names the same student.
     *
     * @throws CommandException if no student matches the target.
     */
    public Student resolve(Model model) throws CommandException {
        return resolve(model, null);
    }

    /** Resolves a target, optionally naming a command that supports explicit ID targeting. */
    public Student resolve(Model model, String idCommandWord) throws CommandException {
        if (index != null) {
            return resolveIndex(model, idCommandWord);
        }
        return resolveNusStudentId(model.getStudentBook().getStudentList());
    }

    /**
     * Returns how this target is named in the log. An NUS Student ID is referred to rather
     * than repeated, because a student's own details never reach the log.
     */
    public String describe() {
        if (index != null) {
            return "shown at " + index.getOneBased();
        }
        return "identified by NUS Student ID";
    }

    /**
     * Returns the student at the displayed position this target holds.
     *
     * @throws CommandException if the position is past the end of the displayed list.
     */
    private Student resolveIndex(Model model, String idCommandWord) throws CommandException {
        List<Student> displayedStudents = model.getFilteredStudentList();
        if (index.getZeroBased() >= displayedStudents.size()) {
            String message = Messages.getErrorMessageForInvalidIndex(indexInput, displayedStudents.size());
            boolean isStoredId = idCommandWord != null && model.getStudentBook().getStudentList().stream()
                    .anyMatch(student -> student.getNusStudentId()
                            .filter(id -> id.getValue().equals(indexInput)).isPresent());
            if (isStoredId) {
                message += " " + Messages.getIdTargetHint(idCommandWord, indexInput);
            }
            throw new CommandException(message);
        }
        return displayedStudents.get(index.getZeroBased());
    }

    /**
     * Returns the first student in {@code students} who holds the NUS Student ID this target
     * holds, scanning past students who hold no ID at all.
     *
     * @throws CommandException if no student holds the ID.
     */
    private Student resolveNusStudentId(List<Student> students) throws CommandException {
        for (Student student : students) {
            if (student.getNusStudentId().filter(nusStudentId::equals).isPresent()) {
                return student;
            }
        }
        throw new CommandException(String.format(Messages.MESSAGE_NO_STUDENT_WITH_NUS_ID, nusStudentId));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof StudentTarget otherStudentTarget)) {
            return false;
        }

        return Objects.equals(index, otherStudentTarget.index)
                && Objects.equals(nusStudentId, otherStudentTarget.nusStudentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, nusStudentId);
    }

    @Override
    public String toString() {
        if (index != null) {
            return new ToStringBuilder(this).add("index", index).toString();
        }
        return new ToStringBuilder(this).add("nusStudentId", nusStudentId).toString();
    }
}
