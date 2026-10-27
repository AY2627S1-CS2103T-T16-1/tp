package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import java.io.FileNotFoundException;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class StringUtilTest {

    @Test
    public void containsIgnoreCase_unicodeCaseVariants_returnsTrue() {
        String[][] cases = {
            {"ΝΙΚΟΣ", "Σ"},
            {"ΝΙΚΟΣ", "σ"},
            {"ΝΙΚΟΣ", "ς"},
            {"οσ", "ΟΣ"},
            {"ΟΣ", "οσ"},
            {"İ", "i"},
            {"ı", "I"},
            {"𐐀", "𐐨"}
        };
        for (String[] pair : cases) {
            String text = pair[0];
            String query = pair[1];
            assertTrue(StringUtil.containsIgnoreCase(text, query));
        }
    }

    @Test
    public void normalizeNameForComparison_curlyApostrophes_matchesStraightApostrophe() {
        assertEquals("sean o'brien", StringUtil.normalizeNameForComparison("Sean O‘Brien"));
        assertEquals("sean o'brien", StringUtil.normalizeNameForComparison("Sean O’Brien"));
    }

    @Test
    public void normalizeNameForComparison_combiningMarkCategories_removesOnlyLatinMarks() {
        // Nonspacing, spacing and enclosing marks all belong to the preceding base letter.
        assertEquals("a", StringUtil.normalizeNameForComparison("A\u0301\u093f\u20dd"));
        assertEquals("क\u0301\u093f\u20dd",
                StringUtil.normalizeNameForComparison("क\u0301\u093f\u20dd"));
    }

    @Test
    public void normalizeNameForComparison_latinLetterWithModifier_readsAsBaseLetter() {
        // no decomposition takes these letters apart, so the stroke has to be mapped away
        String[][] cases = {
            {"Đặng Thị Mai", "dang thi mai"},
            {"đặng thị mai", "dang thi mai"},
            {"Łukasz Nowak", "lukasz nowak"},
            {"Søren Kierkegaard", "soren kierkegaard"},
            {"Ħamed", "hamed"},
            {"Ŧorvald", "torvald"},
            {"Ǥunnar", "gunnar"},
            {"Ɨsabel", "isabel"},
            {"Ƀjörn", "bjorn"},
            {"Paŀla", "palla"}
        };
        for (String[] pair : cases) {
            assertEquals(pair[1], StringUtil.normalizeNameForComparison(pair[0]));
        }
    }

    @Test
    public void normalizeNameForComparison_letterStandingOnItsOwn_staysDistinct() {
        // Unicode names no base letter for these, and a reader spells each of them with two keys,
        // so folding them would be transliteration rather than a comparison key
        String[][] cases = {
            {"Æsa", "aesa"},
            {"Œuvre", "oeuvre"},
            {"Weiß", "weiss"},
            {"Ðóra", "dora"},
            {"Þór", "thor"},
            {"Ŋuyen", "nguyen"}
        };
        for (String[] pair : cases) {
            assertNotEquals(pair[1], StringUtil.normalizeNameForComparison(pair[0]));
        }
    }

    @Test
    public void normalizeNameForComparison_codePointUnicodeDoesNotName_keepsItAsItIs() {
        // U+FDD0 is a permanent noncharacter, so Unicode will never give it a name. A name
        // pasted from a damaged source can still hold one, and the key has to survive it.
        assertEquals("amy﷐", StringUtil.normalizeNameForComparison("Amy﷐"));
    }

    @Test
    public void normalizeNameForComparison_nonLatinScript_keepsItsOwnLettersAndMarks() {
        // Cyrillic Ө is barred like Ø, but a reader tells it apart from Cyrillic and Latin O
        assertNotEquals(StringUtil.normalizeNameForComparison("О"),
                StringUtil.normalizeNameForComparison("Ө"));
        assertNotEquals(StringUtil.normalizeNameForComparison("O"),
                StringUtil.normalizeNameForComparison("Ө"));
        // a Devanagari vowel sign is a vowel of its own, not an optional accent
        assertEquals("कि", StringUtil.normalizeNameForComparison("कि"));
    }

    @Test
    public void normalizeNameForComparison_otherScriptsAndPunctuation_preservesDistinctions() {
        assertFalse(StringUtil.normalizeNameForComparison("किरण")
                .equals(StringUtil.normalizeNameForComparison("करण")));
        assertFalse(StringUtil.normalizeNameForComparison("O'Brien")
                .equals(StringUtil.normalizeNameForComparison("Obrien")));
        assertEquals("", StringUtil.normalizeNameForComparison(""));
        assertThrows(NullPointerException.class, () -> StringUtil.normalizeNameForComparison(null));
    }

    //---------------- Tests for isNonZeroUnsignedInteger --------------------------------------

    @Test
    public void isNonZeroUnsignedInteger() {

        // EP: empty strings
        assertFalse(StringUtil.isNonZeroUnsignedInteger("")); // Boundary value
        assertFalse(StringUtil.isNonZeroUnsignedInteger("  "));

        // EP: not a number
        assertFalse(StringUtil.isNonZeroUnsignedInteger("a"));
        assertFalse(StringUtil.isNonZeroUnsignedInteger("aaa"));

        // EP: zero
        assertFalse(StringUtil.isNonZeroUnsignedInteger("0"));

        // EP: zero as prefix
        assertTrue(StringUtil.isNonZeroUnsignedInteger("01"));

        // EP: signed numbers
        assertFalse(StringUtil.isNonZeroUnsignedInteger("-1"));
        assertFalse(StringUtil.isNonZeroUnsignedInteger("+1"));

        // EP: numbers with white space
        assertFalse(StringUtil.isNonZeroUnsignedInteger(" 10 ")); // Leading/trailing spaces
        assertFalse(StringUtil.isNonZeroUnsignedInteger("1 0")); // Spaces in the middle

        // EP: number larger than Integer.MAX_VALUE
        assertFalse(StringUtil.isNonZeroUnsignedInteger(Long.toString(Integer.MAX_VALUE + 1)));

        // EP: valid numbers, should return true
        assertTrue(StringUtil.isNonZeroUnsignedInteger("1")); // Boundary value
        assertTrue(StringUtil.isNonZeroUnsignedInteger("10"));
    }


    //---------------- Tests for containsIgnoreCase --------------------------------------

    @Test
    public void containsIgnoreCase_nullSentence_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtil.containsIgnoreCase(null, "abc"));
    }

    @Test
    public void containsIgnoreCase_nullKeyword_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtil.containsIgnoreCase("typical sentence", null));
    }

    @Test
    public void containsIgnoreCase_emptyKeyword_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, "empty keyword", () -> StringUtil.containsIgnoreCase("x", ""));
    }

    @Test
    public void containsIgnoreCase_validInputs_correctResult() {

        // Empty sentence
        assertFalse(StringUtil.containsIgnoreCase("", "abc")); // Boundary case
        assertFalse(StringUtil.containsIgnoreCase("    ", "123"));

        // Matches part of a word
        assertTrue(StringUtil.containsIgnoreCase("Hans", "Han")); // Partial match
        assertTrue(StringUtil.containsIgnoreCase("Hans Bo", "bo")); // Case insensitive partial
        assertTrue(StringUtil.containsIgnoreCase("12345678", "345")); // Numeric partial

        // Full word match
        assertTrue(StringUtil.containsIgnoreCase("Hans Bo", "Hans")); // First word
        assertTrue(StringUtil.containsIgnoreCase("Hans Bo", "bo")); // Last word (boundary case)
        assertTrue(StringUtil.containsIgnoreCase("  Hans   Bo  ", "Hans")); // Extra spaces in sentence

        // Case insensitivity
        assertTrue(StringUtil.containsIgnoreCase("Hans Bo", "hans"));
        assertTrue(StringUtil.containsIgnoreCase("Hans Bo", "BO"));
        assertTrue(StringUtil.containsIgnoreCase("HANS BO", "ha"));
    }

    //---------------- Tests for getDetails --------------------------------------

    @Test
    public void getDetails_exceptionGiven() {
        assertTrue(StringUtil.getDetails(new FileNotFoundException("file not found"))
            .contains("java.io.FileNotFoundException: file not found"));
    }

    @Test
    public void getDetails_nullGiven_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtil.getDetails(null));
    }

    //---------------- Tests for stripFormatCharacters -----------------------------------

    @Test
    public void stripFormatCharacters_dropsInvisibleFormatCharactersButKeepsJoiners() {
        assertEquals("delete 1", StringUtil.stripFormatCharacters("\u200Bdelete 1"));
        assertEquals("delete 1", StringUtil.stripFormatCharacters("delete\u200E 1"));
        assertEquals("delete 1", StringUtil.stripFormatCharacters("delete 1\uFEFF"));
        assertEquals("John Doe", StringUtil.stripFormatCharacters("\u202AJohn Doe\u202C")); // embedding
        assertEquals("Johnson", StringUtil.stripFormatCharacters("John\u00ADson")); // soft hyphen

        // scripts such as Sinhala need the joiners to shape a word
        assertEquals("\u0DC1\u0DCA\u200D\u0DBB\u0DD3",
                StringUtil.stripFormatCharacters("\u0DC1\u0DCA\u200D\u0DBB\u0DD3"));
    }

    @Test
    public void stripFormatCharacters_leavesOrdinaryTextAlone() {
        assertEquals("delete 1", StringUtil.stripFormatCharacters("delete 1"));
        assertEquals("find 9123-4567", StringUtil.stripFormatCharacters("find 9123-4567"));
        assertEquals("", StringUtil.stripFormatCharacters(""));
    }

    //---------------- Tests for normalizeFieldValue -------------------------------------

    @Test
    public void normalizeFieldValue_reducesEveryKindOfWhitespaceToOneSpace() {
        assertEquals("John Doe", StringUtil.normalizeFieldValue("John\tDoe"));
        assertEquals("John Doe", StringUtil.normalizeFieldValue("John\u00A0Doe"));
        assertEquals("John Doe", StringUtil.normalizeFieldValue("John\u3000Doe"));
        assertEquals("John Doe", StringUtil.normalizeFieldValue("John  \n Doe"));
        assertEquals("John Doe", StringUtil.normalizeFieldValue("  John Doe  "));
    }

    @Test
    public void normalizeFieldValue_dropsInvisibleFormatCharactersButKeepsJoiners() {
        assertEquals("John", StringUtil.normalizeFieldValue("\u200BJohn"));
        assertEquals("John", StringUtil.normalizeFieldValue("John\uFEFF"));

        // marks a chat app or email wraps a copied name in, which draw nothing on screen
        assertEquals("John Doe", StringUtil.normalizeFieldValue("\u200EJohn Doe")); // left-to-right
        assertEquals("John Doe", StringUtil.normalizeFieldValue("\u200FJohn Doe")); // right-to-left
        assertEquals("John Doe", StringUtil.normalizeFieldValue("\u202AJohn Doe\u202C")); // embedding
        assertEquals("John Doe", StringUtil.normalizeFieldValue("\u202DJohn Doe\u202E")); // override
        assertEquals("John Doe", StringUtil.normalizeFieldValue("\u2066John\u2069 Doe")); // isolates
        assertEquals("Johnson", StringUtil.normalizeFieldValue("John\u00ADson")); // soft hyphen
        assertEquals("John Doe", StringUtil.normalizeFieldValue("John\u2060 Doe")); // word joiner

        // Sinhala needs the joiner to shape correctly
        String sinhala = "\u0DC1\u0DCA\u200D\u0DBB\u0DD3";
        assertEquals(sinhala, StringUtil.normalizeFieldValue(sinhala));
    }

    @Test
    public void normalizeFieldValue_everyFormatCharacter_isStrippedExceptTheTwoJoiners() {
        // naming the characters one by one left the rest of category Cf behind, so a lookalike
        // student could still be added; every format character must answer for itself
        for (int code = Character.MIN_CODE_POINT; code <= Character.MAX_CODE_POINT; code++) {
            if (Character.getType(code) != Character.FORMAT) {
                continue;
            }
            String character = new String(Character.toChars(code));
            boolean isJoiner = code == '\u200C' || code == '\u200D';
            String expected = isJoiner ? "a" + character + "b" : "ab";

            assertEquals(expected, StringUtil.normalizeFieldValue("a" + character + "b"),
                    "U+" + Integer.toHexString(code));
        }
    }

    @Test
    public void normalizeFieldValue_controlCharacters_removesHiddenControlsAndPreservesSeparators() {
        for (int codePoint = 0; codePoint <= 0x9f; codePoint++) {
            if (Character.getType(codePoint) != Character.CONTROL) {
                continue;
            }
            String control = Character.toString(codePoint);
            boolean isSeparator = StringUtil.isWhitespace((char) codePoint);
            assertEquals(isSeparator ? "a b" : "ab", StringUtil.normalizeFieldValue("a" + control + "b"));
            assertEquals(isSeparator ? "a" + control + "b" : "ab",
                    StringUtil.stripFormatCharacters("a" + control + "b"));
        }
        assertEquals("é", StringUtil.normalizeFieldValue("e" + Character.toString(1) + "\u0301"));
        assertEquals("", StringUtil.normalizeFieldValue(Character.toString(1)));
    }

    @Test
    public void normalizeFieldValue_composesToNfc() {
        assertEquals(StringUtil.normalizeFieldValue("Nguy\u1EC5n"),
                StringUtil.normalizeFieldValue("Nguye\u0302\u0303n"));
    }

    @Test
    public void normalizeFieldValue_zeroWidthBetweenBaseAndMark_stillComposes() {
        // A zero-width character sitting between a base letter and its combining mark blocks
        // the two from composing, so the deletion has to happen before the composition.
        assertEquals("\u00E9", StringUtil.normalizeFieldValue("e\u200B\u0301"));
        assertEquals(StringUtil.normalizeFieldValue("\u00E9"),
                StringUtil.normalizeFieldValue("e\u200B\u0301"));
        assertEquals("Nguy\u1EC5n", StringUtil.normalizeFieldValue("Nguye\u200B\u0302\u0303n"));
    }

    //---------------- Tests for hasLetterOrNumber ---------------------------------------

    @Test
    public void hasLetterOrNumber_variousCharacters_returnsExpectedResult() {
        assertTrue(StringUtil.hasLetterOrNumber("a"));
        assertTrue(StringUtil.hasLetterOrNumber("1"));
        assertTrue(StringUtil.hasLetterOrNumber("\u9648")); // a logograph counts
        assertFalse(StringUtil.hasLetterOrNumber(""));
        assertFalse(StringUtil.hasLetterOrNumber("---"));
        assertFalse(StringUtil.hasLetterOrNumber("\uD83D\uDE00")); // an emoji does not
    }

    //---------------- Tests for isWhitespace --------------------------------------

    @Test
    public void isWhitespace_ordinarySeparators_returnsTrue() {
        assertTrue(StringUtil.isWhitespace(' '));
        assertTrue(StringUtil.isWhitespace('\t'));
        assertTrue(StringUtil.isWhitespace('\n'));
    }

    @Test
    public void isWhitespace_unicodeSpaces_returnsTrue() {
        // Character.isWhitespace leaves these out, yet normalizeFieldValue collapses them, so
        // a caller splitting on whitespace has to agree with it
        assertTrue(StringUtil.isWhitespace('\u00A0'), "non-breaking space");
        assertTrue(StringUtil.isWhitespace('\u202F'), "narrow non-breaking space");
        assertTrue(StringUtil.isWhitespace('\u3000'), "ideographic space");
        assertTrue(StringUtil.isWhitespace('\u0085'), "next line");
    }

    @Test
    public void isWhitespace_everyCharacter_agreesWithTheNormalizingPattern() {
        // the two disagreed in both directions while the characters were named one by one
        Pattern pattern = Pattern.compile("(?U)\\s");
        for (int code = Character.MIN_VALUE; code <= Character.MAX_VALUE; code++) {
            String character = String.valueOf((char) code);
            assertEquals(pattern.matcher(character).matches(),
                    StringUtil.isWhitespace(character.charAt(0)),
                    "disagreed on U+" + Integer.toHexString(code));
        }
    }

    @Test
    public void isWhitespace_ordinaryCharacters_returnsFalse() {
        assertFalse(StringUtil.isWhitespace('a'));
        assertFalse(StringUtil.isWhitespace('-'));
        assertFalse(StringUtil.isWhitespace('\u200B'), "a zero-width space separates nothing");
        assertFalse(StringUtil.isWhitespace('\u001C'), "a file separator is not a space");
    }

    //---------------- Tests for computeEditDistance --------------------------------------

    @Test
    public void computeEditDistance_identicalStrings_returnsZero() {
        assertEquals(0, StringUtil.computeEditDistance("delete", "delete"));
        assertEquals(0, StringUtil.computeEditDistance("", ""));
    }

    @Test
    public void computeEditDistance_oneEditApart_returnsOne() {
        assertEquals(1, StringUtil.computeEditDistance("delte", "delete")); // missing letter
        assertEquals(1, StringUtil.computeEditDistance("deletee", "delete")); // extra letter
        assertEquals(1, StringUtil.computeEditDistance("lsit", "list")); // swapped letters
        assertEquals(1, StringUtil.computeEditDistance("lixt", "list")); // wrong letter
    }

    @Test
    public void computeEditDistance_twoEditsApart_returnsTwo() {
        assertEquals(2, StringUtil.computeEditDistance("dlte", "delete")); // two missing letters
        assertEquals(2, StringUtil.computeEditDistance("ls", "list"));
        assertEquals(2, StringUtil.computeEditDistance("lixtt", "list"));
    }

    @Test
    public void computeEditDistance_unrelatedStrings_returnsThreeOrMore() {
        assertEquals(3, StringUtil.computeEditDistance("cat", "add"));
        assertEquals(3, StringUtil.computeEditDistance("list", "find"));
        assertEquals(6, StringUtil.computeEditDistance("", "delete"));
    }

    @Test
    public void computeEditDistance_caseAndAccents_countAsEdits() {
        assertEquals(1, StringUtil.computeEditDistance("List", "list"));
        assertEquals(4, StringUtil.computeEditDistance("LIST", "list"));
        assertEquals(1, StringUtil.computeEditDistance("lïst", "list"));
    }

    @Test
    public void computeEditDistance_isSymmetric() {
        assertEquals(StringUtil.computeEditDistance("delte", "delete"),
                StringUtil.computeEditDistance("delete", "delte"));
        assertEquals(StringUtil.computeEditDistance("ca", "abc"), StringUtil.computeEditDistance("abc", "ca"));
    }

    @Test
    public void computeEditDistance_swapWithAnEditBetweenIt_countsBothEdits() {
        // the optimal string alignment reading: "ca" and "abc" are a swap and an insertion
        // apart, but the insertion falls between the swapped pair, so it counts as three
        assertEquals(3, StringUtil.computeEditDistance("ca", "abc"));
    }

    @Test
    public void computeEditDistance_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtil.computeEditDistance(null, "list"));
        assertThrows(NullPointerException.class, () -> StringUtil.computeEditDistance("list", null));
    }

    //---------------- Tests for getFirstCharacters --------------------------------------

    @Test
    public void getFirstCharacters_valueWithinTheLimit_returnsTheWholeValue() {
        assertEquals("Lab 3", StringUtil.getFirstCharacters("Lab 3", 10));
        assertEquals("Lab 3", StringUtil.getFirstCharacters("Lab 3", 5), "a value exactly at the limit");
        assertEquals("", StringUtil.getFirstCharacters("", 5));
    }

    @Test
    public void getFirstCharacters_valueOverTheLimit_keepsTheOpeningCharacters() {
        assertEquals("attendance", StringUtil.getFirstCharacters("attendance-low", 10));
        assertEquals("", StringUtil.getFirstCharacters("attendance-low", 0));
    }

    @Test
    public void getFirstCharacters_emojiAtTheLimit_keepsItWhole() {
        // an emoji is one character to a reader and two to Java, and half of one draws as a box
        assertEquals("ab\uD83D\uDE00", StringUtil.getFirstCharacters("ab\uD83D\uDE00cd", 3));
        assertEquals("ab", StringUtil.getFirstCharacters("ab\uD83D\uDE00cd", 2));
    }

    @Test
    public void getFirstCharacters_combiningAccentAtTheLimit_keepsItOnItsLetter() {
        String value = "ae\u0301iou"; // "a\u00E9iou", with the accent held as a separate code point

        assertEquals("ae\u0301", StringUtil.getFirstCharacters(value, 2));
        assertEquals("a", StringUtil.getFirstCharacters(value, 1));
    }

    @Test
    public void getFirstCharacters_negativeLimit_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> StringUtil.getFirstCharacters("abc", -1));
    }

    @Test
    public void getFirstCharacters_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtil.getFirstCharacters(null, 3));
    }

    //---------------- Tests for getLastCharacters --------------------------------------

    @Test
    public void getLastCharacters_valueWithinTheLimit_returnsTheWholeValue() {
        assertEquals("Lab 3", StringUtil.getLastCharacters("Lab 3", 10));
        assertEquals("Lab 3", StringUtil.getLastCharacters("Lab 3", 5), "a value exactly at the limit");
        assertEquals("", StringUtil.getLastCharacters("", 5));
    }

    @Test
    public void getLastCharacters_valueOverTheLimit_keepsTheClosingCharacters() {
        assertEquals("ance-low", StringUtil.getLastCharacters("attendance-low", 8));
        assertEquals("", StringUtil.getLastCharacters("attendance-low", 0));
    }

    @Test
    public void getLastCharacters_emojiAtTheLimit_keepsItWhole() {
        // an emoji is one character to a reader and two to Java, and half of one draws as a box
        assertEquals("\uD83D\uDE00cd", StringUtil.getLastCharacters("ab\uD83D\uDE00cd", 3));
        assertEquals("cd", StringUtil.getLastCharacters("ab\uD83D\uDE00cd", 2));
    }

    @Test
    public void getLastCharacters_combiningAccentAtTheLimit_keepsItOnItsLetter() {
        String value = "aie\u0301ou"; // "ai\u00E9ou", with the accent held as a separate code point

        assertEquals("e\u0301ou", StringUtil.getLastCharacters(value, 3));
        assertEquals("ou", StringUtil.getLastCharacters(value, 2));
    }

    @Test
    public void getLastCharacters_negativeLimit_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> StringUtil.getLastCharacters("abc", -1));
    }

    @Test
    public void getLastCharacters_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtil.getLastCharacters(null, 3));
    }

    //---------------- Tests for getPluralSuffix --------------------------------------

    @Test
    public void getPluralSuffix_singularCount_returnsEmptyString() {
        assertEquals("", StringUtil.getPluralSuffix(1));
    }

    @Test
    public void getPluralSuffix_zeroOrMultipleCount_returnsPluralSuffix() {
        assertEquals("s", StringUtil.getPluralSuffix(0));
        assertEquals("s", StringUtil.getPluralSuffix(2));
        assertEquals("s", StringUtil.getPluralSuffix(24));
    }

}
