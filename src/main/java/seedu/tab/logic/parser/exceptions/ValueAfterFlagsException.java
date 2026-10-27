package seedu.tab.logic.parser.exceptions;

/**
 * Identifies a value that belongs to none of the options it follows, independently of its
 * user-facing explanation, so that a command holding a reply that fits its own grammar better
 * can substitute that reply for the shared one.
 */
public class ValueAfterFlagsException extends ParseException {
    /**
     * Creates a value-after-options failure carrying the shared explanation {@code message}.
     */
    public ValueAfterFlagsException(String message) {
        super(message);
    }
}
