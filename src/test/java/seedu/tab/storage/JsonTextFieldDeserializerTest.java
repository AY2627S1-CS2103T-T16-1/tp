package seedu.tab.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the diagnostics given for a saved text field read outside a named property.
 */
public class JsonTextFieldDeserializerTest {

    @Test
    public void deserialize_structuredValueWithoutProperty_describesFieldGenerically() throws Exception {
        for (String json : List.of("[\"first\"]", "{\"home\":\"first\"}")) {
            String foundValue = json.startsWith("[") ? "a list" : "an object";
            JsonDeserializer<String> deserializer = new JsonTextFieldDeserializer().createContextual(null, null);
            try (JsonParser parser = new ObjectMapper().getFactory().createParser(json)) {
                parser.nextToken();

                JsonMappingException exception = assertThrows(
                        JsonMappingException.class, () -> deserializer.deserialize(parser, null));

                assertEquals("This field must be a single text value, not " + foundValue + ".",
                        exception.getOriginalMessage());
            }
        }
    }
}
