package seedu.tab.storage;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Reads the file description without silently converting numbers or Booleans to text.
 */
class JsonCommentDeserializer extends JsonDeserializer<String> {
    private static final String MESSAGE_INVALID_COMMENT = "Invalid _comment value %s. "
            + "Use a JSON string such as \"_comment\": \"My tutorial group\", or remove the key.";

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() == JsonToken.VALUE_STRING) {
            return parser.getText();
        }
        throw JsonMappingException.from(parser,
                String.format(MESSAGE_INVALID_COMMENT, JsonValueDescription.describe(parser)));
    }
}
