package seedu.tab.model.tag;

/**
 * A tag with follow-up meaning, distinct from a user-defined tag with the same name.
 */
public class Flag extends Tag {
    public static final String FOLLOW_UP_NAME = "Needs follow-up";

    /** Constructs the follow-up flag. */
    public Flag() {
        super(FOLLOW_UP_NAME);
    }
}
