package seedu.tab.commons.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
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
    private static final String MESSAGE_NULL_DOCUMENT = "The JSON document must not be null.";

    private static final Logger logger = LogsCenter.getLogger(JsonUtil.class);

    private static ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true)
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
     * <p>Only a file whose absence is established is reported as not found. A file whose folder
     * cannot be opened is read and the failure reported, because the caller treats a missing
     * file as a first run and would otherwise put sample data over records it could not see.
     *
     * @param filePath cannot be null.
     * @param classOfObjectToDeserialize JSON file has to correspond to the structure in the class given here.
     * @throws DataLoadingException if loading of the JSON file failed or the document is null.
     */
    public static <T> Optional<T> readJsonFile(
            Path filePath, Class<T> classOfObjectToDeserialize) throws DataLoadingException {
        requireNonNull(filePath);
        if (Files.notExists(filePath)) {
            return Optional.empty();
        }
        logger.info("JSON file " + filePath + " found.");
        return Optional.of(deserializeDocument(readJsonSource(filePath), filePath, classOfObjectToDeserialize));
    }

    private static String readJsonSource(Path filePath) throws DataLoadingException {
        try {
            return FileUtil.readFromFile(filePath);
        } catch (IOException e) {
            logger.warning("Error reading from JSON file " + filePath + ": " + e);
            throw new DataLoadingException(JsonParseErrorFormatter.formatIoError(e), e);
        }
    }

    private static <T> T deserializeDocument(String jsonSource, Path filePath, Class<T> targetClass)
            throws DataLoadingException {
        try {
            T value = fromJsonString(jsonSource, targetClass);
            if (value == null) {
                IllegalValueException cause = new IllegalValueException(MESSAGE_NULL_DOCUMENT);
                throw new DataLoadingException(cause.getMessage(), cause);
            }
            return value;
        } catch (JsonProcessingException e) {
            String errorMessage = JsonParseErrorFormatter.format(e, jsonSource);
            logger.warning("Error parsing JSON file " + filePath + ": " + errorMessage);
            throw new DataLoadingException(errorMessage, e);
        } catch (IOException e) {
            logger.warning("Error processing JSON file " + filePath + ": " + e);
            throw new DataLoadingException(JsonParseErrorFormatter.formatIoError(e), e);
        }
    }

    /**
     * Returns the deepest wrapped exception that still carries a parse location, so that the
     * reported position identifies the offending token rather than the last mapped value.
     */
    static JsonProcessingException getDetailedJsonProcessingException(JsonProcessingException exception) {
        JsonProcessingException detailedException = exception;
        while (detailedException.getCause() instanceof JsonProcessingException cause
                && (cause.getLocation() != null || detailedException.getLocation() == null)) {
            detailedException = cause;
        }
        return detailedException;
    }

    /**
     * Saves the Json object to the specified file.
     * Overwrites existing file if it exists, creates a new file if it doesn't.
     *
     * @param jsonFile cannot be null.
     * @param filePath cannot be null.
     * @throws IOException if there was an error during writing to the file.
     */
    public static <T> void saveJsonFile(T jsonFile, Path filePath) throws IOException {
        requireNonNull(filePath);
        requireNonNull(jsonFile);

        serializeObjectToJsonFile(filePath, jsonFile);
    }


    /**
     * Converts a given string representation of JSON data to an instance of a class
     *
     * @param <T> The generic type to create an instance of.
     * @param json The JSON document to read.
     * @param instanceClass The target class to deserialize.
     * @return The instance of T with the specified values in the JSON string.
     */
    public static <T> T fromJsonString(String json, Class<T> instanceClass) throws IOException {
        try (JsonParser parser = objectMapper.getFactory().createParser(json)) {
            T value = readJsonValue(parser, instanceClass);
            if (parser.nextToken() != null) {
                throw new JsonParseException(parser,
                        "Expected a single JSON value; remove content after the document.", parser.getTokenLocation());
            }
            return value;
        }
    }

    private static <T> T readJsonValue(JsonParser parser, Class<T> instanceClass) throws IOException {
        try {
            return objectMapper.readValue(parser, instanceClass);
        } catch (JsonProcessingException e) {
            JsonProcessingException detail = getDetailedJsonProcessingException(e);
            // Jackson 2.7.4 omits duplicate-key locations; retain the parser's position.
            if (detail instanceof JsonParseException && detail.getLocation() == null) {
                throw new JsonParseException(parser, detail.getOriginalMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Converts a given instance of a class into its JSON data string representation
     *
     * @param instance The T object to be converted into the JSON string.
     * @param <T> The generic type to create an instance of.
     * @return JSON data representation of the given class instance, in string.
     */
    public static <T> String toJsonString(T instance) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance);
    }

}
