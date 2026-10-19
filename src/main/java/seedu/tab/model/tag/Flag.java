package seedu.tab.model.tag;

import java.util.Arrays;

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
     * Returns true if the keyword names this flag: the whole label {@code Needs follow-up}
     * or one of its words, ignoring case. A fragment such as {@code low} never matches,
     * so a search for a surname cannot list every flagged student.
     */
    public boolean matchesKeyword(String keyword) {
        return FOLLOW_UP_NAME.equalsIgnoreCase(keyword)
                || Arrays.stream(FOLLOW_UP_NAME.split("\\s+")).anyMatch(keyword::equalsIgnoreCase);
    }
}
