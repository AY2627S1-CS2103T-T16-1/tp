package seedu.tab.commons.exceptions;

import java.util.Objects;

import seedu.tab.commons.core.DataLoadingErrorMessage;

/**
 * Represents an error during loading of data from a file.
 */
public class DataLoadingException extends Exception {

    private final DataLoadingErrorMessage errorMessage;

    /**
     * Creates a loading failure caused by an underlying exception.
     */
    public DataLoadingException(Exception cause) {
        super(cause);
        this.errorMessage = DataLoadingErrorMessage.of(Objects.toString(super.getMessage(), ""));
    }

    /**
     * Creates an exception with a user-readable explanation and its underlying cause.
     */
    public DataLoadingException(String message, Exception cause) {
        this(DataLoadingErrorMessage.of(message), cause);
    }

    /**
     * Creates an exception whose explanation quotes the part of the file it is about, so that
     * a reader of the failure can show the quotation as the listing it is.
     */
    public DataLoadingException(DataLoadingErrorMessage errorMessage, Exception cause) {
        super(errorMessage.toDisplayText(), cause);
        this.errorMessage = errorMessage;
    }

    public DataLoadingErrorMessage getErrorMessage() {
        return errorMessage;
    }

}
