package seedu.tab.commons.util;

/**
 * Formats bounded invalid-value diagnostics shared by commands and persisted-data validation.
 */
public final class ValueMessages {
    public static final int MAX_QUOTED_VALUE_LENGTH = 60;
    public static final String MESSAGE_INVALID_VALUE = "%1$s \"%2$s\" is not valid: %3$s";

    private ValueMessages() {}

    /**
     * Returns a diagnostic naming the field, a bounded value and the rejection reason.
     */
    public static String getErrorMessageForInvalidValue(String fieldName, String value, String reason) {
        return String.format(MESSAGE_INVALID_VALUE, fieldName, shorten(value), reason);
    }

    /**
     * Returns a bounded prefix followed by an ellipsis, or the complete value when it fits.
     */
    public static String shorten(String value) {
        String shown = StringUtil.getFirstCharacters(value, MAX_QUOTED_VALUE_LENGTH);
        return shown.equals(value) ? value : shown + "…";
    }
}
