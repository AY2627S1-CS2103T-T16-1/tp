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
        // Empty and whitespace-only values are rejected after normalization.
        assertFalse(Tag.isValidTagName("")); // empty
        assertFalse(Tag.isValidTagName("   ")); // spaces only
        // Nonblank punctuation still supplies no letter or number and is rejected.
        assertFalse(Tag.isValidTagName("---")); // punctuation only
    }

    @Test
    public void constructor_storesTheNormalizedForm() {
        // a tag is a key in a set, so two that look identical must not both be stored
        assertEquals("Lab 3", new Tag("Lab  3").getTagName());
        assertEquals("Lab 3", new Tag(" Lab 3 ").getTagName());
        assertEquals(new Tag("Lab 3"), new Tag("Lab\u00A03"));
        assertEquals(new Tag("caf\u00E9"), new Tag("cafe\u200B\u0301"));

        // an invisible mark a paste carries must not split one tag into two lookalikes
        assertEquals(new Tag("friend"), new Tag("\u200Efriend"));
        assertEquals(new Tag("friend"), new Tag("friend\u200F"));
    }

    @Test
    public void equals_sameInstanceAndNull_obeysEqualityContract() {
        Tag tag = new Tag("Lab 3");

        assertTrue(tag.equals(tag));
        assertFalse(tag.equals(null));
    }
    @Test
    public void equals_differentValuesAndTypes_returnsFalse() {
        Tag tag = new Tag("Lab 3");

        assertFalse(tag.equals(new Tag("Lab 4")));
        assertFalse(tag.equals("Lab 3"));
        // Case variants remain distinct for Tag.equals, unlike case-insensitive command matching.
        assertFalse(tag.equals(new Tag("lab 3")));
        assertEquals(tag, new Tag("Lab 3"));
    }

    @Test
    public void displayOrder_namesDifferingInCase_ignoresCase() {
        assertTrue(Tag.DISPLAY_ORDER.compare(new Tag("apple"), new Tag("Banana")) < 0);
        assertTrue(Tag.DISPLAY_ORDER.compare(new Tag("Zebra"), new Tag("apple")) > 0);

        // digits sort before letters, so a numbered tag leads
        assertTrue(Tag.DISPLAY_ORDER.compare(new Tag("1lab"), new Tag("apple")) < 0);
    }

    @Test
    public void displayOrder_namesAlikeButForCase_ordersByTheExactName() {
        assertTrue(Tag.DISPLAY_ORDER.compare(new Tag("Lab 3"), new Tag("lab 3")) < 0);
        assertTrue(Tag.DISPLAY_ORDER.compare(new Tag("lab 3"), new Tag("Lab 3")) > 0);
    }

    @Test
    public void displayOrder_sameName_comparesEqual() {
        Tag tag = new Tag("Lab 3");

        assertEquals(0, Tag.DISPLAY_ORDER.compare(tag, tag));
        assertEquals(0, Tag.DISPLAY_ORDER.compare(tag, new Tag("Lab 3")));
    }

}
