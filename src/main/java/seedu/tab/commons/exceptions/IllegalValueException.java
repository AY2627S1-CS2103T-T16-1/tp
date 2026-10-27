package seedu.tab.commons.exceptions;

/**
 * Signals that some given data does not fulfill some constraints.
 */
public class IllegalValueException extends Exception {
    /**
     * Creates a constraint failure with the supplied explanation.
     *
     * @param message should contain relevant information on the failed constraint(s).
     */
    public IllegalValueException(String message) {
        super(message);
    }

    /**
     * Creates a constraint failure with the supplied explanation.
     *
     * @param message should contain relevant information on the failed constraint(s).
     * @param cause of the main exception.
     */
    public IllegalValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
