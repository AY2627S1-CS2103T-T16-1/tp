package seedu.tab.commons.exceptions;

/**
 * Represents an error during loading of data from a file.
 */
public class DataLoadingException extends Exception {
    public DataLoadingException(Exception cause) {
        super(cause);
    }

    /**
     * Creates an exception with a user-readable explanation and its underlying cause.
     */
    public DataLoadingException(String message, Exception cause) {
        super(message, cause);
    }

}
