package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_PHONE;
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
    public void getErrorMessageForMissingPrefixes_onePrefix_namesTheField() {
        assertEquals("Missing required field(s): e/EMAIL",
                Messages.getErrorMessageForMissingPrefixes(PREFIX_EMAIL));
    }

    @Test
    public void getErrorMessageForMissingPrefixes_severalPrefixes_namesEachInOrder() {
        assertEquals("Missing required field(s): n/NAME, p/PHONE, e/EMAIL",
                Messages.getErrorMessageForMissingPrefixes(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL));
    }

    @Test
    public void getErrorMessageForMissingPrefixes_noPrefixes_throwsAssertionError() {
        // naming no fields at all would leave the user with "Missing required field(s): "
        assertThrows(AssertionError.class, () -> Messages.getErrorMessageForMissingPrefixes());
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
    public void getErrorMessageForDuplicateFlags_oneFlag_namesTheField() {
        assertEquals("Multiple values specified for the following single-valued field(s): -e",
                Messages.getErrorMessageForDuplicateFlags(FLAG_EMAIL));
    }

    @Test
    public void getErrorMessageForDuplicatePrefixes_noPrefixes_throwsAssertionError() {
        // the sibling method carries the same precondition and had no test of its own
        assertThrows(AssertionError.class, () -> Messages.getErrorMessageForDuplicatePrefixes());
    }

    @Test
    public void getErrorMessageForDuplicatePrefixes_onePrefix_namesTheField() {
        assertEquals("Multiple values specified for the following single-valued field(s): e/",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_EMAIL));
    }

    @Test
    public void getErrorMessageForMissingPrefixes_repeatedPrefix_keepsBoth() {
        // the caller decides what to report; the message does not silently drop anything
        assertEquals("Missing required field(s): e/EMAIL, e/EMAIL",
                Messages.getErrorMessageForMissingPrefixes(PREFIX_EMAIL, PREFIX_EMAIL));
    }
}
