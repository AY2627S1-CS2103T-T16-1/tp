package seedu.tab.storage;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import seedu.tab.commons.util.ValueMessages;

/**
 * Names the saved value a deserializer turned down, bounding it rather than quoting a whole
 * object or array back at the reader of the file.
 */
final class JsonValueDescription {

    private JsonValueDescription() {} // this class only holds helpers

    /**
     * Returns a bounded description of the value {@code parser} is reading, naming an object or
     * an array rather than quoting the whole of it back.
     */
    static String describe(JsonParser parser) throws IOException {
        return switch (parser.getCurrentToken()) {
            case START_OBJECT -> "an object";
            case START_ARRAY -> "an array";
            default -> ValueMessages.shorten(parser.getText());
        };
    }

    /**
     * Returns {@link #describe(JsonParser)} with a text value in the quotes the file spells it
     * with, so that a value such as {@code "5"} is told apart from the number 5.
     */
    static String describeQuoted(JsonParser parser) throws IOException {
        String description = describe(parser);
        return parser.getCurrentToken() == JsonToken.VALUE_STRING ? "\"" + description + "\"" : description;
    }
}
