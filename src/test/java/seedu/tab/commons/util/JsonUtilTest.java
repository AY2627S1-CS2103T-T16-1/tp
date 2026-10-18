package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    public void deserializeObjectFromJsonFile_noExceptionThrown() throws IOException {
        FileUtil.writeToFile(SERIALIZATION_FILE, SerializableTestClass.JSON_STRING_REPRESENTATION);

        SerializableTestClass serializableTestClass = JsonUtil
                .deserializeObjectFromJsonFile(SERIALIZATION_FILE, SerializableTestClass.class);

        assertEquals(serializableTestClass.getName(), SerializableTestClass.getNameTestValue());
        assertEquals(serializableTestClass.getListOfLocalDateTimes(), SerializableTestClass.getListTestValues());
        assertEquals(serializableTestClass.getMapOfIntegerToString(), SerializableTestClass.getHashMapTestValues());
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
    public void getJsonProcessingErrorMessage_withoutLocation_omitsLocation() {
        JsonProcessingException exception = new JsonProcessingException("Invalid content without a location") {};

        assertEquals("The data is not in the expected JSON format: Invalid content without a location",
                JsonUtil.getJsonProcessingErrorMessage(exception));
    }

    @Test
    public void getIoErrorMessage_accessDenied_explainsPermissionFailure() {
        AccessDeniedException exception = new AccessDeniedException("data/tab.json");

        assertEquals("TAB does not have permission to read this file.", JsonUtil.getIoErrorMessage(exception));
    }

    @Test
    public void getIoErrorMessage_otherIoFailure_includesDetails() {
        IOException exception = new IOException("The device is unavailable");

        assertEquals("The file could not be read: The device is unavailable",
                JsonUtil.getIoErrorMessage(exception));
    }

    //TODO: @Test jsonUtil_readJsonStringToObjectInstance_correctObject()

    //TODO: @Test jsonUtil_writeThenReadObjectToJson_correctObject()
}
