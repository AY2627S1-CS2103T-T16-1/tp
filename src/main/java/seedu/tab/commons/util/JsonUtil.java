package seedu.tab.commons.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.commons.exceptions.IllegalValueException;

/**
 * Converts a Java object instance to JSON and vice versa
 */
public class JsonUtil {

    private static final int JSON_ERROR_CONTEXT_LINE_COUNT = 1;
    private static final int JSON_ERROR_MAX_LINE_LENGTH = 160;
    private static final String JSON_ERROR_ELLIPSIS = "...";
    private static final String JSON_ERROR_LINE_SEPARATOR_PATTERN = "\\r\\n|\\r|\\n";
    private static final String JSON_ERROR_SOURCE_MARKER = "[Source:";
    private static final String JSON_ERROR_SOURCE_LOCATION_PREFIX = "; line: ";
    private static final Pattern JSON_ERROR_SOURCE_LOCATION_PATTERN =
            Pattern.compile("; line: (\\d+), column: (\\d+)\\]");
    private static final Logger logger = LogsCenter.getLogger(JsonUtil.class);

    private static ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    private JsonUtil() {} // this class only holds helpers

    static <T> void serializeObjectToJsonFile(Path jsonFile, T objectToSerialize) throws IOException {
        FileUtil.writeToFile(jsonFile, toJsonString(objectToSerialize));
    }

    /**
     * Returns the JSON object from the given file or {@code Optional.empty()} object if the file is not found.
     * If any values are missing from the file, default values will be used, as long as the file is a valid JSON file.
     *
     * @param filePath cannot be null.
     * @param classOfObjectToDeserialize JSON file has to correspond to the structure in the class given here.
     * @throws DataLoadingException if loading of the JSON file failed or the document is null.
     */
    public static <T> Optional<T> readJsonFile(
            Path filePath, Class<T> classOfObjectToDeserialize) throws DataLoadingException {
        requireNonNull(filePath);

        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        logger.info("JSON file " + filePath + " found.");

        String jsonSource;
        try {
            jsonSource = FileUtil.readFromFile(filePath);
        } catch (IOException e) {
            logger.warning("Error reading from JSON file " + filePath + ": " + e);
            throw new DataLoadingException(getIoErrorMessage(e), e);
        }

        try {
            T jsonFile = fromJsonString(jsonSource, classOfObjectToDeserialize);
            if (jsonFile == null) {
                IllegalValueException cause = new IllegalValueException("The JSON document must not be null.");
                throw new DataLoadingException(cause.getMessage(), cause);
            }
            return Optional.of(jsonFile);
        } catch (JsonProcessingException e) {
            String errorMessage = getJsonProcessingErrorMessage(e, jsonSource);
            logger.warning("Error parsing JSON file " + filePath + ": " + errorMessage);
            throw new DataLoadingException(errorMessage, e);
        } catch (IOException e) {
            logger.warning("Error processing JSON file " + filePath + ": " + e);
            throw new DataLoadingException(getIoErrorMessage(e), e);
        }
    }

    static String getJsonProcessingErrorMessage(JsonProcessingException exception, String jsonSource) {
        JsonProcessingException detailedException = getDetailedJsonProcessingException(exception);
        JsonLocation location = detailedException.getLocation();
        String message = "The data is not in the expected JSON format"
                + getJsonLocationMessage(location, jsonSource) + ": "
                + getConciseJsonErrorMessage(detailedException);
        String sourceExcerpt = getJsonSourceExcerpt(jsonSource, location);
        return sourceExcerpt.isEmpty() ? message : message + "\nAffected source:\n" + sourceExcerpt;
    }

    private static JsonProcessingException getDetailedJsonProcessingException(JsonProcessingException exception) {
        JsonProcessingException detailedException = exception;
        while (detailedException.getCause() instanceof JsonProcessingException) {
            detailedException = (JsonProcessingException) detailedException.getCause();
        }
        return detailedException;
    }

    private static String getJsonLocationMessage(JsonLocation location, String jsonSource) {
        if (location == null || location.getLineNr() < 1) {
            return "";
        }
        if (location.getColumnNr() < 1) {
            return String.format(" at line %d", location.getLineNr());
        }

        if (jsonSource != null) {
            String[] sourceLines = splitJsonSourceIntoLines(jsonSource);
            int lineIndex = location.getLineNr() - 1;
            if (lineIndex >= sourceLines.length
                    || lineIndex == sourceLines.length - 1
                    && location.getColumnNr() > sourceLines[lineIndex].length() + 1) {
                return " at the end of the file";
            }
            if (location.getColumnNr() > sourceLines[lineIndex].length() + 1) {
                return String.format(" at line %d", location.getLineNr());
            }
        }
        return String.format(" at line %d, column %d", location.getLineNr(), location.getColumnNr());
    }

    private static String getConciseJsonErrorMessage(JsonProcessingException exception) {
        String originalMessage = exception.getOriginalMessage();
        if (originalMessage == null || originalMessage.isBlank()) {
            return "The parser did not provide further details.";
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
                ? String.format("line %s", matcher.group(1))
                : String.format("line %s, column %d", matcher.group(1), column);
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
        if (jsonSource == null || location == null || location.getLineNr() < 1) {
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
            excerpt.append(String.format("%s %" + lineNumberWidth + "d | %s",
                    marker, lineIndex + 1, line));
        }
        return excerpt.toString();
    }

    private static String truncateJsonSourceLine(String sourceLine, int affectedColumn,
                                                 boolean isAffectedLine) {
        if (sourceLine.length() <= JSON_ERROR_MAX_LINE_LENGTH) {
            return sourceLine;
        }
        if (!isAffectedLine) {
            return sourceLine.substring(0, JSON_ERROR_MAX_LINE_LENGTH - JSON_ERROR_ELLIPSIS.length())
                    + JSON_ERROR_ELLIPSIS;
        }

        int windowLength = JSON_ERROR_MAX_LINE_LENGTH - 2 * JSON_ERROR_ELLIPSIS.length();
        int affectedIndex = Math.max(0, Math.min(affectedColumn - 1, sourceLine.length() - 1));
        int windowStart = Math.max(0, Math.min(affectedIndex - windowLength / 2,
                sourceLine.length() - windowLength));
        int windowEnd = Math.min(sourceLine.length(), windowStart + windowLength);
        String leadingEllipsis = windowStart == 0 ? "" : JSON_ERROR_ELLIPSIS;
        String trailingEllipsis = windowEnd == sourceLine.length() ? "" : JSON_ERROR_ELLIPSIS;
        return leadingEllipsis + sourceLine.substring(windowStart, windowEnd) + trailingEllipsis;
    }

    static String getIoErrorMessage(IOException exception) {
        if (exception instanceof CharacterCodingException) {
            return "The file is not valid UTF-8. Reopen it with its original encoding and save it as UTF-8,"
                    + " then restart TAB.";
        }
        if (exception instanceof AccessDeniedException) {
            return "TAB does not have permission to read this file.";
        }
        return "The file could not be read: " + exception.getMessage();
    }

    /**
     * Saves the Json object to the specified file.
     * Overwrites existing file if it exists, creates a new file if it doesn't.
     * @param jsonFile cannot be null
     * @param filePath cannot be null
     * @throws IOException if there was an error during writing to the file
     */
    public static <T> void saveJsonFile(T jsonFile, Path filePath) throws IOException {
        requireNonNull(filePath);
        requireNonNull(jsonFile);

        serializeObjectToJsonFile(filePath, jsonFile);
    }


    /**
     * Converts a given string representation of JSON data to an instance of a class
     * @param <T> The generic type to create an instance of
     * @return The instance of T with the specified values in the JSON string
     */
    public static <T> T fromJsonString(String json, Class<T> instanceClass) throws IOException {
        return objectMapper.readValue(json, instanceClass);
    }

    /**
     * Converts a given instance of a class into its JSON data string representation
     * @param instance The T object to be converted into the JSON string
     * @param <T> The generic type to create an instance of
     * @return JSON data representation of the given class instance, in string
     */
    public static <T> String toJsonString(T instance) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance);
    }

}
