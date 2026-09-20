package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class PrefixTest {

    @Test
    public void getLabel_namedPrefix_joinsTheMarkerAndTheFieldName() {
        assertEquals("e/EMAIL", new Prefix("e/", "EMAIL").getLabel());
    }

    @Test
    public void getLabel_unnamedPrefix_isJustTheMarker() {
        assertEquals("e/", new Prefix("e/").getLabel());
    }

    @Test
    public void equals_sameMarkerDifferentFieldName_returnsTrue() {
        // a prefix is identified by the characters that mark it, not by what it is called,
        // so naming the fields cannot change how the tokenizer matches them
        assertEquals(new Prefix("e/"), new Prefix("e/", "EMAIL"));
    }

    @Test
    public void hashCode_sameMarkerDifferentFieldName_returnsSameHashCode() {
        assertEquals(new Prefix("e/", "EMAIL").hashCode(), new Prefix("e/", "MAIL").hashCode());
    }

    @Test
    public void equals_differentMarker_returnsFalse() {
        assertNotEquals(new Prefix("e/", "EMAIL"), new Prefix("a/", "EMAIL"));
    }
}
