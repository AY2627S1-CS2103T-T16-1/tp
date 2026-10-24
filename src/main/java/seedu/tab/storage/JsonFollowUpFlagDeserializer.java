package seedu.tab.storage;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import seedu.tab.logic.Messages;

/** Reads a follow-up flag without silently converting numbers or strings to booleans. */
class JsonFollowUpFlagDeserializer extends JsonDeserializer<Boolean> {
    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() == JsonToken.VALUE_TRUE) {
            return true;
        }
        if (parser.getCurrentToken() == JsonToken.VALUE_FALSE) {
            return false;
        }
        String value = Messages.shorten(parser.getText());
        if (parser.getCurrentToken() == JsonToken.VALUE_STRING) {
            value = "\"" + value + "\"";
        }
        throw JsonMappingException.from(parser,
                "Invalid flag value " + value + ". Use JSON true, false or null for the follow-up flag.");
    }
}
