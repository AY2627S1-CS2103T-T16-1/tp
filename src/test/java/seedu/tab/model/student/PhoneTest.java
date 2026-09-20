package seedu.tab.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class PhoneTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Phone(null));
    }

    @Test
    public void constructor_invalidPhone_throwsIllegalArgumentException() {
        String invalidPhone = "";
        assertThrows(IllegalArgumentException.class, () -> new Phone(invalidPhone));
    }

    @Test
    public void isValidPhone() {
        // null phone number
        assertThrows(NullPointerException.class, () -> Phone.isValidPhone(null));

        // too few digits to be a phone number
        assertFalse(Phone.isValidPhone("")); // empty string
        assertFalse(Phone.isValidPhone(" ")); // spaces only
        assertFalse(Phone.isValidPhone("91")); // two digits
        assertFalse(Phone.isValidPhone("phone")); // no digits at all
        assertFalse(Phone.isValidPhone("+65")); // a country code and nothing else

        // valid phone numbers
        assertTrue(Phone.isValidPhone("911")); // exactly 3 digits
        assertTrue(Phone.isValidPhone("93121534"));
        assertTrue(Phone.isValidPhone("124293842033123")); // long phone number

        // the shapes a real roster holds, which the old digits-only rule turned away
        assertTrue(Phone.isValidPhone("9312 1534")); // spaces between groups
        assertTrue(Phone.isValidPhone("+65 9123 4567")); // country code
        assertTrue(Phone.isValidPhone("+60 12 345 6789")); // a longer country code
        assertTrue(Phone.isValidPhone("6516-2727 ext 21")); // an extension
        assertTrue(Phone.isValidPhone("1234 5678 (HP) 1111-3333 (Office)")); // two numbers
    }

    @Test
    public void equals() {
        Phone phone = new Phone("999");

        // same values -> returns true
        assertTrue(phone.equals(new Phone("999")));

        // same object -> returns true
        assertTrue(phone.equals(phone));

        // null -> returns false
        assertFalse(phone.equals(null));

        // different types -> returns false
        assertFalse(phone.equals(5.0f));

        // different values -> returns false
        assertFalse(phone.equals(new Phone("995")));
    }

    @Test
    public void getFailureReason_reportsTheDigitCount() {
        assertEquals("a phone number needs at least 3 digits", Phone.getFailureReason("12"));
        assertEquals("a phone number needs at least 3 digits", Phone.getFailureReason(""));
        assertEquals("a phone number needs at least 3 digits", Phone.getFailureReason("abc"));
    }
}
