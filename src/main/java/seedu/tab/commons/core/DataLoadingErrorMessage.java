package seedu.tab.commons.core;

import static java.util.Objects.requireNonNull;

/**
 * A failure to load saved data as the reader of it sees it: an explanation, and the excerpt of
 * the data file the explanation points at where there is one.
 *
 * <p>The two are held apart because they are read differently. The explanation is prose, while
 * the excerpt is a listing whose line numbers and separators only line up where every character
 * is the same width.
 *
 * @param description The explanation of the failure.
 * @param sourceExcerpt The quoted part of the data file, empty where the failure names none.
 */
public record DataLoadingErrorMessage(String description, String sourceExcerpt) {

    /** Introduces the excerpt wherever the message is read as one block of text. */
    public static final String SOURCE_EXCERPT_CAPTION = "Affected source:";

    /**
     * Creates a message of an explanation and the part of the data file it points at.
     */
    public DataLoadingErrorMessage {
        requireNonNull(description);
        requireNonNull(sourceExcerpt);
    }

    /**
     * Returns a message that quotes no part of the data file.
     */
    public static DataLoadingErrorMessage of(String description) {
        return new DataLoadingErrorMessage(description, "");
    }

    /**
     * Returns this message with {@code description} in place of its explanation, so that a
     * caller that knows more about the failure can say so without losing the excerpt.
     */
    public DataLoadingErrorMessage withDescription(String description) {
        return new DataLoadingErrorMessage(description, sourceExcerpt);
    }

    public boolean hasSourceExcerpt() {
        return !sourceExcerpt.isEmpty();
    }

    /**
     * Returns the whole message as one block of text, for the log and for callers that have
     * nowhere to show the excerpt of its own.
     */
    public String toDisplayText() {
        if (!hasSourceExcerpt()) {
            return description;
        }
        return description + "\n" + SOURCE_EXCERPT_CAPTION + "\n" + sourceExcerpt;
    }
}
