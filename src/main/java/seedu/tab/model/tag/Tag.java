package seedu.tab.model.tag;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import java.util.Comparator;

import seedu.tab.commons.util.StringUtil;

/**
 * Represents a Tag in the student book.
 * Guarantees: immutable; name is valid as declared in {@link #isValidTagName(String)}
 */
public class Tag {

    /** How this field is named when a value of it is reported as invalid. */
    public static final String FIELD_NAME = "Tag";

    public static final String MESSAGE_CONSTRAINTS =
            "Tags should contain at least one letter or number, in any writing system";

    /**
     * Orders tags alphabetically whatever their case, and breaks a tie between names that
     * differ only in case by the name itself, so that the order is total and the same every
     * time. The command reply and the student card both list a student's tags, and the two
     * disagreeing would read as a mistake, so both take their order from here.
     */
    public static final Comparator<Tag> DISPLAY_ORDER =
            Comparator.comparing(Tag::getTagName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Tag::getTagName);

    private final String tagName;

    /**
     * Constructs a {@code Tag}.
     *
     * @param tagName A valid tag name.
     */
    public Tag(String tagName) {
        requireNonNull(tagName);
        checkArgument(isValidTagName(tagName), MESSAGE_CONSTRAINTS);
        this.tagName = StringUtil.normalizeFieldValue(tagName);
    }

    /**
     * Returns true if a given string is a valid tag name.
     */
    public static boolean isValidTagName(String test) {
        requireNonNull(test);
        return StringUtil.holdsLetterOrNumber(test);
    }

    public String getTagName() {
        return tagName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // A follow-up flag and a general tag with the same text have different meanings.
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Tag otherTag = (Tag) other;
        return tagName.equals(otherTag.tagName);
    }

    @Override
    public int hashCode() {
        return tagName.hashCode();
    }

    /**
     * Returns why {@code test} does not hold a tag. The answer is only meaningful when
     * {@link #isValidTagName(String)} rejects it.
     */
    public static String getFailureReason(String test) {
        requireNonNull(test);
        return StringUtil.REASON_NO_LETTER_OR_NUMBER;
    }

    /**
     * Formats state as text for viewing.
     */
    public String toString() {
        return '[' + tagName + ']';
    }

}
