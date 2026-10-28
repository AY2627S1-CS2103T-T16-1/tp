package seedu.tab.storage;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

/**
 * Reads a saved text field, naming the field and its rule instead of letting a Jackson
 * type error reach the reader when the value is a list or an object.
 */
class JsonTextFieldDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {
    private static final String MESSAGE_EXPECTED_TEXT = "%s must be a single text value, not %s.";
    private static final String UNNAMED_FIELD = "This field";

    private final String fieldName;

    JsonTextFieldDeserializer() {
        this(UNNAMED_FIELD);
    }

    private JsonTextFieldDeserializer(String fieldName) {
        this.fieldName = fieldName;
    }

    /** Remembers the field this instance reads, so that its diagnostics can name it. */
    @Override
    public JsonDeserializer<String> createContextual(DeserializationContext context, BeanProperty property) {
        return property == null ? this : new JsonTextFieldDeserializer(property.getName());
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token == JsonToken.START_ARRAY) {
            throw createExpectedTextException(parser, "a list");
        }
        if (token == JsonToken.START_OBJECT) {
            throw createExpectedTextException(parser, "an object");
        }
        return parser.getValueAsString();
    }

    private JsonMappingException createExpectedTextException(JsonParser parser, String foundValue) {
        return JsonMappingException.from(parser, String.format(MESSAGE_EXPECTED_TEXT, fieldName, foundValue));
    }
}
