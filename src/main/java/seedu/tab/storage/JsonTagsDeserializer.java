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
 * Reads saved tags with field-specific diagnostics instead of Jackson type errors.
 */
class JsonTagsDeserializer extends JsonDeserializer<List<JsonAdaptedTag>> {
    private static final String MESSAGE_EXPECTED_ARRAY = "Use a JSON array of tag strings, such as [\"Lab 3\"].";
    private static final String MESSAGE_EXPECTED_STRING = "Each tag must be a JSON string, such as \"Lab 3\".";
    private static final String MESSAGE_INVALID_VALUE = "Invalid %s value: %s. %s";

    @Override
    public List<JsonAdaptedTag> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
            throw createInvalidValueException(parser, "tags", MESSAGE_EXPECTED_ARRAY);
        }
        List<JsonAdaptedTag> tags = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
                throw createInvalidValueException(parser, "tags entry " + (tags.size() + 1),
                        MESSAGE_EXPECTED_STRING);
            }
            tags.add(new JsonAdaptedTag(parser.getText()));
        }
        return tags;
    }

    /**
     * Creates a bounded tag diagnostic without exposing Jackson implementation errors.
     */
    private static JsonMappingException createInvalidValueException(JsonParser parser, String field, String reason)
            throws IOException {
        String value = JsonValueDescription.describeQuoted(parser);
        return JsonMappingException.from(parser, String.format(MESSAGE_INVALID_VALUE, field, value, reason));
    }
}
