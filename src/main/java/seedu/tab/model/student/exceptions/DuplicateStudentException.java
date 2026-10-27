package seedu.tab.model.student.exceptions;

/**
 * Signals that the operation will result in duplicate Students (Students are considered
 * duplicates if they have the same
 * identity).
 */
public class DuplicateStudentException extends RuntimeException {
    /**
     * Creates a failure for an operation that would duplicate a student identity.
     */
    public DuplicateStudentException() {
        super("Operation would result in duplicate students");
    }
}
