package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.AccessDeniedException;
import java.text.BreakIterator;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;

import seedu.tab.commons.core.DataLoadingErrorMessage;

/**
 * Tests the explanations given for a data file that cannot be read.
 */
public class JsonParseErrorFormatterTest {

    private static final String GRINNING_FACE = "\uD83D\uDE00";
    private static final String SINGAPORE_FLAG = "\uD83C\uDDF8\uD83C\uDDEC";
    private static final String FAMILY = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67";
    private static final String ACCENTED_E = "e\u0301";
    private static final String ELLIPSIS = "\u2026";
    private static final int LONG_LINE_CHARACTER_COUNT = 200;
    private static final int EXCERPT_MAX_CHARACTER_COUNT = 160;

    // Characters a reader sees as one but Jackson reports several columns for. Each is repeated
    // into a line too long to quote whole, so that wherever the crop lands it lands inside one
    // of them unless the crop reads the line the way a reader reads it.
    private static final String[] MULTI_COLUMN_CHARACTERS = {
        GRINNING_FACE, SINGAPORE_FLAG, FAMILY, ACCENTED_E,
    };

    @Test
    public void format_regionalDefaults_keepAsciiLocations() {
        Locale general = Locale.getDefault();
        Locale display = Locale.getDefault(Locale.Category.DISPLAY);
        Locale format = Locale.getDefault(Locale.Category.FORMAT);
        try {
            for (String tag : new String[] {"en-SG", "tr-TR", "az-AZ", "fa-IR", "ar-EG", "bn-BD"}) {
                Locale.setDefault(Locale.forLanguageTag(tag));
                format_withoutSource_keepsAvailableLocation();
                format_unusableColumn_reportsLineOnly();
                format_sourceDescriptions_omitsSourceDetails();
            }
        } finally {
            Locale.setDefault(general);
            Locale.setDefault(Locale.Category.DISPLAY, display);
            Locale.setDefault(Locale.Category.FORMAT, format);
        }
    }

    @Test
    public void format_affectedLineInSource_keepsExcerptApartFromExplanation() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 2, 1);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        DataLoadingErrorMessage errorMessage = JsonParseErrorFormatter.format(exception, "first\nsecond\nthird");

        assertEquals("The data is not in the expected JSON format at line 2, column 1: Invalid content",
                errorMessage.description());
        assertEquals("  1 | first\n> 2 | second\n  3 | third", errorMessage.sourceExcerpt());
        assertTrue(errorMessage.hasSourceExcerpt());
    }

    @Test
    public void format_withoutSource_quotesNoPartOfTheFile() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 2, 3);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        DataLoadingErrorMessage errorMessage = JsonParseErrorFormatter.format(exception, null);

        assertFalse(errorMessage.hasSourceExcerpt());
        assertEquals("", errorMessage.sourceExcerpt());
        assertEquals(errorMessage.description(), errorMessage.toDisplayText());
    }

    @Test
    public void format_withoutLocation_omitsLocation() {
        JsonProcessingException exception = new JsonProcessingException("Invalid content without a location") {};

        assertEquals("The data is not in the expected JSON format: Invalid content without a location",
                JsonParseErrorFormatter.format(exception, "source that must not be displayed").toDisplayText());
    }

    @Test
    public void format_withoutSource_keepsAvailableLocation() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 2, 3);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        assertEquals("The data is not in the expected JSON format at line 2, column 3: Invalid content",
                JsonParseErrorFormatter.format(exception, null).toDisplayText());
    }

    @Test
    public void format_wrappedCauseWithLocation_reportsDeepestLocation() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 2, 3);
        JsonProcessingException cause = createJsonProcessingException("Invalid content", location);
        JsonProcessingException exception = new JsonProcessingException("Mapping failed", null, cause) {};

        assertEquals("The data is not in the expected JSON format at line 2, column 3: Invalid content",
                JsonParseErrorFormatter.format(exception, null).toDisplayText());
    }

    @Test
    public void format_invalidLine_omitsLocationAndSource() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 0, 4);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        assertEquals("The data is not in the expected JSON format: Invalid content",
                JsonParseErrorFormatter.format(exception, "source that must not be displayed").toDisplayText());
    }

    @Test
    public void format_unusableColumn_reportsLineOnly() {
        String expectedMessage = "The data is not in the expected JSON format at line 2: Invalid content\n"
                + "Affected source:\n"
                + "  1 | first\n"
                + "> 2 | second\n"
                + "  3 | third";
        for (int column : new int[] {0, 99}) {
            JsonLocation location = new JsonLocation("hidden source", -1, -1, 2, column);
            JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

            assertEquals(expectedMessage,
                    JsonParseErrorFormatter.format(exception, "first\nsecond\nthird").toDisplayText());
        }
    }

    @Test
    public void format_missingParserDetails_reportsFallback() {
        for (String originalMessage : new String[] {null, " \t "}) {
            JsonProcessingException exception = new JsonProcessingException(originalMessage) {};

            assertEquals("The data is not in the expected JSON format: "
                            + "The parser did not provide further details.",
                    JsonParseErrorFormatter.format(exception, null).toDisplayText());
        }
    }

    @Test
    public void format_sourceDescriptions_omitsSourceDetails() {
        assertConciseJsonMessage("Unexpected token (from [Source: hidden])", "Unexpected token");
        assertConciseJsonMessage("Unexpected token (for ROOT starting at [Source: hidden])", "Unexpected token");
        assertConciseJsonMessage("Unexpected token at [Source: hidden]", "Unexpected token");
        assertConciseJsonMessage("Unexpected token [Source: hidden]", "Unexpected token");
        assertConciseJsonMessage("Unexpected token at context [Source: hidden]", "Unexpected token at context");
        assertConciseJsonMessage("Unexpected token (near [Source: hidden])", "Unexpected token");
        assertConciseJsonMessage("Unexpected token (near [Source: hidden; line: unknown])", "Unexpected token");
        assertConciseJsonMessage(
                "Unexpected close marker '}': expected ']' "
                        + "(for ARRAY starting at [Source: hidden; line: 8, column: 14])",
                "Unexpected close marker '}': expected ']' (for ARRAY starting at line 8, column 14)");
    }

    @Test
    public void format_locationBeyondSource_omitsExcerpt() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 3, 1);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        assertEquals("The data is not in the expected JSON format at the end of the file: Invalid content",
                JsonParseErrorFormatter.format(exception, "only one line").toDisplayText());
    }

    @Test
    public void format_locationAfterLastLine_marksLastLine() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 3, 1);
        JsonProcessingException exception = createJsonProcessingException("Unexpected end", location);

        String expectedMessage = "The data is not in the expected JSON format at the end of the file: Unexpected end\n"
                + "Affected source:\n"
                + "  1 | first\n"
                + "> 2 | second";
        assertEquals(expectedMessage,
                JsonParseErrorFormatter.format(exception, "first\nsecond").toDisplayText());
    }

    @Test
    public void format_longAffectedLineNearStart_preservesErrorArea() {
        String jsonSource = "x".repeat(200);
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 1, 1);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        String expectedMessage = "The data is not in the expected JSON format at line 1, column 1: Invalid content\n"
                + "Affected source:\n"
                + "> 1 | " + "x".repeat(158) + "…";
        assertEquals(expectedMessage, JsonParseErrorFormatter.format(exception, jsonSource).toDisplayText());
    }

    @Test
    public void format_croppedLine_marksTheCropWithOneEllipsisCharacter() {
        String excerptLine = getAffectedExcerptLine("x".repeat(500), 250);

        assertTrue(excerptLine.startsWith(ELLIPSIS));
        assertTrue(excerptLine.endsWith(ELLIPSIS));
        assertFalse(excerptLine.contains("..."));
        assertEquals(EXCERPT_MAX_CHARACTER_COUNT, countCharacters(excerptLine));
    }

    @Test
    public void format_longAffectedLine_cropsBetweenCharactersAtEveryColumn() {
        for (String character : MULTI_COLUMN_CHARACTERS) {
            String sourceLine = character.repeat(LONG_LINE_CHARACTER_COUNT);

            for (int column = 1; column <= sourceLine.length() + 1; column++) {
                assertWholeCharactersOf(getAffectedExcerptLine(sourceLine, column), sourceLine);
            }
        }
    }

    @Test
    public void format_flagHalfwayThroughCropPoint_keepsWholeCharacters() {
        String sourceLine = SINGAPORE_FLAG.repeat(LONG_LINE_CHARACTER_COUNT);
        // A flag is two halves that each read as a character on their own, so a crop that starts
        // reading the line at this column pairs every half after it with the wrong neighbour and
        // puts its own end inside a flag.
        int halfwayThroughAFlag = 399;

        assertWholeCharactersOf(getAffectedExcerptLine(sourceLine, halfwayThroughAFlag), sourceLine);
    }

    @Test
    public void format_longContextLine_cropsBetweenCharacters() {
        for (String character : MULTI_COLUMN_CHARACTERS) {
            String sourceLine = character.repeat(LONG_LINE_CHARACTER_COUNT);
            JsonLocation location = new JsonLocation("hidden source", -1, -1, 2, 1);
            JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

            String excerpt = JsonParseErrorFormatter
                    .format(exception, sourceLine + "\nsecond").sourceExcerpt();

            assertWholeCharactersOf(getExcerptLine(excerpt, "  1 | "), sourceLine);
        }
    }

    @Test
    public void format_longAffectedLine_marksOnlyTheEndsItCropped() {
        for (String character : MULTI_COLUMN_CHARACTERS) {
            String sourceLine = character.repeat(LONG_LINE_CHARACTER_COUNT);
            int lastColumn = sourceLine.length();
            int middleColumn = lastColumn / 2 + 1;

            String fromStart = getAffectedExcerptLine(sourceLine, 1);
            assertFalse(fromStart.startsWith(ELLIPSIS));
            assertTrue(fromStart.endsWith(ELLIPSIS));
            assertWholeCharactersOf(fromStart, sourceLine);

            String aroundMiddle = getAffectedExcerptLine(sourceLine, middleColumn);
            assertTrue(aroundMiddle.startsWith(ELLIPSIS));
            assertTrue(aroundMiddle.endsWith(ELLIPSIS));
            assertWholeCharactersOf(aroundMiddle, sourceLine);

            String toEnd = getAffectedExcerptLine(sourceLine, lastColumn);
            assertTrue(toEnd.startsWith(ELLIPSIS));
            assertFalse(toEnd.endsWith(ELLIPSIS));
            assertWholeCharactersOf(toEnd, sourceLine);
        }
    }

    @Test
    public void format_twoDigitLines_alignsSeparators() {
        String jsonSource = String.join("\n", "one", "two", "three", "four", "five",
                "six", "seven", "eight", "nine", "ten");
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 9, 1);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        DataLoadingErrorMessage errorMessage = JsonParseErrorFormatter.format(exception, jsonSource);

        assertEquals("   8 | eight\n>  9 | nine\n  10 | ten", errorMessage.sourceExcerpt());
    }

    @Test
    public void format_rejectedComment_explainsRuleAndKeepsExcerpt() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 2, 2);
        JsonProcessingException exception = createJsonProcessingException("Unexpected character ('/' (code 47)):"
                + " maybe a (non-standard) comment? (not recognized as one since Feature 'ALLOW_COMMENTS'"
                + " not enabled for parser)", location);

        String expectedMessage = "The data is not in the expected JSON format at line 2, column 2:"
                + " A JSON file cannot hold comments. Remove this one, or move its text into the"
                + " optional top-level \"_comment\" field.\n"
                + "Affected source:\n"
                + "  1 | {\n"
                + "> 2 | // a note\n"
                + "  3 | }";
        assertEquals(expectedMessage,
                JsonParseErrorFormatter.format(exception, "{\n// a note\n}").toDisplayText());
    }

    @Test
    public void format_blankSource_explainsEmptyFileWithoutLocationOrExcerpt() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 1, 0);
        JsonProcessingException exception =
                createJsonProcessingException("No content to map due to end-of-input", location);

        for (String jsonSource : new String[] {"", " \n\t "}) {
            assertEquals("The data is not in the expected JSON format: The file holds no data."
                            + " Restore your backup, or delete the file and restart TAB"
                            + " to start again from the sample students.",
                    JsonParseErrorFormatter.format(exception, jsonSource).toDisplayText());
        }
    }

    @Test
    public void formatIoError_invalidEncoding_explainsRecovery() {
        MalformedInputException exception = new MalformedInputException(1);

        assertEquals("The file is not valid UTF-8. Reopen it with its original encoding and save it as UTF-8,"
                        + " then restart TAB.",
                JsonParseErrorFormatter.formatIoError(exception));
    }

    @Test
    public void formatIoError_accessDenied_explainsPermissionFailure() {
        AccessDeniedException exception = new AccessDeniedException("data/tab.json");

        assertEquals("TAB does not have permission to read this file or the folder holding it.",
                JsonParseErrorFormatter.formatIoError(exception));
    }

    @Test
    public void formatIoError_otherIoFailure_includesDetails() {
        IOException exception = new IOException("The device is unavailable");

        assertEquals("The file could not be read: The device is unavailable",
                JsonParseErrorFormatter.formatIoError(exception));
    }

    private JsonProcessingException createJsonProcessingException(String message, JsonLocation location) {
        return new JsonProcessingException(message, location) {};
    }

    /**
     * Returns the only excerpt line of a one-line {@code sourceLine} reported as faulty at
     * {@code affectedColumn}, without the marker and line number in front of it.
     */
    private String getAffectedExcerptLine(String sourceLine, int affectedColumn) {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 1, affectedColumn);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        return getExcerptLine(JsonParseErrorFormatter.format(exception, sourceLine).sourceExcerpt(), "> 1 | ");
    }

    private String getExcerptLine(String excerpt, String prefix) {
        String excerptLine = excerpt.lines()
                .filter(line -> line.startsWith(prefix))
                .findFirst()
                .orElseThrow();
        return excerptLine.substring(prefix.length());
    }

    /**
     * Asserts that the part of {@code excerptLine} quoted from {@code sourceLine} begins and ends
     * where {@code sourceLine} has one character end and the next begin, that the line as shown
     * carries no half of a stored pair, and that it stays within the width an excerpt line is
     * allowed.
     */
    private void assertWholeCharactersOf(String excerptLine, String sourceLine) {
        String window = excerptLine.replace(ELLIPSIS, "");
        int windowStart = sourceLine.indexOf(window);
        assertTrue(windowStart >= 0, excerptLine);

        BreakIterator characters = BreakIterator.getCharacterInstance();
        characters.setText(sourceLine);
        assertTrue(characters.isBoundary(windowStart), excerptLine);
        assertTrue(characters.isBoundary(windowStart + window.length()), excerptLine);
        assertEquals(0, countUnpairedSurrogates(excerptLine), excerptLine);
        assertTrue(countCharacters(excerptLine) <= EXCERPT_MAX_CHARACTER_COUNT, excerptLine);
    }

    private int countUnpairedSurrogates(String value) {
        int unpaired = 0;
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            boolean isPairedHigh = Character.isHighSurrogate(unit)
                    && index + 1 < value.length() && Character.isLowSurrogate(value.charAt(index + 1));
            if (isPairedHigh) {
                index++;
            } else if (Character.isHighSurrogate(unit) || Character.isLowSurrogate(unit)) {
                unpaired++;
            }
        }
        return unpaired;
    }

    private int countCharacters(String value) {
        BreakIterator characters = BreakIterator.getCharacterInstance();
        characters.setText(value);
        int count = 0;
        while (characters.next() != BreakIterator.DONE) {
            count++;
        }
        return count;
    }

    private void assertConciseJsonMessage(String originalMessage, String conciseMessage) {
        JsonProcessingException exception = new JsonProcessingException(originalMessage) {};
        assertEquals("The data is not in the expected JSON format: " + conciseMessage,
                JsonParseErrorFormatter.format(exception, null).toDisplayText());
    }
}
