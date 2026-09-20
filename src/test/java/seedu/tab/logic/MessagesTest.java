package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.tab.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class MessagesTest {

    @Test
    public void getErrorMessageForMissingPrefixes_onePrefix_namesTheField() {
        assertEquals("Missing required field(s): e/EMAIL",
                Messages.getErrorMessageForMissingPrefixes(PREFIX_EMAIL));
    }

    @Test
    public void getErrorMessageForMissingPrefixes_severalPrefixes_namesEachInOrder() {
        assertEquals("Missing required field(s): n/NAME, p/PHONE, e/EMAIL, a/ADDRESS",
                Messages.getErrorMessageForMissingPrefixes(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL,
                        PREFIX_ADDRESS));
    }

    @Test
    public void getErrorMessageForMissingPrefixes_noPrefixes_throwsAssertionError() {
        // naming no fields at all would leave the user with "Missing required field(s): "
        assertThrows(AssertionError.class, () -> Messages.getErrorMessageForMissingPrefixes());
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
