package seedu.tab.logic.parser.exceptions;

import seedu.tab.logic.Messages;

/**
 * Identifies an unsupported option independently of its user-facing explanation.
 */
public class UnknownFlagException extends ParseException {
    private final String marker;

    /**
     * Creates an unknown-option failure retaining the marker that was supplied.
     */
    public UnknownFlagException(String marker) {
        super(String.format(Messages.MESSAGE_UNKNOWN_FLAG, marker));
        this.marker = marker;
    }

    public String getMarker() {
        return marker;
    }
}
