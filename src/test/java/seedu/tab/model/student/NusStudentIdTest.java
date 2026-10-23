package seedu.tab.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class NusStudentIdTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new NusStudentId(null));
    }

    @Test
    public void constructor_invalidNusStudentId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new NusStudentId("A0123-456X"));
    }

    @Test
    public void constructor_lowercaseLetters_normalizesToUppercase() {
        assertEquals("A01234567X", new NusStudentId("a01234567x").value);
    }

    @Test
    public void isValidNusStudentId() {
        assertThrows(NullPointerException.class, () -> NusStudentId.isValidNusStudentId(null));

        assertFalse(NusStudentId.isValidNusStudentId(""));
        assertFalse(NusStudentId.isValidNusStudentId("A0123-456X"));
        assertFalse(NusStudentId.isValidNusStudentId("A0123_456X"));
        assertFalse(NusStudentId.isValidNusStudentId("A0123!456X"));
        assertFalse(NusStudentId.isValidNusStudentId("?1234567X"));
        assertFalse(NusStudentId.isValidNusStudentId("A".repeat(NusStudentId.MAX_LENGTH + 1)));

        assertTrue(NusStudentId.isValidNusStudentId("A01234567X"));
        assertTrue(NusStudentId.isValidNusStudentId("E0123456"));
        assertTrue(NusStudentId.isValidNusStudentId("a76543210y"));
        assertTrue(NusStudentId.isValidNusStudentId("A"));
        assertTrue(NusStudentId.isValidNusStudentId("1"));
        assertTrue(NusStudentId.isValidNusStudentId("A".repeat(NusStudentId.MAX_LENGTH)));
    }

    @Test
    public void isValidNusStudentId_surroundingWhitespaceOrInvisibleMarks_isAccepted() {
        // an ID pasted from a web page carries marks the reader cannot see, so the value is
        // normalized before it is judged, as names, phones and tags already are
        assertTrue(NusStudentId.isValidNusStudentId(" A01234567X ")); // leading and trailing space
        assertTrue(NusStudentId.isValidNusStudentId("\u200EA01234567X")); // leading direction mark
        assertEquals("A01234567X", new NusStudentId("\u200Ea01234567x").value);
    }

    @Test
    public void getFailureReason_invalidValues_namesTheActualProblem() {
        assertEquals(NusStudentId.MESSAGE_EMPTY, NusStudentId.getFailureReason(""));
        assertEquals(NusStudentId.MESSAGE_NON_ALPHANUMERIC,
                NusStudentId.getFailureReason("A0123-456X"));
        assertEquals(NusStudentId.MESSAGE_TOO_LONG,
                NusStudentId.getFailureReason("A".repeat(NusStudentId.MAX_LENGTH + 1)));
        assertEquals("", NusStudentId.getFailureReason("E0123456"));
    }

    @Test
    public void isValidNusStudentId_veryLongAlphanumericValue_rejectsWithoutStackOverflow() {
        assertFalse(NusStudentId.isValidNusStudentId("A".repeat(100_000)));
    }

    @Test
    public void equals() {
        NusStudentId nusStudentId = new NusStudentId("A01234567X");

        assertTrue(nusStudentId.equals(new NusStudentId("a01234567x")));
        assertTrue(nusStudentId.equals(nusStudentId));
        assertFalse(nusStudentId.equals(null));
        assertFalse(nusStudentId.equals("A01234567X"));
        assertFalse(nusStudentId.equals(new NusStudentId("A76543210Y")));
    }
}
