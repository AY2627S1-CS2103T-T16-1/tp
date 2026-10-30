package seedu.tab.commons.util;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AccessDeniedException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;

import seedu.tab.commons.core.DataLoadingErrorMessage;

/**
 * Turns a failure to read a JSON file into an explanation a person can act on.
 */
class JsonParseErrorFormatter {

    private static final int JSON_ERROR_CONTEXT_LINE_COUNT = 1;
    private static final int JSON_ERROR_MAX_LINE_LENGTH = 160;
    private static final String JSON_ERROR_ELLIPSIS = "…";
    private static final int JSON_ERROR_ELLIPSIS_LENGTH =
            JSON_ERROR_ELLIPSIS.codePointCount(0, JSON_ERROR_ELLIPSIS.length());
    private static final String JSON_ERROR_LINE_SEPARATOR_PATTERN = "\\r\\n|\\r|\\n";
    private static final String JSON_ERROR_SOURCE_MARKER = "[Source:";
    private static final String JSON_ERROR_COMMENT_MARKER = "Feature 'ALLOW_COMMENTS'";
    private static final String JSON_ERROR_NO_CONTENT_MARKER = "No content to map";
    private static final String MESSAGE_COMMENTS_NOT_ALLOWED = "A JSON file cannot hold comments."
            + " Remove this one, or move its text into the optional top-level \"_comment\" field.";
    private static final String MESSAGE_FILE_HAS_NO_DATA = "The file holds no data."
            + " Restore your backup, or delete the file and restart TAB to start again from the sample students.";
    private static final String JSON_ERROR_SOURCE_LOCATION_PREFIX = "; line: ";
    private static final Pattern JSON_ERROR_SOURCE_LOCATION_PATTERN =
            Pattern.compile("; line: (\\d+), column: (\\d+)\\]");

    private JsonParseErrorFormatter() {} // this class only holds helpers

    /**
     * Returns an explanation of the given parse failure, naming the offending location and
     * quoting the affected part of {@code jsonSource} when the parser reports a usable one.
     *
     * @param exception The parsing failure to explain.
     * @param jsonSource the JSON text that failed to parse, or null when it is unavailable.
     */
    static DataLoadingErrorMessage format(JsonProcessingException exception, String jsonSource) {
        JsonProcessingException detailedException = JsonUtil.getDetailedJsonProcessingException(exception);
        JsonLocation location = detailedException.getLocation();
        String message = "The data is not in the expected JSON format"
                + getJsonLocationMessage(location, jsonSource) + ": "
                + getConciseJsonErrorMessage(detailedException);
        return new DataLoadingErrorMessage(message, getJsonSourceExcerpt(jsonSource, location));
    }

    /**
     * Returns an explanation of the given read failure, with recovery guidance where the
     * cause is one the reader can put right.
     */
    static String formatIoError(IOException exception) {
        if (exception instanceof CharacterCodingException) {
            return "The file is not valid UTF-8. Reopen it with its original encoding and save it as UTF-8,"
                    + " then restart TAB.";
        }
        if (exception instanceof AccessDeniedException) {
            return "TAB does not have permission to read this file or the folder holding it.";
        }
        return "The file could not be read: " + exception.getMessage();
    }

    private static String getJsonLocationMessage(JsonLocation location, String jsonSource) {
        if (location == null || location.getLineNr() < 1 || isBlankSource(jsonSource)) {
            return "";
        }
        if (location.getColumnNr() < 1) {
            return String.format(Locale.ROOT, " at line %d", location.getLineNr());
        }

        if (jsonSource != null) {
            String[] sourceLines = splitJsonSourceIntoLines(jsonSource);
            int lineIndex = location.getLineNr() - 1;
            if (lineIndex >= sourceLines.length
                    || (lineIndex == sourceLines.length - 1
                            && location.getColumnNr() > sourceLines[lineIndex].length() + 1)) {
                return " at the end of the file";
            }
            if (location.getColumnNr() > sourceLines[lineIndex].length() + 1) {
                return String.format(Locale.ROOT, " at line %d", location.getLineNr());
            }
        }
        return String.format(Locale.ROOT, " at line %d, column %d", location.getLineNr(), location.getColumnNr());
    }

    private static String getConciseJsonErrorMessage(JsonProcessingException exception) {
        String originalMessage = exception.getOriginalMessage();
        if (originalMessage == null || originalMessage.isBlank()) {
            return "The parser did not provide further details.";
        }

        Optional<String> plainMessage = getPlainJsonErrorMessage(originalMessage);
        if (plainMessage.isPresent()) {
            return plainMessage.get();
        }

        int sourceMarkerIndex = originalMessage.indexOf(JSON_ERROR_SOURCE_MARKER);
        if (sourceMarkerIndex < 0) {
            return originalMessage.strip();
        }

        Optional<String> messageWithLocation = replaceJsonSourceWithLocation(originalMessage, sourceMarkerIndex);
        if (messageWithLocation.isPresent()) {
            return messageWithLocation.get().strip();
        }

        int sourceContextIndex = getJsonSourceContextStart(originalMessage, sourceMarkerIndex);
        return originalMessage.substring(0, sourceContextIndex).strip();
    }

    /**
     * Returns a plainly worded explanation of the parser complaints whose own wording names
     * Java types or parser features the reader of a data file has no use for.
     */
    private static Optional<String> getPlainJsonErrorMessage(String originalMessage) {
        if (originalMessage.contains(JSON_ERROR_COMMENT_MARKER)) {
            return Optional.of(MESSAGE_COMMENTS_NOT_ALLOWED);
        }
        if (originalMessage.contains(JSON_ERROR_NO_CONTENT_MARKER)) {
            return Optional.of(MESSAGE_FILE_HAS_NO_DATA);
        }
        return Optional.empty();
    }

    private static Optional<String> replaceJsonSourceWithLocation(String originalMessage, int sourceMarkerIndex) {
        int locationSuffixIndex = originalMessage.lastIndexOf(JSON_ERROR_SOURCE_LOCATION_PREFIX);
        if (locationSuffixIndex < sourceMarkerIndex) {
            return Optional.empty();
        }

        Matcher matcher = JSON_ERROR_SOURCE_LOCATION_PATTERN.matcher(originalMessage);
        matcher.region(locationSuffixIndex, originalMessage.length());
        if (!matcher.lookingAt()) {
            return Optional.empty();
        }

        int column = Integer.parseInt(matcher.group(2));
        String safeLocation = column < 1
                ? String.format(Locale.ROOT, "line %s", matcher.group(1))
                : String.format(Locale.ROOT, "line %s, column %d", matcher.group(1), column);
        return Optional.of(originalMessage.substring(0, sourceMarkerIndex)
                + safeLocation + originalMessage.substring(matcher.end()));
    }

    private static int getJsonSourceContextStart(String originalMessage, int sourceMarkerIndex) {
        int openingParenthesisIndex = originalMessage.lastIndexOf('(', sourceMarkerIndex);
        if (openingParenthesisIndex >= 0) {
            return openingParenthesisIndex;
        }

        int precedingAtIndex = originalMessage.lastIndexOf(" at ", sourceMarkerIndex);
        if (precedingAtIndex >= 0
                && originalMessage.substring(precedingAtIndex + " at ".length(), sourceMarkerIndex).isBlank()) {
            return precedingAtIndex;
        }
        return sourceMarkerIndex;
    }

    private static String getJsonSourceExcerpt(String jsonSource, JsonLocation location) {
        if (jsonSource == null || isBlankSource(jsonSource) || location == null || location.getLineNr() < 1) {
            return "";
        }

        String[] sourceLines = splitJsonSourceIntoLines(jsonSource);
        int requestedLineIndex = location.getLineNr() - 1;
        if (requestedLineIndex > sourceLines.length) {
            return "";
        }

        int affectedLineIndex = Math.min(requestedLineIndex, sourceLines.length - 1);
        int affectedColumn = requestedLineIndex == affectedLineIndex
                ? location.getColumnNr()
                : sourceLines[affectedLineIndex].length() + 1;
        int firstLineIndex = Math.max(0, affectedLineIndex - JSON_ERROR_CONTEXT_LINE_COUNT);
        int lastLineIndex = Math.min(sourceLines.length - 1,
                affectedLineIndex + JSON_ERROR_CONTEXT_LINE_COUNT);
        return formatJsonSourceLines(sourceLines, firstLineIndex, lastLineIndex,
                affectedLineIndex, affectedColumn);
    }

    /**
     * Returns whether the file held no JSON at all, leaving no position to report and
     * nothing worth quoting back to the reader.
     */
    private static boolean isBlankSource(String jsonSource) {
        return jsonSource != null && jsonSource.isBlank();
    }

    private static String[] splitJsonSourceIntoLines(String jsonSource) {
        return jsonSource.split(JSON_ERROR_LINE_SEPARATOR_PATTERN, -1);
    }

    private static String formatJsonSourceLines(String[] sourceLines, int firstLineIndex,
                                                int lastLineIndex, int affectedLineIndex,
                                                int affectedColumn) {
        StringBuilder excerpt = new StringBuilder();
        int lineNumberWidth = Integer.toString(lastLineIndex + 1).length();
        for (int lineIndex = firstLineIndex; lineIndex <= lastLineIndex; lineIndex++) {
            if (!excerpt.isEmpty()) {
                excerpt.append('\n');
            }
            boolean isAffectedLine = lineIndex == affectedLineIndex;
            String marker = isAffectedLine ? ">" : " ";
            String line = truncateJsonSourceLine(sourceLines[lineIndex], affectedColumn, isAffectedLine);
            excerpt.append(String.format(Locale.ROOT, "%s %" + lineNumberWidth + "d | %s",
                    marker, lineIndex + 1, line));
        }
        return excerpt.toString();
    }

    /**
     * Returns as much of {@code sourceLine} as an excerpt line is allowed to show: the part
     * around {@code affectedColumn} when the line is the affected one, its opening otherwise,
     * with each end that something was dropped from marked.
     *
     * <p>The allowance is counted in characters as a reader counts them, and every cut falls
     * where one character ends and the next begins. Cutting at a stored index would halve an
     * emoji or separate a letter from its accent, leaving the excerpt showing a character no
     * font can draw.
     */
    private static String truncateJsonSourceLine(String sourceLine, int affectedColumn,
                                                 boolean isAffectedLine) {
        String withinAllowance = StringUtil.getFirstCharacters(sourceLine, JSON_ERROR_MAX_LINE_LENGTH);
        if (withinAllowance.equals(sourceLine)) {
            return sourceLine;
        }
        if (!isAffectedLine) {
            return StringUtil.getFirstCharacters(sourceLine,
                    JSON_ERROR_MAX_LINE_LENGTH - JSON_ERROR_ELLIPSIS_LENGTH) + JSON_ERROR_ELLIPSIS;
        }
        return cropAroundAffectedColumn(sourceLine, affectedColumn);
    }

    /**
     * Returns the stretch of {@code sourceLine} that surrounds {@code affectedColumn}, widened to
     * the whole allowance when the column sits too near an end for the mistake to be centred.
     */
    private static String cropAroundAffectedColumn(String sourceLine, int affectedColumn) {
        int windowLength = JSON_ERROR_MAX_LINE_LENGTH - 2 * JSON_ERROR_ELLIPSIS_LENGTH;
        int affectedIndex = Math.max(0, Math.min(affectedColumn - 1, sourceLine.length()));
        String beforeColumn = sourceLine.substring(0, affectedIndex);
        String fromColumn = sourceLine.substring(affectedIndex);
        String throughWindowEnd = beforeColumn
                + StringUtil.getFirstCharacters(fromColumn, windowLength / 2);

        // A mistake near the start of the line has little before it to show, so the width that
        // leaves unused goes to the text after the mistake rather than staying empty.
        String lineOpening = StringUtil.getFirstCharacters(sourceLine, windowLength);
        if (lineOpening.length() > throughWindowEnd.length()) {
            throughWindowEnd = lineOpening;
        }

        String window = StringUtil.getLastCharacters(throughWindowEnd, windowLength);
        String leadingEllipsis = window.equals(throughWindowEnd) ? "" : JSON_ERROR_ELLIPSIS;
        String trailingEllipsis = throughWindowEnd.equals(sourceLine) ? "" : JSON_ERROR_ELLIPSIS;
        return leadingEllipsis + window + trailingEllipsis;
    }
}
