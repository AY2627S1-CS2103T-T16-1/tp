package seedu.tab.model.tag;

import static java.util.Objects.requireNonNull;

import java.text.Normalizer;
import java.util.regex.Pattern;

import seedu.tab.commons.util.StringUtil;

/**
 * A tag with follow-up meaning, distinct from a user-defined tag with the same name.
 */
public class FollowUpFlag extends Tag {
    public static final String FOLLOW_UP_NAME = "Needs follow-up";

    // Unicode hyphens and dashes that look like a hyphen to a reader
    private static final Pattern HYPHEN_OR_DASH =
            Pattern.compile("[\\u002D\\u2010\\u2011\\u2012\\u2013\\u2014\\u2212]");
    // Trailing punctuation that should be ignored for comparison
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.!?]+$");

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
        String normalizedMarker = normalizeForFollowUpComparison(FOLLOW_UP_NAME);
        // Compare both the hyphenated form and the fully collapsed form
        // This catches "Needs follow-up" vs "Needs follow up" vs "Needs followup"
        return normalizedMarker.equalsIgnoreCase(normalizedName)
                || collapseForComparison(normalizedMarker).equalsIgnoreCase(collapseForComparison(normalizedName));
    }

    /**
     * Normalizes a tag name for follow-up comparison by:
     * 1. Stripping format characters and normalizing whitespace (via normalizeFieldValue)
     * 2. Mapping Unicode hyphens and dashes to ASCII hyphen
     * 3. Treating spaces as hyphens
     * 4. Removing trailing punctuation
     * 5. Normalizing to NFC form
     * 6. For comparison, also creates a fully collapsed form without any hyphens/spaces
     */
    private static String normalizeForFollowUpComparison(String name) {
        String normalized = StringUtil.normalizeFieldValue(name);
        // Map all hyphen/dash variants to ASCII hyphen
        normalized = HYPHEN_OR_DASH.matcher(normalized).replaceAll("-");
        // Treat spaces as hyphens for comparison
        normalized = normalized.replace(' ', '-');
        // Remove trailing punctuation
        normalized = TRAILING_PUNCTUATION.matcher(normalized).replaceAll("");
        return Normalizer.normalize(normalized, Normalizer.Form.NFC);
    }

    /**
     * Creates a fully collapsed form for comparison - removes all hyphens and spaces.
     * This catches variants like "Needs followup" (no separator) vs "Needs follow-up".
     */
    private static String collapseForComparison(String normalized) {
        return normalized.replace("-", "").replace(" ", "");
    }

}
