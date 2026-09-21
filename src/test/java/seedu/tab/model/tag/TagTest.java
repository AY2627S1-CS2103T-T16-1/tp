package seedu.tab.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class TagTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Tag(null));
    }

    @Test
    public void constructor_invalidTagName_throwsIllegalArgumentException() {
        String invalidTagName = "";
        assertThrows(IllegalArgumentException.class, () -> new Tag(invalidTagName));
    }

    @Test
    public void isValidTagName() {
        // null tag name
        assertThrows(NullPointerException.class, () -> Tag.isValidTagName(null));
    }

    @Test
    public void getFailureReason_explainsTheOnlyWayATagCanFail() {
        assertEquals("it holds no letter or number", Tag.getFailureReason(""));
        assertEquals("it holds no letter or number", Tag.getFailureReason("---"));
    }

    @Test
    public void isValidTagName_theTagsOurSpecificationUses_returnsTrue() {
        assertTrue(Tag.isValidTagName("T1")); // a slot
        assertTrue(Tag.isValidTagName("Lab 3")); // two words, and in docs/images/Ui.png
        assertTrue(Tag.isValidTagName("Tutorial 5"));
        assertTrue(Tag.isValidTagName("AY2627 Sem 1 CS2103T T16")); // four words
        assertTrue(Tag.isValidTagName("needs-followup")); // a hyphen
        assertTrue(Tag.isValidTagName("CS2103T/T16")); // a slash
        assertTrue(Tag.isValidTagName("\u9648\u4f1f\u660e")); // a non-Latin tag
    }

    @Test
    public void isValidTagName_carriesNoIdentity_returnsFalse() {
        assertFalse(Tag.isValidTagName("")); // empty
        assertFalse(Tag.isValidTagName("   ")); // spaces only
        assertFalse(Tag.isValidTagName("---")); // punctuation only
    }

    @Test
    public void constructor_storesTheNormalizedForm() {
        // a tag is a key in a set, so two that look identical must not both be stored
        assertEquals("Lab 3", new Tag("Lab  3").tagName);
        assertEquals("Lab 3", new Tag(" Lab 3 ").tagName);
        assertEquals(new Tag("Lab 3"), new Tag("Lab\u00A03"));
        assertEquals(new Tag("caf\u00E9"), new Tag("cafe\u200B\u0301"));
    }
}
