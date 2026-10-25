package seedu.tab.model.tag;

import static java.util.Objects.requireNonNull;

import seedu.tab.commons.util.StringUtil;

/**
 * A tag with follow-up meaning, distinct from a user-defined tag with the same name.
 */
public class Flag extends Tag {
    public static final String FOLLOW_UP_NAME = "Needs follow-up";

    /** Constructs the follow-up flag. */
    public Flag() {
        super(FOLLOW_UP_NAME);
    }

    /**
     * Returns true if {@code name} reads as the label this flag shows.
     *
     * <p>Case and surrounding whitespace are ignored, because a card shows a tag as it shows
     * the flag and {@code tag} already treats two spellings of one tag as one tag. A spelling
     * the product would display beside the flag, and count as one tag with it, is the same
     * label to the reader whatever its case.
     */
    public static boolean isFollowUpName(String name) {
        requireNonNull(name);
        return FOLLOW_UP_NAME.equalsIgnoreCase(StringUtil.normalizeFieldValue(name));
    }

}
