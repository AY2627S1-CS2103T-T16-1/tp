package seedu.tab.logic.parser.exceptions;

import seedu.tab.commons.exceptions.IllegalValueException;

/**
 * Represents a parse error encountered by a parser.
 */
public class ParseException extends IllegalValueException {

    /**
     * Creates a parse failure with an explanation for the user.
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Creates a parse failure retaining the underlying cause.
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
