package seedu.tab.logic.parser.exceptions;

import seedu.tab.logic.Messages;

/**
 * Identifies an unsupported option independently of its user-facing explanation.
 */
public class UnknownFlagException extends ParseException {
    private final String marker;

    /** Creates a failure retaining the supplied marker, with an optional supported-option suggestion. */
    public UnknownFlagException(String marker, String suggestion) {
        super(String.format(Messages.MESSAGE_UNKNOWN_FLAG, marker)
                + (suggestion == null ? "" : " Did you mean " + suggestion + "?"));
        this.marker = marker;
    }

    public String getMarker() {
        return marker;
    }
}
