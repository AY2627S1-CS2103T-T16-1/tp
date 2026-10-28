package seedu.tab.model.tag;

import static java.util.Objects.requireNonNull;

import java.util.regex.Pattern;

import seedu.tab.commons.util.StringUtil;

/**
 * A tag with follow-up meaning, distinct from a user-defined tag with the same name.
 */
public class FollowUpFlag extends Tag {
    public static final String FOLLOW_UP_NAME = "Needs follow-up";

    private static final Pattern HYPHEN_OR_DASH = Pattern.compile("[\\p{Pd}\\u2212]");
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("\\p{P}+$");
    private static final String NORMALIZED_MARKER = normalizeForFollowUpComparison(FOLLOW_UP_NAME);
    private static final String COLLAPSED_MARKER = collapseForComparison(NORMALIZED_MARKER);

    /**
     * Explains why this label names no tag. Each entry point appends the remedy that suits it,
     * since a command is corrected by typing a flag and a data file by editing a field.
     */
    public static final String MESSAGE_RESERVED =
            "\"" + FOLLOW_UP_NAME + "\" marks a student for follow-up rather than naming a tag, "
                    + "so a card would show it twice.";

    /** Constructs the follow-up flag. */
    public FollowUpFlag() {
        super(FOLLOW_UP_NAME);
    }

    /**
     * Returns true if {@code name} reads as the label this flag shows.
     *
     * <p>Case and surrounding whitespace are ignored, because a card shows a tag as it shows
     * the flag and {@code tag} already treats two spellings of one tag as one tag. A spelling
     * the product would display beside the flag, and count as one tag with it, is the same
     * label to the reader whatever its case.
     *
     * <p>Unicode hyphens, dashes, and spaces are treated as equivalent, and trailing
     * punctuation is ignored, so that near spellings a reader would confuse with the marker
     * are also refused.
     */
    public static boolean isFollowUpName(String name) {
        requireNonNull(name);
        String normalizedName = normalizeForFollowUpComparison(name);
        return NORMALIZED_MARKER.equalsIgnoreCase(normalizedName)
                || COLLAPSED_MARKER.equalsIgnoreCase(collapseForComparison(normalizedName));
    }

    /**
     * Returns the label with equivalent separators and no trailing punctuation, so pasted
     * typography does not disguise the reserved marker.
     */
    private static String normalizeForFollowUpComparison(String name) {
        String normalized = StringUtil.normalizeFieldValue(name);
        normalized = HYPHEN_OR_DASH.matcher(normalized).replaceAll("-");
        normalized = normalized.replace(' ', '-');
        return TRAILING_PUNCTUATION.matcher(normalized).replaceAll("");
    }

    /**
     * Returns the label without separators, so an omitted hyphen cannot disguise the marker.
     */
    private static String collapseForComparison(String normalized) {
        return normalized.replace("-", "");
    }

}
