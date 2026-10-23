package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import seedu.tab.commons.exceptions.DataLoadingException;
import seedu.tab.testutil.SerializableTestClass;
import seedu.tab.testutil.TestUtil;

/**
 * Tests JSON Read and Write
 */
public class JsonUtilTest {

    private static final Path SERIALIZATION_FILE = TestUtil.getFilePathInSandboxFolder("serialize.json");

    @TempDir
    public Path testFolder;

    @Test
    public void serializeObjectToJsonFile_noExceptionThrown() throws IOException {
        SerializableTestClass serializableTestClass = new SerializableTestClass();
        serializableTestClass.setTestValues();

        JsonUtil.serializeObjectToJsonFile(SERIALIZATION_FILE, serializableTestClass);

        assertEquals(FileUtil.readFromFile(SERIALIZATION_FILE), SerializableTestClass.JSON_STRING_REPRESENTATION);
    }

    @Test
    public void readJsonFile_missingFile_isEmpty() throws Exception {
        assertEquals(Optional.empty(),
                JsonUtil.readJsonFile(testFolder.resolve("absent.json"), SerializableTestClass.class));
    }

    @Test
    public void readJsonFile_unopenableFolder_reportsTheFailureRatherThanAMissingFile() throws Exception {
        // a folder the account cannot open hides whether the file is there. Answering "not
        // found" would let the caller treat a loaded book as a first run and write sample
        // data over records it never read
        Path directory = Files.createDirectory(testFolder.resolve("data"));
        Path filePath = directory.resolve("tab.json");
        Files.writeString(filePath, SerializableTestClass.JSON_STRING_REPRESENTATION);
        assumePosixPermissions(directory);
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(directory);
        Files.setPosixFilePermissions(directory, Set.of());

        try {
            Assumptions.assumeFalse(Files.isReadable(filePath), "The test account can still read the file.");
            DataLoadingException exception = assertThrows(DataLoadingException.class, () ->
                    JsonUtil.readJsonFile(filePath, SerializableTestClass.class));

            assertEquals("TAB does not have permission to read this file or the folder holding it.",
                    exception.getMessage());
        } finally {
            Files.setPosixFilePermissions(directory, originalPermissions);
        }
        assertEquals(SerializableTestClass.JSON_STRING_REPRESENTATION, Files.readString(filePath));
    }

    private void assumePosixPermissions(Path file) throws IOException {
        Assumptions.assumeTrue(Files.getFileStore(file).supportsFileAttributeView("posix"),
                "POSIX permissions are unavailable.");
        Assumptions.assumeFalse("root".equals(System.getProperty("user.name")),
                "root is allowed past file permissions.");
    }

    @Test
    public void readJsonFile_nullDocument_reportsMalformedDataAndPreservesFile() throws IOException {
        Path filePath = testFolder.resolve("null.json");
        for (String contents : new String[] {"null", "  null  ", "\nnull\n"}) {
            Files.writeString(filePath, contents);

            DataLoadingException exception = assertThrows(
                    DataLoadingException.class, () -> JsonUtil.readJsonFile(filePath, SerializableTestClass.class));

            assertEquals("The JSON document must not be null.", exception.getMessage());
            assertEquals(contents, Files.readString(filePath));
        }
    }

    @Test
    public void readJsonFile_directoryInsteadOfFile_reportsReadError() {
        DataLoadingException exception = assertThrows(
                DataLoadingException.class, () -> JsonUtil.readJsonFile(testFolder, SerializableTestClass.class));

        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    public void readJsonFile_processingIoError_reportsReadError() throws IOException {
        Path filePath = testFolder.resolve("processingIoError.json");
        Files.writeString(filePath, "{}");

        DataLoadingException exception = assertThrows(
                DataLoadingException.class, () -> JsonUtil.readJsonFile(filePath, JsonWithIoFailure.class));

        assertEquals("The file could not be read: simulated processing failure", exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    public void getJsonProcessingErrorMessage_withoutLocation_omitsLocation() {
        JsonProcessingException exception = new JsonProcessingException("Invalid content without a location") {};

        assertEquals("The data is not in the expected JSON format: Invalid content without a location",
                JsonUtil.getJsonProcessingErrorMessage(exception, "source that must not be displayed"));
    }

    @Test
    public void getJsonProcessingErrorMessage_withoutSource_keepsAvailableLocation() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 2, 3);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        assertEquals("The data is not in the expected JSON format at line 2, column 3: Invalid content",
                JsonUtil.getJsonProcessingErrorMessage(exception, null));
    }

    @Test
    public void getJsonProcessingErrorMessage_invalidLine_omitsLocationAndSource() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 0, 4);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        assertEquals("The data is not in the expected JSON format: Invalid content",
                JsonUtil.getJsonProcessingErrorMessage(exception, "source that must not be displayed"));
    }

    @Test
    public void getJsonProcessingErrorMessage_unusableColumn_reportsLineOnly() {
        String expectedMessage = "The data is not in the expected JSON format at line 2: Invalid content\n"
                + "Affected source:\n"
                + "  1 | first\n"
                + "> 2 | second\n"
                + "  3 | third";
        for (int column : new int[] {0, 99}) {
            JsonLocation location = new JsonLocation("hidden source", -1, -1, 2, column);
            JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

            assertEquals(expectedMessage,
                    JsonUtil.getJsonProcessingErrorMessage(exception, "first\nsecond\nthird"));
        }
    }

    @Test
    public void getJsonProcessingErrorMessage_missingParserDetails_reportsFallback() {
        for (String originalMessage : new String[] {null, " \t "}) {
            JsonProcessingException exception = new JsonProcessingException(originalMessage) {};

            assertEquals("The data is not in the expected JSON format: "
                            + "The parser did not provide further details.",
                    JsonUtil.getJsonProcessingErrorMessage(exception, null));
        }
    }

    @Test
    public void getJsonProcessingErrorMessage_sourceDescriptions_omitsSourceDetails() {
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
    public void getJsonProcessingErrorMessage_locationBeyondSource_omitsExcerpt() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 3, 1);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        assertEquals("The data is not in the expected JSON format at the end of the file: Invalid content",
                JsonUtil.getJsonProcessingErrorMessage(exception, "only one line"));
    }

    @Test
    public void getJsonProcessingErrorMessage_locationAfterLastLine_marksLastLine() {
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 3, 1);
        JsonProcessingException exception = createJsonProcessingException("Unexpected end", location);

        String expectedMessage = "The data is not in the expected JSON format at the end of the file: Unexpected end\n"
                + "Affected source:\n"
                + "  1 | first\n"
                + "> 2 | second";
        assertEquals(expectedMessage,
                JsonUtil.getJsonProcessingErrorMessage(exception, "first\nsecond"));
    }

    @Test
    public void getJsonProcessingErrorMessage_longAffectedLineNearStart_preservesErrorArea() {
        String jsonSource = "x".repeat(200);
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 1, 1);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        String expectedMessage = "The data is not in the expected JSON format at line 1, column 1: Invalid content\n"
                + "Affected source:\n"
                + "> 1 | " + "x".repeat(154) + "...";
        assertEquals(expectedMessage, JsonUtil.getJsonProcessingErrorMessage(exception, jsonSource));
    }

    @Test
    public void readJsonFile_syntaxErrorInMultiLineJson_reportsNearbyNumberedLines() throws IOException {
        String jsonSource = """
                {
                  "first": {
                    "name": "Before"
                  },
                  "student": {
                    "name": "Alice"
                    "phone": "91234567"
                  },
                  "last": {
                    "name": "After"
                  }
                }
                """;

        String errorMessage = readMalformedJsonAndGetMessage("multiLine.json", jsonSource);

        String expectedMessage = "The data is not in the expected JSON format at line 7, column 6: "
                + "Unexpected character ('\"' (code 34)): was expecting comma to separate OBJECT entries\n"
                + "Affected source:\n"
                + "  6 |     \"name\": \"Alice\"\n"
                + "> 7 |     \"phone\": \"91234567\"\n"
                + "  8 |   },";
        assertEquals(expectedMessage, errorMessage);
        assertFalse(errorMessage.contains("Before"));
        assertFalse(errorMessage.contains("After"));
    }

    @Test
    public void readJsonFile_syntaxErrorsAtFileBoundaries_reportsAvailableLines() throws IOException {
        String startErrorMessage = readMalformedJsonAndGetMessage("start.json", "}\n\"later\": true");
        String endErrorMessage = readMalformedJsonAndGetMessage("end.json", """
                {
                  "student": {
                    "name": "Alice"
                """);

        assertTrue(startErrorMessage.contains("at line 1, column 2"));
        assertTrue(startErrorMessage.contains("> 1 | }"));
        assertTrue(startErrorMessage.contains("(for ROOT starting at line 1)"));
        assertFalse(startErrorMessage.contains("[Source:"));
        assertFalse(startErrorMessage.contains("| null"));
        assertTrue(endErrorMessage.contains("Unexpected end-of-input"));
        assertTrue(endErrorMessage.contains("(from line 2, column 14)"));
        assertFalse(endErrorMessage.contains("[Source:"));
        assertTrue(endErrorMessage.contains("at the end of the file"));
        assertTrue(endErrorMessage.contains(" 3 |     \"name\": \"Alice\""));
    }

    @Test
    public void readJsonFile_mismatchedClosingBracket_preservesStartLocation() throws IOException {
        String jsonSource = "{\n  \"tags\": [\"friends\"\n}\n";

        String errorMessage = readMalformedJsonAndGetMessage("mismatchedBracket.json", jsonSource);

        assertTrue(errorMessage.contains("Unexpected close marker '}': expected ']'"));
        assertTrue(errorMessage.contains("(for ARRAY starting at line 2, column 11)"));
        assertFalse(errorMessage.contains("[Source:"));
    }

    @Test
    public void readJsonFile_unicodeSeparatorInsideString_marksJacksonLine() throws IOException {
        String jsonSource = "{\n"
                + "  \"name\": \"Alice\u2028Pauline\",\n"
                + "  \"first\": true\n"
                + "  \"second\": false\n"
                + "}\n";

        String errorMessage = readMalformedJsonAndGetMessage("unicodeSeparator.json", jsonSource);

        assertTrue(errorMessage.contains("at line 4, column 4"));
        assertTrue(errorMessage.contains("> 4 |   \"second\": false"));
    }

    @Test
    public void getJsonProcessingErrorMessage_twoDigitLines_alignsSeparators() {
        String jsonSource = String.join("\n", "one", "two", "three", "four", "five",
                "six", "seven", "eight", "nine", "ten");
        JsonLocation location = new JsonLocation("hidden source", -1, -1, 9, 1);
        JsonProcessingException exception = createJsonProcessingException("Invalid content", location);

        String errorMessage = JsonUtil.getJsonProcessingErrorMessage(exception, jsonSource);

        assertTrue(errorMessage.contains("   8 | eight\n>  9 | nine\n  10 | ten"));
    }

    @Test
    public void readJsonFile_syntaxErrorInLongSingleLineJson_boundsExcerptAroundError() throws IOException {
        String padding = "a".repeat(500);
        String jsonSource = "{\"padding\":\"" + padding
                + "\",\"name\":\"Alice\" \"phone\":\"91234567\"}";

        String errorMessage = readMalformedJsonAndGetMessage("singleLine.json", jsonSource);

        assertTrue(errorMessage.contains("at line 1, column 531"));
        assertTrue(errorMessage.contains("> 1 | ..."));
        assertTrue(errorMessage.contains("\"name\":\"Alice\" \"phone\":\"91234567\"}"));
        assertFalse(errorMessage.contains(padding));
        assertTrue(errorMessage.length() < jsonSource.length());
    }

    @Test
    public void readJsonFile_syntaxErrorBesideLongLine_boundsContextLine() throws IOException {
        String padding = "b".repeat(500);
        String jsonSource = """
                {
                  "padding": "%s"
                  "name": "Alice"
                }
                """.formatted(padding);

        String errorMessage = readMalformedJsonAndGetMessage("longContextLine.json", jsonSource);

        String contextLine = errorMessage.lines()
                .filter(line -> line.startsWith("  2 | "))
                .findFirst()
                .orElseThrow();
        String displayedSource = contextLine.substring(contextLine.indexOf("| ") + 2);
        assertTrue(displayedSource.endsWith("..."));
        assertTrue(displayedSource.length() <= 160);
        assertFalse(errorMessage.contains(padding));
        assertTrue(errorMessage.contains("> 3 |   \"name\": \"Alice\""));
    }

    @Test
    public void getIoErrorMessage_accessDenied_explainsPermissionFailure() {
        AccessDeniedException exception = new AccessDeniedException("data/tab.json");

        assertEquals("TAB does not have permission to read this file or the folder holding it.",
                JsonUtil.getIoErrorMessage(exception));
    }

    @Test
    public void getIoErrorMessage_otherIoFailure_includesDetails() {
        IOException exception = new IOException("The device is unavailable");

        assertEquals("The file could not be read: The device is unavailable",
                JsonUtil.getIoErrorMessage(exception));
    }

    private String readMalformedJsonAndGetMessage(String fileName, String jsonSource) throws IOException {
        Path filePath = testFolder.resolve(fileName);
        FileUtil.writeToFile(filePath, jsonSource);

        DataLoadingException exception = assertThrows(
                DataLoadingException.class, () -> JsonUtil.readJsonFile(filePath, Object.class));
        return exception.getMessage();
    }

    private JsonProcessingException createJsonProcessingException(String message, JsonLocation location) {
        return new JsonProcessingException(message, location) {};
    }

    private void assertConciseJsonMessage(String originalMessage, String conciseMessage) {
        JsonProcessingException exception = new JsonProcessingException(originalMessage) {};
        assertEquals("The data is not in the expected JSON format: " + conciseMessage,
                JsonUtil.getJsonProcessingErrorMessage(exception, null));
    }

    @JsonDeserialize(using = IoExceptionThrowingDeserializer.class)
    private static class JsonWithIoFailure {
    }

    private static class IoExceptionThrowingDeserializer extends JsonDeserializer<JsonWithIoFailure> {

        @Override
        public JsonWithIoFailure deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            throw new IOException("simulated processing failure");
        }
    }

    //TODO: @Test jsonUtil_readJsonStringToObjectInstance_correctObject()

    //TODO: @Test jsonUtil_writeThenReadObjectToJson_correctObject()
}
