package seedu.tab.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import seedu.tab.model.student.Email;
import seedu.tab.model.student.Student;

/** Formats a non-blocking warning about an email address whose domain holds no dot. */
final class DotlessEmailDomainWarning {
    static final String MESSAGE_WARNING = "WARNING: Possibly incomplete email: %1$s has no dot in its domain.";
    static final String MESSAGE_ADVICE =
            "Check whether a part such as .com or .nus.edu is missing; the address is saved as entered.";

    private DotlessEmailDomainWarning() {
    }

    /**
     * Returns {@code successMessage} followed by the warning when {@code changedStudent} holds an
     * email whose domain has no dot, and {@code successMessage} alone otherwise.
     */
    static String appendTo(String successMessage, Student changedStudent) {
        return appendWarning(successMessage, changedStudent, null);
    }

    /**
     * Returns {@code successMessage} followed by the warning when an edit gives
     * {@code changedStudent} an email whose domain has no dot. An email that {@code originalStudent}
     * already holds carries no warning, so an unrelated edit does not repeat an acknowledged one.
     */
    static String appendChangedEmailTo(String successMessage, Student originalStudent, Student changedStudent) {
        requireNonNull(originalStudent);
        return appendWarning(successMessage, changedStudent, originalStudent);
    }

    private static String appendWarning(String successMessage, Student changedStudent, Student originalStudent) {
        requireNonNull(successMessage);
        requireNonNull(changedStudent);

        Optional<Email> email = changedStudent.getEmail();
        if (originalStudent != null && email.equals(originalStudent.getEmail())) {
            return successMessage;
        }
        return email.filter(Email::hasDotlessDomain)
                .map(dotlessEmail -> successMessage + "\n\n"
                        + String.format(MESSAGE_WARNING, dotlessEmail.getValue()) + "\n" + MESSAGE_ADVICE)
                .orElse(successMessage);
    }
}
