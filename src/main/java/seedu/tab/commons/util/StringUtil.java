package seedu.tab.commons.util;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper functions for handling strings.
 */
public class StringUtil {

    /** Why {@link #holdsLetterOrNumber(String)} turned a value down, phrased for the user. */
    public static final String REASON_NO_LETTER_OR_NUMBER = "it holds no letter or number";

    // The Unicode name of a Latin letter that is one base letter carrying a modifier, such as
    // LATIN SMALL LETTER D WITH STROKE for đ. The base letter has to be a single letter, and the
    // modifier must name no second letter, so that the pattern passes over the letters whose name
    // spells out two of them: ǅ is LATIN CAPITAL LETTER D WITH SMALL LETTER Z WITH CARON, and
    // taking D alone from it would lose the z a reader sees.
    private static final Pattern LETTER_WITH_MODIFIER =
            Pattern.compile("LATIN (?:CAPITAL|SMALL) LETTER ([A-Z]) WITH (?!.*LETTER).+");

    // almost every Unicode format character (category Cf), except the joiner and non-joiner a
    // script needs to shape a word. A few of them do draw something — the Arabic end-of-ayah
    // sign and the characters that tag a Scotland flag — but none is likely in a class list,
    // and dropping them is the price of closing the whole category. Non-whitespace control
    // characters are removed too; whitespace controls remain available as separators.
    private static final Pattern INVISIBLE = Pattern.compile("(?U)[\\p{Cf}\\p{Cc}&&[^\\u200C\\u200D\\s]]");
    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");
    private static final Pattern HAS_LETTER_OR_NUMBER = Pattern.compile("[\\p{L}\\p{N}]");
    // Every character Unicode calls a dash, together with the minus sign, which it files under
    // mathematical symbols instead. Which dash a name list carries is the choice of whatever
    // wrote it rather than of the reader: a word processor turns a typed hyphen into an en dash,
    // and a roll exported from one spells Mary–Jane where a sign-up sheet spells Mary-Jane. The
    // prolonged sound mark of ラーメン is a letter to Unicode and stays, as does any other
    // dash-shaped character that is not punctuation. FollowUpFlag folds the same set.
    private static final Pattern HYPHEN_OR_DASH = Pattern.compile("[\\p{Pd}\\u2212]");
    // The characters a name list puts where a reader sees an apostrophe, each read as the straight
    // one when names are compared: the two curly quotation marks a word processor substitutes, the
    // fullwidth apostrophe a CJK keyboard produces, the prime a spreadsheet leaves behind, and the
    // three modifier letters that a roll of transliterated names carries — the modifier apostrophe
    // of DʼSouza, the turned comma that is the okina of Hawaiʻi, and the modifier prime that stands
    // for the soft sign in Tatʹyana. Unicode files those three as letters, and in their own
    // orthographies they are letters; a reader at an ASCII keyboard still has one key for all of
    // them, and this builds a comparison key while the stored spelling stands untouched.
    private static final String APOSTROPHE_VARIANTS = "\u2018\u2019\uff07\u2032\u02bc\u02bb\u02b9";

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
    public static String getFirstCharacters(String value, int limit) {
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
    public static String getLastCharacters(String value, int limit) {
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
     * @param sentence cannot be null.
     * @param keyword cannot be null, cannot be empty.
     */
    public static boolean containsIgnoreCase(String sentence, String keyword) {
        requireNonNull(sentence);
        requireNonNull(keyword);
        checkArgument(!keyword.isEmpty(), "empty keyword");

        return foldCase(sentence).contains(foldCase(keyword));
    }

    /**
     * Returns a comparison key for a name, ignoring case, Latin accents, the style of an
     * apostrophe and the difference between a dash and a space. The stored name is left unchanged.
     * Marks belonging to other scripts are retained because they may represent separate vowels
     * rather than optional accents.
     *
     * <p>A dash reads as a space because two lists of the same class routinely disagree on it:
     * a sign-up sheet carries {@code Mary-Jane}, the official roll carries {@code Mary Jane}, and
     * a roll typed in a word processor carries {@code Mary–Jane}. Whitespace runs are then
     * reduced the way a stored value reduces them, so a dash that already has spaces around it
     * does not leave a gap the other spelling cannot match.
     *
     * <p>The key is composed back to NFC last. Decomposing is what sets a Latin accent apart from
     * its letter so it can be dropped, but it also splits a Hangul syllable into the letters it is
     * written from, and those are letters rather than marks, so nothing drops them again. 이 would
     * stay as the two letters that open 임, and a search for the one surname would find the other.
     * Composing cannot bring back a mark already dropped, so the accents stay folded.
     */
    public static String normalizeNameForComparison(String name) {
        requireNonNull(name);
        String decomposed = Normalizer.normalize(name, Normalizer.Form.NFD);
        String withBaseLetters = stripLetterModifiers(decomposed);
        StringBuilder result = new StringBuilder();
        boolean isLatin = false;
        for (int codePoint : withBaseLetters.codePoints().toArray()) {
            int type = Character.getType(codePoint);
            boolean isMark = type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                    || type == Character.ENCLOSING_MARK;
            if (!isMark) {
                isLatin = Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN;
            }
            if (!isMark || !isLatin) {
                result.appendCodePoint(straightenApostrophe(codePoint));
            }
        }
        String dashesAsSpaces = HYPHEN_OR_DASH.matcher(result.toString()).replaceAll(" ");
        String spaced = WHITESPACE.matcher(dashesAsSpaces).replaceAll(" ");
        return Normalizer.normalize(foldCase(spaced), Normalizer.Form.NFC);
    }

    /**
     * Returns {@code codePoint} with any character a name list uses for an apostrophe read as the
     * straight one, so that {@code O’Brien} typed in a word processor and {@code DʼSouza} pasted
     * from a roster both match the {@code O'Brien} and {@code D'Souza} a reader types in TAB.
     *
     * <p>The grave accent and the acute accent are left as they are, even though a keyboard
     * without an apostrophe key produces them. Both mark the letter beside them rather than stand
     * between two letters, which is how this method already treats the accents it strips, and a
     * name spelled with one is far more likely to want that reading than an apostrophe.
     */
    private static int straightenApostrophe(int codePoint) {
        return APOSTROPHE_VARIANTS.indexOf(codePoint) >= 0 ? '\'' : codePoint;
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
     * Returns {@code value} with every Latin letter that carries a stroke, a slash, a bar, a hook
     * or another modifier replaced by the letter underneath it, so that {@code Đặng} reads as
     * {@code Dang} and {@code Søren} as {@code Soren} when names are compared.
     *
     * <p>Canonical decomposition alone cannot reach these letters. An accented letter such as
     * {@code é} splits into a base letter and a mark that the caller then drops, but {@code Đ},
     * {@code Ł} and {@code Ø} are single code points that no decomposition takes apart, so their
     * stroke survives every step and a reader typing {@code dang} finds nobody.
     *
     * <p>Which letter lies underneath comes from the Unicode name rather than a list written
     * here, because Unicode already answers the question for every such letter and never
     * renames one. The name has to describe one base letter plus a modifier, which keeps the
     * mapping one letter to one letter and leaves out the letters that stand on their own:
     * {@code Æ}, {@code Œ}, {@code ß}, {@code Ð} and {@code Þ} name no base letter, and a reader
     * spells each of them with two keys, so expanding one would be transliteration rather than
     * a comparison key.
     */
    private static String stripLetterModifiers(String value) {
        StringBuilder result = new StringBuilder();
        for (int codePoint : value.codePoints().toArray()) {
            result.appendCodePoint(findBaseLetter(codePoint));
        }
        return result.toString();
    }

    /**
     * Returns the letter underneath {@code codePoint} when Unicode names it as a base letter
     * carrying a modifier, and {@code codePoint} itself otherwise. The letter comes back in
     * capital form, as Unicode names spell it; the caller folds case afterwards.
     */
    private static int findBaseLetter(int codePoint) {
        String name = Character.getName(codePoint);
        if (name == null) {
            return codePoint;
        }
        Matcher named = LETTER_WITH_MODIFIER.matcher(name);
        return named.matches() ? named.group(1).codePointAt(0) : codePoint;
    }

    /**
     * Returns how many single-character edits turn {@code source} into {@code target}, counting
     * an insertion, a deletion, a substitution and a swap of two neighboring characters as one
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
    public static int computeEditDistance(String source, String target) {
        requireNonNull(source);
        requireNonNull(target);

        int[][] distances = new int[source.length() + 1][target.length() + 1];
        for (int i = 0; i <= source.length(); i++) {
            distances[i][0] = i;
        }
        for (int column = 0; column <= target.length(); column++) {
            distances[0][column] = column;
        }
        for (int i = 1; i <= source.length(); i++) {
            for (int j = 1; j <= target.length(); j++) {
                distances[i][j] = computeCheapestEdit(distances, source, target, i, j);
            }
        }
        return distances[source.length()][target.length()];
    }

    /**
     * Returns the fewest edits that align the first {@code i} characters of {@code source} with
     * the first {@code j} of {@code target}, given the answers for every shorter pair.
     */
    private static int computeCheapestEdit(int[][] distances, String source, String target, int i, int j) {
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
     * Returns true if {@code text} holds nothing a command could be read from: nothing at all,
     * only whitespace, or only the invisible characters a paste carries. The command box asks
     * this before submitting, so a stray space before Enter is ignored the way an empty line
     * is rather than answered as a mistyped command.
     */
    public static boolean isBlankInput(String text) {
        requireNonNull(text);
        String visible = stripFormatCharacters(text);
        for (int index = 0; index < visible.length(); index++) {
            if (!isWhitespace(visible.charAt(index))) {
                return false;
            }
        }
        return true;
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
     *
     * <p>This reads {@code s} as it stands. A raw value a person typed goes through
     * {@link #holdsLetterOrNumber(String)}, which normalizes it first.
     */
    public static boolean hasLetterOrNumber(String s) {
        requireNonNull(s);
        return HAS_LETTER_OR_NUMBER.matcher(s).find();
    }

    /**
     * Returns true if {@code value} contains at least one letter or number once normalized:
     * the rule a stored field value has to meet, shared so that two fields asking it cannot
     * drift apart. A value of punctuation alone, such as "!!!", survives normalization and is
     * turned down all the same.
     *
     * <p>This is the question a field asks of what a person typed, so it calls
     * {@link #normalizeFieldValue(String)} first and judges the value in the form the field
     * stores. {@link #hasLetterOrNumber(String)} answers for a string as it stands.
     */
    public static boolean holdsLetterOrNumber(String value) {
        requireNonNull(value);
        return hasLetterOrNumber(normalizeFieldValue(value));
    }

    /**
     * Returns true if {@code s} represents a non-zero unsigned integer
     * e.g. 1, 2, 3, ..., {@code Integer.MAX_VALUE} <br>
     * Will return false for any other non-null string input
     * e.g. empty string, "-1", "0", "+1", and " 2 " (untrimmed), "3 0" (contains whitespace), "1 a" (contains letters)
     *
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
