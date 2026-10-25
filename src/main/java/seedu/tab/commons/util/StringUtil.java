package seedu.tab.commons.util;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Helper functions for handling strings.
 */
public class StringUtil {

    // almost every Unicode format character (category Cf), except the joiner and non-joiner a
    // script needs to shape a word. A few of them do draw something — the Arabic end-of-ayah
    // sign and the characters that tag a Scotland flag — but none is likely in a class list,
    // and dropping them is the price of closing the whole category. Non-whitespace control
    // characters are removed too; whitespace controls remain available as separators.
    private static final Pattern INVISIBLE = Pattern.compile("(?U)[\\p{Cf}\\p{Cc}&&[^\\u200C\\u200D\\s]]");
    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");
    private static final Pattern HAS_LETTER_OR_NUMBER = Pattern.compile("[\\p{L}\\p{N}]");

    private StringUtil() {} // this class only holds the helpers

    /**
     * Returns true if {@code character} separates one value from the next.
     *
     * <p>The answer comes from the very pattern {@link #normalizeFieldValue} collapses with,
     * so a caller that splits on whitespace and the value it then stores cannot disagree. On
     * one character, matching one or more of them is the same as matching one. Naming the
     * characters instead would leave gaps in both directions: {@code Character.isWhitespace}
     * misses the non-breaking space that a command pasted from a web page carries, and claims
     * the file separator, which the pattern does not.
     */
    public static boolean isWhitespace(char character) {
        return WHITESPACE.matcher(String.valueOf(character)).matches();
    }

    /**
     * Returns the first {@code limit} characters of {@code value}, counted as a reader counts them
     * rather than as Java stores them, or {@code value} itself when it holds no more than
     * {@code limit} of them.
     *
     * <p>Cutting the string at an index cuts it between the two halves of an emoji, or between a
     * letter and the accent that belongs to it, and what is left is drawn as a broken glyph. A
     * name, a tag and a value quoted back as invalid may each hold either. Counting the same way
     * also keeps a value that is long in the way Java stores it, and short in the way it reads,
     * from being reported as cut when nothing was taken off it.
     *
     * @param value The value to cut.
     * @param limit The greatest number of characters to keep.
     * @return The opening characters of {@code value}, or {@code value} itself when it is already
     *         within {@code limit}.
     */
    public static String firstCharacters(String value, int limit) {
        requireNonNull(value);
        checkArgument(limit >= 0, "negative limit");

        BreakIterator characters = BreakIterator.getCharacterInstance();
        characters.setText(value);
        int end = characters.first();
        for (int counted = 0; counted < limit; counted++) {
            int next = characters.next();
            if (next == BreakIterator.DONE) {
                return value;
            }
            end = next;
        }
        return value.substring(0, end);
    }

    /**
     * Returns the last {@code limit} characters of {@code value}, counted as a reader counts them
     * rather than as Java stores them, or {@code value} itself when it holds no more than
     * {@code limit} of them.
     *
     * <p>Counting back from the end carries the same trap as counting forward from the start: the
     * index {@code limit} places from the end falls between the two halves of an emoji, or between
     * a letter and the accent that belongs to it, and what is left is drawn as a broken glyph. A
     * caller that keeps both ends of a value needs the two ends counted the same way.
     *
     * @param value The value to cut.
     * @param limit The greatest number of characters to keep.
     * @return The closing characters of {@code value}, or {@code value} itself when it is already
     *         within {@code limit}.
     */
    public static String lastCharacters(String value, int limit) {
        requireNonNull(value);
        checkArgument(limit >= 0, "negative limit");

        BreakIterator characters = BreakIterator.getCharacterInstance();
        characters.setText(value);
        int start = characters.last();
        for (int counted = 0; counted < limit; counted++) {
            int previous = characters.previous();
            if (previous == BreakIterator.DONE) {
                return value;
            }
            start = previous;
        }
        return value.substring(start);
    }

    /**
     * Returns true if {@code sentence} contains {@code keyword}, ignoring case.
     * A partial match is allowed (e.g. "Hans" contains "Han").
     *
     * @param sentence cannot be null
     * @param keyword cannot be null, cannot be empty
     */
    public static boolean containsIgnoreCase(String sentence, String keyword) {
        requireNonNull(sentence);
        requireNonNull(keyword);
        checkArgument(!keyword.isEmpty(), "empty keyword");

        return foldCase(sentence).contains(foldCase(keyword));
    }

    /**
     * Returns a comparison key for a name, ignoring case, Latin accents and curly apostrophes.
     * The stored name is left unchanged. Marks belonging to other scripts are retained because
     * they may represent separate vowels rather than optional accents.
     */
    public static String normalizeNameForComparison(String name) {
        requireNonNull(name);
        String decomposed = Normalizer.normalize(name, Normalizer.Form.NFD);
        StringBuilder result = new StringBuilder();
        boolean isLatin = false;
        for (int codePoint : decomposed.codePoints().toArray()) {
            int type = Character.getType(codePoint);
            boolean isMark = type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                    || type == Character.ENCLOSING_MARK;
            if (!isMark) {
                isLatin = Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN;
            }
            if (!isMark || !isLatin) {
                result.appendCodePoint(codePoint == '’' || codePoint == '‘' ? '\'' : codePoint);
            }
        }
        return foldCase(result.toString());
    }

    private static String foldCase(String value) {
        // Fold each code point independently: whole-word lowercasing turns final Σ into ς,
        // while an isolated Σ query becomes σ. This also follows equalsIgnoreCase for tags.
        StringBuilder result = new StringBuilder();
        value.codePoints().forEach(codePoint ->
                result.appendCodePoint(Character.toLowerCase(Character.toUpperCase(codePoint))));
        return result.toString();
    }

    /**
     * Returns how many single-character edits turn {@code source} into {@code target}, counting
     * an insertion, a deletion, a substitution and a swap of two neighbouring characters as one
     * edit each. Comparison is by character, so case and accents matter; a caller that wants to
     * ignore either folds both strings first.
     *
     * <p>Swaps are counted because a fast typist makes them as readily as the other three, and
     * measuring {@code lsit} as two edits away from {@code list} would put an everyday slip out
     * of reach of any threshold a wilder guess stays outside of.
     *
     * <p>No stretch of the string is edited twice, which is what the literature calls the
     * optimal string alignment distance. The count differs from the unrestricted one only when
     * a swapped pair is edited again between its two characters, as in {@code ca} and
     * {@code abc}, which this counts as three edits rather than two. Such a string has two
     * things wrong with it in the same place, so counting it as the further of the two is the
     * reading a caller looking for a likely typo wants.
     */
    public static int editDistance(String source, String target) {
        requireNonNull(source);
        requireNonNull(target);

        int[][] distances = new int[source.length() + 1][target.length() + 1];
        for (int i = 0; i <= source.length(); i++) {
            distances[i][0] = i;
        }
        for (int j = 0; j <= target.length(); j++) {
            distances[0][j] = j;
        }
        for (int i = 1; i <= source.length(); i++) {
            for (int j = 1; j <= target.length(); j++) {
                distances[i][j] = cheapestEdit(distances, source, target, i, j);
            }
        }
        return distances[source.length()][target.length()];
    }

    /**
     * Returns the fewest edits that align the first {@code i} characters of {@code source} with
     * the first {@code j} of {@code target}, given the answers for every shorter pair.
     */
    private static int cheapestEdit(int[][] distances, String source, String target, int i, int j) {
        int substitutionCost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
        int cheapest = Math.min(
                Math.min(distances[i - 1][j] + 1, distances[i][j - 1] + 1),
                distances[i - 1][j - 1] + substitutionCost);

        boolean isSwap = i > 1 && j > 1
                && source.charAt(i - 1) == target.charAt(j - 2)
                && source.charAt(i - 2) == target.charAt(j - 1);
        return isSwap ? Math.min(cheapest, distances[i - 2][j - 2] + 1) : cheapest;
    }

    /**
     * Returns a detailed message of {@code t}, including the stack trace.
     */
    public static String getDetails(Throwable t) {
        requireNonNull(t);
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return t.getMessage() + "\n" + sw.toString();
    }

    /**
     * Returns {@code raw} without the invisible format and non-whitespace control characters that let two
     * identical-looking pieces of a command differ: a direction mark or a zero-width space
     * pasted before a command word or an index is removed, while the zero-width joiner and
     * non-joiner a script needs to shape a word are kept.
     *
     * <p>The rule is the one {@link #normalizeFieldValue} applies to a stored value, so a
     * character the user cannot see is treated the same whether it lands in a field or on
     * the command line.
     */
    public static String stripFormatCharacters(String raw) {
        requireNonNull(raw);
        return INVISIBLE.matcher(raw).replaceAll("");
    }

    /**
     * Returns {@code raw} in the form a stored field holds: without invisible format
     * and non-whitespace control characters that would let two identical-looking values differ, with every run of
     * whitespace reduced to one ASCII space, the ends trimmed, and the result composed to
     * Unicode form NFC.
     *
     * <p>Composing happens last. An invisible character sitting between a base letter and its
     * combining mark blocks the two from composing, so {@code e\u200B\u0301} would otherwise
     * be stored decomposed and compare unequal to {@code \u00E9}, which looks the same.
     *
     * <p>Every Unicode format character is discarded but the zero-width joiner and non-joiner,
     * because scripts such as Sinhala and Arabic need them to shape correctly. Non-whitespace
     * control characters are discarded too. Whitespace controls still become spaces. What remains of
     * a name, a tag or a number is therefore what a reader sees: a direction mark pasted from a
     * chat app or a soft hyphen hides nothing, and two values that look alike are stored alike.
     */
    public static String normalizeFieldValue(String raw) {
        requireNonNull(raw);
        String visible = stripFormatCharacters(raw);
        String spaced = WHITESPACE.matcher(visible).replaceAll(" ").trim();
        return Normalizer.normalize(spaced, Normalizer.Form.NFC);
    }

    /**
     * Returns true if {@code s} holds at least one character that Unicode calls a letter or a
     * number. Logographic scripts such as Chinese count: Han characters are letters to Unicode,
     * even though a reader would not call them letters.
     */
    public static boolean hasLetterOrNumber(String s) {
        requireNonNull(s);
        return HAS_LETTER_OR_NUMBER.matcher(s).find();
    }

    /**
     * Returns true if {@code s} represents a non-zero unsigned integer
     * e.g. 1, 2, 3, ..., {@code Integer.MAX_VALUE} <br>
     * Will return false for any other non-null string input
     * e.g. empty string, "-1", "0", "+1", and " 2 " (untrimmed), "3 0" (contains whitespace), "1 a" (contains letters)
     * @throws NullPointerException if {@code s} is null.
     */
    public static boolean isNonZeroUnsignedInteger(String s) {
        requireNonNull(s);

        try {
            int value = Integer.parseInt(s);
            return value > 0 && !s.startsWith("+"); // "+1" is successfully parsed by Integer#parseInt(String)
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Returns an empty string if {@code count} is 1 (singular), or "s" for any other count (plural).
     *
     * @param count The item count.
     * @return The plural suffix ("" for singular, "s" for plural).
     */
    public static String getPluralSuffix(int count) {
        return count == 1 ? "" : "s";
    }
}
