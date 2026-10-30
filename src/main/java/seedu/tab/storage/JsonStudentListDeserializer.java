package seedu.tab.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Reads the saved student list with shape-specific diagnostics instead of Jackson type errors.
 */
class JsonStudentListDeserializer extends JsonDeserializer<List<JsonAdaptedStudent>> {
    private static final String MESSAGE_EXPECTED_ARRAY =
            "Use a JSON array of student records, such as [{\"name\": \"Alex Yeoh\"}].";
    private static final String MESSAGE_EXPECTED_RECORD =
            "Each student record must be a JSON object, such as {\"name\": \"Alex Yeoh\"}.";
    private static final String MESSAGE_INVALID_VALUE = "Invalid %s value: %s. %s";
    private static final String STUDENTS_FIELD = "students";

    @Override
    public List<JsonAdaptedStudent> deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
            throw createInvalidValueException(parser, STUDENTS_FIELD, MESSAGE_EXPECTED_ARRAY);
        }
        List<JsonAdaptedStudent> students = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            students.add(readStudent(parser, context, students.size()));
        }
        return students;
    }

    /**
     * Returns the record at {@code index}, leaving an entry written as null for the book itself
     * to report as a missing record.
     */
    private static JsonAdaptedStudent readStudent(JsonParser parser, DeserializationContext context, int index)
            throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token != JsonToken.START_OBJECT) {
            throw createInvalidValueException(parser, STUDENTS_FIELD + " entry " + (index + 1),
                    MESSAGE_EXPECTED_RECORD);
        }
        return readRecord(parser, context, index);
    }

    /**
     * Returns the record the entry at {@code index} holds, noting that position in whatever the
     * record's own fields turn down so that the book can still name the record number.
     */
    private static JsonAdaptedStudent readRecord(JsonParser parser, DeserializationContext context, int index)
            throws IOException {
        try {
            return context.readValue(parser, JsonAdaptedStudent.class);
        } catch (JsonMappingException e) {
            throw JsonMappingException.wrapWithPath(e, List.class, index);
        }
    }

    /**
     * Creates a bounded student list diagnostic without exposing Jackson implementation errors.
     */
    private static JsonMappingException createInvalidValueException(JsonParser parser, String field, String reason)
            throws IOException {
        String value = JsonValueDescription.describeQuoted(parser);
        return JsonMappingException.from(parser, String.format(MESSAGE_INVALID_VALUE, field, value, reason));
    }
}
