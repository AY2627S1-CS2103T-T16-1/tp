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
        assertThrows(IllegalArgumentException.class, () -> new NusStudentId("A0123456X"));
    }

    @Test
    public void constructor_lowercaseLetters_normalizesToUppercase() {
        assertEquals("A01234567X", new NusStudentId("a01234567x").value);
    }

    @Test
    public void isValidNusStudentId() {
        assertThrows(NullPointerException.class, () -> NusStudentId.isValidNusStudentId(null));

        assertFalse(NusStudentId.isValidNusStudentId(""));
        assertFalse(NusStudentId.isValidNusStudentId("A0123456X"));
        assertFalse(NusStudentId.isValidNusStudentId("A012345678X"));
        assertFalse(NusStudentId.isValidNusStudentId("a01234567x7X"));
        assertFalse(NusStudentId.isValidNusStudentId("A01234X67Y"));
        assertFalse(NusStudentId.isValidNusStudentId("A012345678"));
        assertFalse(NusStudentId.isValidNusStudentId(" A01234567X "));

        assertTrue(NusStudentId.isValidNusStudentId("A01234567X"));
        assertTrue(NusStudentId.isValidNusStudentId("a76543210y"));
    }

    @Test
    public void getFailureReason_invalidValues_namesTheActualProblem() {
        assertEquals("a NUS Student ID cannot be empty", NusStudentId.getFailureReason(""));
        assertEquals("a NUS Student ID needs to start with A",
                NusStudentId.getFailureReason("E01234567X"));
        assertEquals("a NUS Student ID needs 8 digits and a final letter after A",
                NusStudentId.getFailureReason("A"));
        assertEquals("a NUS Student ID needs to end with a letter from A to Z",
                NusStudentId.getFailureReason("A012345678"));
        assertEquals("the characters between the first A and final letter must all be digits",
                NusStudentId.getFailureReason("A01234X67Y"));
        assertEquals("a NUS Student ID needs exactly 8 digits between the first A and final letter",
                NusStudentId.getFailureReason("A0123456X"));
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
