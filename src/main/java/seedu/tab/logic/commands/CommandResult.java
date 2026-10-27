package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.model.student.Student;

/**
 * Represents the result of a command execution.
 */
public class CommandResult {

    private final String feedbackToUser;

    /** Help information should be shown to the user. */
    private final boolean isHelpRequested;

    /** The application should exit. */
    private final boolean isExitRequested;

    private final Student studentToReveal;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean isHelpRequested, boolean isExitRequested) {
        this(feedbackToUser, isHelpRequested, isExitRequested, null);
    }

    private CommandResult(String feedbackToUser, boolean isHelpRequested, boolean isExitRequested,
            Student studentToReveal) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.isHelpRequested = isHelpRequested;
        this.isExitRequested = isExitRequested;
        this.studentToReveal = studentToReveal;
    }

    /**
     * Constructs a {@code CommandResult} with the specified {@code feedbackToUser},
     * and other fields set to their default value.
     */
    public CommandResult(String feedbackToUser) {
        this(feedbackToUser, false, false);
    }

    /**
     * Constructs a result that asks the UI to reveal a successfully added student.
     */
    public CommandResult(String feedbackToUser, Student studentToReveal) {
        this(feedbackToUser, false, false, requireNonNull(studentToReveal));
    }

    public Optional<Student> getStudentToReveal() {
        return Optional.ofNullable(studentToReveal);
    }

    public String getFeedbackToUser() {
        return feedbackToUser;
    }

    public boolean isHelpRequested() {
        return isHelpRequested;
    }

    public boolean isExitRequested() {
        return isExitRequested;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof CommandResult otherCommandResult)) {
            return false;
        }

        return feedbackToUser.equals(otherCommandResult.feedbackToUser)
                && isHelpRequested == otherCommandResult.isHelpRequested
                && isExitRequested == otherCommandResult.isExitRequested
                && Objects.equals(studentToReveal, otherCommandResult.studentToReveal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, isHelpRequested, isExitRequested, studentToReveal);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("isHelpRequested", isHelpRequested)
                .add("isExitRequested", isExitRequested)
                .add("studentToReveal", studentToReveal)
                .toString();
    }

}
