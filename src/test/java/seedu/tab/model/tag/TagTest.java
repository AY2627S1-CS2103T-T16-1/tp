package seedu.tab.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void getFailureReason_separatesEmptyFromMalformed() {
        assertEquals("a tag may not be empty", Tag.getFailureReason(""));
        assertEquals("a tag may hold letters and digits only", Tag.getFailureReason("Lab 3"));
        assertEquals("a tag may hold letters and digits only", Tag.getFailureReason("hubby*"));
    }
}
