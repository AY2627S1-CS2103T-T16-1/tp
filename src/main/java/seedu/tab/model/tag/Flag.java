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
     * Returns true if the keyword names this flag: the whole label {@code Needs follow-up},
     * or the opening letters of one of its words, ignoring case. A fragment from the middle
     * of a word, such as {@code low}, never matches, so a search for a surname cannot list
     * every flagged student, while {@code fol} still finds the flagged students a TA
     * remembering only "follow" would look for.
     */
    public boolean matchesKeyword(String keyword) {
        if (keyword.isEmpty()) {
            return false;
        }
        if (FOLLOW_UP_NAME.equalsIgnoreCase(keyword)) {
            return true;
        }
        return Arrays.stream(FOLLOW_UP_NAME.split("\\s+"))
                .anyMatch(word -> word.regionMatches(true, 0, keyword, 0, keyword.length()));
    }
}
