package seedu.tab.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DataLoadingErrorMessageTest {

    private static final String DESCRIPTION = "The data is not in the expected JSON format at line 2: Invalid content";
    private static final String EXCERPT = "  1 | first\n> 2 | second";

    @Test
    public void of_descriptionOnly_quotesNoPartOfTheFile() {
        DataLoadingErrorMessage errorMessage = DataLoadingErrorMessage.of(DESCRIPTION);

        assertEquals(DESCRIPTION, errorMessage.description());
        assertEquals("", errorMessage.sourceExcerpt());
        assertFalse(errorMessage.hasSourceExcerpt());
        assertEquals(DESCRIPTION, errorMessage.toDisplayText());
    }

    @Test
    public void toDisplayText_withExcerpt_introducesTheQuotation() {
        DataLoadingErrorMessage errorMessage = new DataLoadingErrorMessage(DESCRIPTION, EXCERPT);

        assertTrue(errorMessage.hasSourceExcerpt());
        assertEquals(DESCRIPTION + "\nAffected source:\n" + EXCERPT, errorMessage.toDisplayText());
    }

    @Test
    public void withDescription_newDescription_keepsTheQuotation() {
        DataLoadingErrorMessage errorMessage =
                new DataLoadingErrorMessage(DESCRIPTION, EXCERPT).withDescription("Student record 2: " + DESCRIPTION);

        assertEquals("Student record 2: " + DESCRIPTION, errorMessage.description());
        assertEquals(EXCERPT, errorMessage.sourceExcerpt());
    }

    @Test
    public void constructor_nullPart_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new DataLoadingErrorMessage(null, EXCERPT));
        assertThrows(NullPointerException.class, () -> new DataLoadingErrorMessage(DESCRIPTION, null));
    }
}
