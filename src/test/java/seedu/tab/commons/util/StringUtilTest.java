package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import java.io.FileNotFoundException;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class StringUtilTest {

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
    public void normalizeFieldValue_dropsZeroWidthCharactersButKeepsJoiners() {
        assertEquals("John", StringUtil.normalizeFieldValue("\u200BJohn"));
        assertEquals("John", StringUtil.normalizeFieldValue("John\uFEFF"));

        // Sinhala needs the joiner to shape correctly
        String sinhala = "\u0DC1\u0DCA\u200D\u0DBB\u0DD3";
        assertEquals(sinhala, StringUtil.normalizeFieldValue(sinhala));
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

}
