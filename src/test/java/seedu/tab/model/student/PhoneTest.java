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
        assertFalse(Phone.isValidPhone("91")); // lower rejected boundary: one digit below the minimum
        assertFalse(Phone.isValidPhone("phone")); // no digits at all
        assertFalse(Phone.isValidPhone("+65")); // a country code and nothing else

        // valid phone numbers
        assertTrue(Phone.isValidPhone("911")); // lower accepted boundary: exactly 3 digits
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
    public void constructor_longPhoneText_preservesCompleteValue() {
        String digits = "1234567890".repeat(40);
        String annotated = "+65 9123 4567 (HP), 6516-2727 ext 21 (Office); " + "contact note ".repeat(40).strip();

        assertTrue(Phone.isValidPhone(digits));
        assertEquals(digits, new Phone(digits).getValue());
        assertTrue(Phone.isValidPhone(annotated));
        assertEquals(annotated, new Phone(annotated).getValue());
    }

    @Test
    public void constructor_storesTheNumberTheWayItLooks() {
        // WhatsApp wraps a copied number in direction marks, which draw nothing on screen
        assertEquals("98765432", new Phone("\u202A98765432\u202C").getValue());
        assertEquals("+65 9123 4567", new Phone("\u200E+65 9123 4567").getValue());
    }

    @Test
    public void getDigits_formattedUnicodeNumber_normalizesWithoutChangingValue() {
        String formatted = "+٦٥ (９１２３)-४५६७ ext 𝟚𝟙";
        Phone phone = new Phone(formatted);

        assertEquals("659123456721", phone.getDigits());
        assertEquals(formatted, phone.getValue());
        // No decimal digits: normalization returns empty even though this is not a valid phone.
        assertEquals("", Phone.normalizeDigits("---"));
    }

    @Test
    public void isLikelySameNumber_null_throwsNullPointerException() {
        Phone phone = new Phone("87438807");

        assertThrows(NullPointerException.class, () -> phone.isLikelySameNumber(null));
    }

    @Test
    public void isLikelySameNumber_sameDigitsHoweverWritten_returnsTrue() {
        Phone phone = new Phone("8743-8807");

        assertTrue(phone.isLikelySameNumber(new Phone("87438807")));
        assertTrue(phone.isLikelySameNumber(new Phone("(8743) 8807")));
        assertTrue(phone.isLikelySameNumber(new Phone("٨٧٤٣٨٨٠٧")));
        assertTrue(new Phone("000").isLikelySameNumber(new Phone("0 0 0")));
    }

    @Test
    public void isLikelySameNumber_countryCodeInFront_returnsTrue() {
        Phone local = new Phone("87438807");

        for (String international : new String[] {"+65 8743 8807", "0065 8743 8807",
            "+65-8743-8807", "6587438807", "1 8743 8807"}) {
            assertTrue(local.isLikelySameNumber(new Phone(international)));
            assertTrue(new Phone(international).isLikelySameNumber(local));
        }
    }

    @Test
    public void isLikelySameNumber_digitsAddedAtTheEnd_returnsFalse() {
        Phone phone = new Phone("8743 8807");

        assertFalse(phone.isLikelySameNumber(new Phone("8743 8807 ext 12")));
        assertFalse(new Phone("8743 8807 ext 12").isLikelySameNumber(phone));
        assertFalse(phone.isLikelySameNumber(new Phone("874388071")));
    }

    @Test
    public void isLikelySameNumber_tailShorterThanAWholeNumber_returnsFalse() {
        assertFalse(new Phone("4567").isLikelySameNumber(new Phone("+65 9123 4567")));
        assertFalse(new Phone("+65 9123 4567").isLikelySameNumber(new Phone("4567")));
        assertFalse(new Phone("234567").isLikelySameNumber(new Phone("91234567")));
        assertTrue(new Phone("1234567").isLikelySameNumber(new Phone("91234567")));
    }

    @Test
    public void isLikelySameNumber_differentNumbersSharingATail_returnsFalse() {
        assertFalse(new Phone("61234567").isLikelySameNumber(new Phone("71234567")));
        assertFalse(new Phone("61234567").isLikelySameNumber(new Phone("+65 81234567")));
        assertFalse(new Phone("87438807").isLikelySameNumber(new Phone("+65 8743 8808")));
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
