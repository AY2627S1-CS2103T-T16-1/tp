package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class MessagesTest {

    @Test
    public void format_studentWithEmail_holdsEveryField() {
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withEmail("alice@example.com").withTags("friends").build();
        assertEquals("Alice Pauline; Phone: 94351253; Email: alice@example.com; Tags: [friends]",
                Messages.format(student));
    }

    @Test
    public void format_studentWithoutEmail_omitsTheEmailField() {
        // spelled out rather than compared against format itself, so that "Email: null" or a
        // stray separator is caught
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withTags("friends").withoutEmail().build();
        assertEquals("Alice Pauline; Phone: 94351253; Tags: [friends]", Messages.format(student));
    }

    @Test
    public void format_studentWithoutEmailOrTags_endsAtTheTagLabel() {
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withTags().withoutEmail().build();
        assertEquals("Alice Pauline; Phone: 94351253; Tags: ", Messages.format(student));
    }

    @Test
    public void format_flaggedStudent_includesFollowUpStatus() {
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withTags("friends").withFlag(true).build();
        assertEquals("Alice Pauline; Phone: 94351253; Email: amy@gmail.com; Tags: [friends]; Needs follow-up",
                Messages.format(student));
    }

    @Test
    public void getErrorMessageForInvalidValue_shortValue_isQuotedWhole() {
        assertEquals("Email \"bob!yahoo\" is not valid: no @",
                Messages.getErrorMessageForInvalidValue("Email", "bob!yahoo", "no @"));
    }

    @Test
    public void getErrorMessageForInvalidValue_valueAtTheQuotingLimit_isQuotedWhole() {
        String value = "a".repeat(Messages.MAX_QUOTED_VALUE_LENGTH);

        assertEquals("Email \"" + value + "\" is not valid: too long",
                Messages.getErrorMessageForInvalidValue("Email", value, "too long"));
    }

    @Test
    public void getErrorMessageForInvalidValue_longValue_isShortenedSoTheReasonStaysVisible() {
        // a value of several thousand characters would otherwise push the reason out of sight
        String value = "a".repeat(10000);

        String message = Messages.getErrorMessageForInvalidValue("Email", value, "too long");

        assertEquals("Email \"" + "a".repeat(Messages.MAX_QUOTED_VALUE_LENGTH) + "…\" is not valid: too long",
                message);
        assertTrue(message.length() < 200, "the message is still far too long: " + message.length());
    }

    @Test
    public void getErrorMessageForMissingFields_noFields_throwsAssertionError() {
        // naming no fields at all would leave the user with "Missing required field(s): "
        assertThrows(AssertionError.class, () -> Messages.getErrorMessageForMissingFields());
    }

    @Test
    public void getErrorMessageForMissingFields_severalFields_namesEachInOrder() {
        assertEquals("Missing required field(s): NAME, -p PHONE",
                Messages.getErrorMessageForMissingFields("NAME", FLAG_PHONE.getLabel()));
    }

    @Test
    public void getErrorMessageForDuplicateFlags_noFlags_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> Messages.getErrorMessageForDuplicateFlags());
    }

    @Test
    public void getErrorMessageForDuplicateFlags_severalFlags_namesEachInTheOrderGiven() {
        // a set would let the order vary between runs, which makes the message unpredictable
        assertEquals("Multiple values specified for the following single-valued field(s): -p -e",
                Messages.getErrorMessageForDuplicateFlags(FLAG_PHONE, FLAG_EMAIL));
    }

    @Test
    public void getErrorMessageForDuplicateFlags_repeatedFlag_namesItOnce() {
        assertEquals("Multiple values specified for the following single-valued field(s): -e",
                Messages.getErrorMessageForDuplicateFlags(FLAG_EMAIL, FLAG_EMAIL));
    }

    @Test
    public void getErrorMessageForDuplicateFlags_oneFlag_namesTheField() {
        assertEquals("Multiple values specified for the following single-valued field(s): -e",
                Messages.getErrorMessageForDuplicateFlags(FLAG_EMAIL));
    }

}
