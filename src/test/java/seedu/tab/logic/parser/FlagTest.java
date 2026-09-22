package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FlagTest {

    @Test
    public void getLabel_joinsTheMarkerAndTheFieldName() {
        assertEquals("-e EMAIL", new Flag("-e", "EMAIL").getLabel());
    }

    @Test
    public void markerOnlyFlag_labelIsJustTheMarkerAndTakesNoValue() {
        Flag markerOnly = new Flag("-f");
        assertEquals("-f", markerOnly.getLabel());
        assertFalse(markerOnly.takesValue());
    }

    @Test
    public void valueTakingFlag_takesValue() {
        assertTrue(new Flag("-e", "EMAIL").takesValue());
    }

    @Test
    public void toString_isJustTheMarker() {
        // the marker alone is what a user typed, so it is what a message quotes back
        assertEquals("-e", new Flag("-e", "EMAIL").toString());
    }

    @Test
    public void equals_sameMarkerDifferentFieldName_returnsTrue() {
        // a flag is identified by the characters that mark it, not by what it is called,
        // so naming the fields cannot change how the tokenizer matches them
        assertEquals(new Flag("-e", "MAIL"), new Flag("-e", "EMAIL"));
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        Flag flag = new Flag("-e", "EMAIL");
        assertTrue(flag.equals(flag));
    }

    @Test
    public void equals_null_returnsFalse() {
        assertFalse(new Flag("-e", "EMAIL").equals(null));
    }

    @Test
    public void equals_differentType_returnsFalse() {
        assertFalse(new Flag("-e", "EMAIL").equals("-e"));
    }

    @Test
    public void equals_differentMarker_returnsFalse() {
        assertNotEquals(new Flag("-e", "EMAIL"), new Flag("-p", "EMAIL"));
    }

    @Test
    public void hashCode_sameMarkerDifferentFieldName_returnsSameHashCode() {
        assertEquals(new Flag("-e", "EMAIL").hashCode(), new Flag("-e", "MAIL").hashCode());
    }
}
