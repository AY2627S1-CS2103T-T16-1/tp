package seedu.tab.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class EmailTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Email(null));
    }

    @Test
    public void constructor_invalidEmail_throwsIllegalArgumentException() {
        String invalidEmail = "";
        assertThrows(IllegalArgumentException.class, () -> new Email(invalidEmail));
    }

    @Test
    public void isValidEmail() {
        // null email
        assertThrows(NullPointerException.class, () -> Email.isValidEmail(null));

        // blank email
        assertFalse(Email.isValidEmail("")); // empty string
        assertFalse(Email.isValidEmail(" ")); // spaces only

        // missing parts
        assertFalse(Email.isValidEmail("@example.com")); // missing local part
        assertFalse(Email.isValidEmail("peterjackexample.com")); // missing '@' symbol
        assertFalse(Email.isValidEmail("peterjack@")); // missing domain name

        // invalid parts
        assertFalse(Email.isValidEmail("peterjack@-")); // invalid domain name
        assertFalse(Email.isValidEmail("peterjack@exam_ple.com")); // underscore in domain name
        assertFalse(Email.isValidEmail("peter jack@example.com")); // spaces in local part
        assertFalse(Email.isValidEmail("peterjack@exam ple.com")); // spaces in domain name
        assertFalse(Email.isValidEmail(" peterjack@example.com")); // leading space
        assertFalse(Email.isValidEmail("peterjack@example.com ")); // trailing space
        assertFalse(Email.isValidEmail("peterjack@@example.com")); // double '@' symbol
        assertFalse(Email.isValidEmail("peter@jack@example.com")); // '@' symbol in local part
        assertFalse(Email.isValidEmail("-peterjack@example.com")); // local part starts with a hyphen
        assertFalse(Email.isValidEmail("peterjack-@example.com")); // local part ends with a hyphen
        assertFalse(Email.isValidEmail("peter..jack@example.com")); // local part has two consecutive periods
        assertFalse(Email.isValidEmail("peterjack@example@com")); // '@' symbol in domain name
        assertFalse(Email.isValidEmail("peterjack@.example.com")); // domain name starts with a period
        assertFalse(Email.isValidEmail("peterjack@example.com.")); // domain name ends with a period
        assertFalse(Email.isValidEmail("peterjack@-example.com")); // domain name starts with a hyphen
        assertFalse(Email.isValidEmail("peterjack@example.com-")); // domain name ends with a hyphen
        assertFalse(Email.isValidEmail("peterjack@example.c")); // top level domain has less than two chars

        // valid email
        assertTrue(Email.isValidEmail("PeterJack_1190@example.com")); // underscore in local part
        assertTrue(Email.isValidEmail("PeterJack.1190@example.com")); // period in local part
        assertTrue(Email.isValidEmail("PeterJack+1190@example.com")); // '+' symbol in local part
        assertTrue(Email.isValidEmail("PeterJack-1190@example.com")); // hyphen in local part
        assertTrue(Email.isValidEmail("a@bc")); // minimal
        assertTrue(Email.isValidEmail("test@localhost")); // alphabets only
        assertTrue(Email.isValidEmail("123@145")); // numeric local part and domain name
        assertTrue(Email.isValidEmail("a1+be.d@example1.com")); // mixture of alphanumeric and special characters
        assertTrue(Email.isValidEmail("peter_jack@very-very-very-long-example.com")); // long domain name
        assertTrue(Email.isValidEmail("if.you.dream.it_you.can.do.it@example.com")); // long local part
        assertTrue(Email.isValidEmail("e1234567@u.nus.edu")); // more than one period in domain
    }

    @Test
    public void isWithinLengthLimit_atTheLimit_isAccepted() {
        String atLimit = addressOfLength(Email.MAX_LENGTH);

        assertEquals(Email.MAX_LENGTH, atLimit.length());
        assertTrue(Email.isWithinLengthLimit(atLimit));
        assertTrue(Email.isValidEmail(atLimit));
    }

    @Test
    public void isWithinLengthLimit_oneCharacterOver_isRejected() {
        String overLimit = addressOfLength(Email.MAX_LENGTH + 1);

        assertEquals(Email.MAX_LENGTH + 1, overLimit.length());
        assertFalse(Email.isWithinLengthLimit(overLimit));
    }

    @Test
    public void isValidEmail_addressOverTheLengthLimit_isStillStructurallyValid() {
        // the limit belongs to what a command may enter, not to the field itself. Narrowing
        // isValidEmail would make a file an earlier version wrote unloadable, and storage
        // validates with it, so a record the product had already accepted would be discarded.
        String overLimit = addressOfLength(Email.MAX_LENGTH + 1);

        assertTrue(Email.isValidEmail(overLimit));
    }

    @Test
    public void getFailureReason_addressLongEnoughToOverflowTheStack_namesTheLength() {
        // getFailureReason matches the local part on its own, so it is guarded by the same
        // length branch and must answer without exhausting the stack
        String overflowing = "a.".repeat(5000) + "a@example.com";

        String reason = Email.getFailureReason(overflowing);

        assertTrue(reason.contains(String.valueOf(Email.MAX_LENGTH)), reason);
        assertTrue(reason.contains(String.valueOf(overflowing.length())), reason);
    }

    /**
     * Returns a valid-looking address of exactly {@code length} characters, padded in the local
     * part so that only the length is at fault.
     */
    private static String addressOfLength(int length) {
        String domain = "@example.com";
        return "a".repeat(length - domain.length()) + domain;
    }

    @Test
    public void equals() {
        Email email = new Email("valid@email");

        // same values -> returns true
        assertTrue(email.equals(new Email("valid@email")));

        // same object -> returns true
        assertTrue(email.equals(email));

        // null -> returns false
        assertFalse(email.equals(null));

        // different types -> returns false
        assertFalse(email.equals(5.0f));

        // different values -> returns false
        assertFalse(email.equals(new Email("other.valid@email")));
    }

    @Test
    public void getFailureReason_namesThePartAtFault() {
        assertEquals("an email needs an @ between the local part and the domain",
                Email.getFailureReason("e1234567"));
        assertEquals("an email may hold only one @", Email.getFailureReason("a@@b.com"));
        assertEquals("there is nothing before the @", Email.getFailureReason("@u.nus.edu"));
        assertEquals("there is nothing after the @", Email.getFailureReason("john@"));
        assertEquals("the last part of the domain needs at least 2 characters",
                Email.getFailureReason("john@x"));
        assertEquals("the part before the @ may hold letters and digits, joined by any of +_.-",
                Email.getFailureReason("john doe@x.com"));
        assertEquals("the domain may hold letters and digits in labels separated by dots, "
                + "with hyphens allowed inside a label", Email.getFailureReason("john@x_y.com"));
    }
}
