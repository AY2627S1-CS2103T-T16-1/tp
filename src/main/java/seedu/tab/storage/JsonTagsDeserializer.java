package seedu.tab.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import seedu.tab.logic.Messages;

/** Reads saved tags with field-specific diagnostics instead of Jackson type errors. */
class JsonTagsDeserializer extends JsonDeserializer<List<JsonAdaptedTag>> {
    @Override
    public List<JsonAdaptedTag> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
            throw invalidValue(parser, "tags", "Use a JSON array of tag strings, such as [\"Lab 3\"].");
        }
        List<JsonAdaptedTag> tags = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
                throw invalidValue(parser, "tags entry " + (tags.size() + 1),
                        "Each tag must be a JSON string, such as \"Lab 3\".");
            }
            tags.add(new JsonAdaptedTag(parser.getText()));
        }
        return tags;
    }

    private static JsonMappingException invalidValue(JsonParser parser, String field, String reason)
            throws IOException {
        String value = switch (parser.getCurrentToken()) {
            case START_OBJECT -> "an object";
            case START_ARRAY -> "an array";
            default -> Messages.shorten(parser.getText());
        };
        if (parser.getCurrentToken() == JsonToken.VALUE_STRING) {
            value = "\"" + value + "\"";
        }
        return JsonMappingException.from(parser, "Invalid " + field + " value: " + value + ". " + reason);
    }
}
