package seedu.tab.commons.util;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Helper functions for handling strings.
 */
public class StringUtil {

    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B\\uFEFF]");
    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");
    private static final Pattern HAS_LETTER_OR_NUMBER = Pattern.compile("[\\p{L}\\p{N}]");

    /**
     * Returns true if {@code character} separates one value from the next.
     *
     * <p>{@code Character.isWhitespace} leaves out the non-breaking space, which a command
     * pasted from a web page or a chat message carries. Matching it here keeps a caller that
     * splits on whitespace agreeing with {@link #normalizeFieldValue}, whose pattern does
     * match it.
     */
    public static boolean isWhitespace(char character) {
        return Character.isWhitespace(character) || Character.isSpaceChar(character);
    }

    /**
     * Returns true if the {@code sentence} contains the {@code word}.
     *   Ignores case, but a full word match is required.
     *   <br>examples:<pre>
     *       containsWordIgnoreCase("ABc def", "abc") == true
     *       containsWordIgnoreCase("ABc def", "DEF") == true
     *       containsWordIgnoreCase("ABc def", "AB") == false //not a full word match
     *       </pre>
     * @param sentence cannot be null
     * @param word cannot be null, cannot be empty, must be a single word
     */
    public static boolean containsWordIgnoreCase(String sentence, String word) {
        requireNonNull(sentence);
        requireNonNull(word);

        String preppedWord = word.trim();
        checkArgument(!preppedWord.isEmpty(), "Word parameter cannot be empty");
        checkArgument(preppedWord.split("\\s+").length == 1, "Word parameter should be a single word");

        String preppedSentence = sentence;
        String[] wordsInPreppedSentence = preppedSentence.split("\\s+");

        return Arrays.stream(wordsInPreppedSentence)
                .anyMatch(preppedWord::equalsIgnoreCase);
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
     * Returns {@code raw} in the form a stored field holds: without the zero-width characters
     * that would let two identical-looking values differ, with every run of whitespace reduced
     * to one ASCII space, the ends trimmed, and the result composed to Unicode form NFC.
     *
     * Composing happens last. A zero-width character sitting between a base letter and its
     * combining mark blocks the two from composing, so {@code e\u200B\u0301} would otherwise
     * be stored decomposed and compare unequal to {@code \u00E9}, which looks the same.
     *
     * The zero-width joiner and non-joiner are kept, because scripts such as Sinhala and
     * Arabic need them to shape correctly.
     */
    public static String normalizeFieldValue(String raw) {
        requireNonNull(raw);
        String visible = ZERO_WIDTH.matcher(raw).replaceAll("");
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
}
