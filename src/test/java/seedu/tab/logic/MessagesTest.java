package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.util.ValueMessages;
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
    public void format_studentWithoutEmailOrTags_omitsBothFields() {
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withTags().withoutEmail().build();
        assertEquals("Alice Pauline; Phone: 94351253", Messages.format(student));
    }

    @Test
    public void format_studentWithoutPhone_omitsThePhoneField() {
        // spelled out rather than compared against format itself, so that "Phone: null" or a
        // stray separator is caught
        Student student = new StudentBuilder().withName("Email Only").withoutPhone()
                .withEmail("e1234567@u.nus.edu").withNusStudentId("E1234567").withTags().build();
        assertEquals("Email Only; NUS Student ID: E1234567; Email: e1234567@u.nus.edu",
                Messages.format(student));
    }

    @Test
    public void format_studentWithNameOnly_holdsJustTheName() {
        Student student = new StudentBuilder().withName("Alice Pauline").withoutPhone()
                .withoutEmail().withTags().build();
        assertEquals("Alice Pauline", Messages.format(student));
    }

    @Test
    public void format_studentWithMultipleTags_sortsTagsAlphabetically() {
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withoutEmail().withTags("urgent", "friends", "colleague").build();
        assertEquals("Alice Pauline; Phone: 94351253; Tags: [colleague][friends][urgent]",
                Messages.format(student));
    }

    @Test
    public void format_studentWithMixedCaseTags_sortsTagsAlphabeticallyIgnoringCase() {
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withoutEmail().withTags("Zebra", "apple", "Banana", "1lab").build();
        assertEquals("Alice Pauline; Phone: 94351253; Tags: [1lab][apple][Banana][Zebra]",
                Messages.format(student));
    }

    @Test
    public void format_studentWithNusStudentId_includesItBeforeContactDetails() {
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withoutEmail().withNusStudentId("A01234567X").withTags().build();

        assertEquals("Alice Pauline; NUS Student ID: A01234567X; Phone: 94351253",
                Messages.format(student));
    }

    @Test
    public void format_flaggedStudent_includesFollowUpStatus() {
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withTags("friends").withFlag(true).build();
        assertEquals("Alice Pauline; Phone: 94351253; Email: amy@gmail.com; Tags: [friends]; Needs follow-up",
                Messages.format(student));
    }

    @Test
    public void format_flaggedStudentWithoutTags_omitsTagFieldAndIncludesFollowUp() {
        Student student = new StudentBuilder().withName("Alice Pauline").withPhone("94351253")
                .withoutEmail().withTags().withFlag(true).build();
        assertEquals("Alice Pauline; Phone: 94351253; Needs follow-up", Messages.format(student));
    }

    @Test
    public void getErrorMessageForInvalidValue_shortValue_isQuotedWhole() {
        assertEquals("Email \"bob!yahoo\" is not valid: no @",
                Messages.getErrorMessageForInvalidValue("Email", "bob!yahoo", "no @"));
    }

    @Test
    public void getErrorMessageForInvalidValue_valueAtTheQuotingLimit_isQuotedWhole() {
        String value = "a".repeat(ValueMessages.MAX_QUOTED_VALUE_LENGTH);

        assertEquals("Email \"" + value + "\" is not valid: too long",
                Messages.getErrorMessageForInvalidValue("Email", value, "too long"));
    }

    @Test
    public void getErrorMessageForInvalidValue_longValue_isShortenedSoTheReasonStaysVisible() {
        // a value of several thousand characters would otherwise push the reason out of sight
        String value = "a".repeat(10000);

        String message = Messages.getErrorMessageForInvalidValue("Email", value, "too long");

        assertEquals("Email \"" + "a".repeat(ValueMessages.MAX_QUOTED_VALUE_LENGTH) + "…\" is not valid: too long",
                message);
        assertTrue(message.length() < 200, "the message is still far too long: " + message.length());
    }

    @Test
    public void shorten_longValueWithEmojiAtBoundary_keepsEmojiIntact() {
        String prefix = "a".repeat(ValueMessages.MAX_QUOTED_VALUE_LENGTH - 1);
        String value = prefix + "😀" + "more";

        assertEquals(prefix + "😀…", Messages.shorten(value));
    }

    @Test
    public void shorten_longValueWithCombiningAccentAtBoundary_keepsTheAccentOnItsLetter() {
        // a letter and the accent that follows it are two code points and one character
        String prefix = "a".repeat(ValueMessages.MAX_QUOTED_VALUE_LENGTH - 1);
        String value = prefix + "e\u0301" + "more";

        assertEquals(prefix + "e\u0301…", Messages.shorten(value));
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
        assertEquals("Options given more than once: -p -e. Give each listed option only once.",
                Messages.getErrorMessageForDuplicateFlags("-p", "-e"));
    }

    @Test
    public void getErrorMessageForDuplicateFlags_repeatedFlag_namesItOnce() {
        assertEquals("Options given more than once: -e. Give each listed option only once.",
                Messages.getErrorMessageForDuplicateFlags("-e", "-e"));
    }

    @Test
    public void getErrorMessageForDuplicateFlags_oneFlag_namesTheOption() {
        assertEquals("Options given more than once: -e. Give each listed option only once.",
                Messages.getErrorMessageForDuplicateFlags("-e"));
    }

    @Test
    public void getErrorMessageForDuplicateFlags_presenceOptions_namesOptionsRatherThanValues() {
        assertEquals("Options given more than once: -f -nf. Give each listed option only once.",
                Messages.getErrorMessageForDuplicateFlags("-f", "-nf"));
    }

    @Test
    public void getErrorMessageForMistypedCommand_oneSuggestion_namesIt() {
        assertEquals("Unknown command \"delte\". Did you mean delete?",
                Messages.getErrorMessageForMistypedCommand("delte", List.of("delete")));
    }

    @Test
    public void getErrorMessageForMistypedCommand_twoSuggestions_joinsThemWithOr() {
        assertEquals("Unknown command \"eit\". Did you mean edit or exit?",
                Messages.getErrorMessageForMistypedCommand("eit", List.of("edit", "exit")));
    }

    @Test
    public void getErrorMessageForMistypedCommand_threeSuggestions_commasAllButTheLast() {
        // the command words TAB has today cannot produce three, but the message must read as
        // a sentence whenever a later one does
        assertEquals("Unknown command \"adi\". Did you mean add, edit or exit?",
                Messages.getErrorMessageForMistypedCommand("adi", List.of("add", "edit", "exit")));
    }

    @Test
    public void getErrorMessageForMistypedCommand_noSuggestions_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> Messages.getErrorMessageForMistypedCommand("adi", List.of()));
    }

    @Test
    public void getErrorMessageForMistypedCommand_longWord_isShortenedLikeAnyOtherValue() {
        String typed = "x".repeat(ValueMessages.MAX_QUOTED_VALUE_LENGTH + 1);
        assertEquals("Unknown command \"" + "x".repeat(ValueMessages.MAX_QUOTED_VALUE_LENGTH)
                        + "…\". Did you mean add?",
                Messages.getErrorMessageForMistypedCommand(typed, List.of("add")));
    }

    @Test
    public void getErrorMessageForUnknownCommand_word_namesItAndPointsToHelp() {
        assertEquals("Unknown command \"qwerty\". Enter help to see every command.",
                Messages.getErrorMessageForUnknownCommand("qwerty"));
    }

    @Test
    public void getStudentsFoundOverview_zeroStudents_formatsPlural() {
        assertEquals("Found 0 students.", Messages.getStudentsFoundOverview(0));
    }

    @Test
    public void getStudentsFoundOverview_oneStudent_formatsSingular() {
        assertEquals("Found 1 student.", Messages.getStudentsFoundOverview(1));
    }

    @Test
    public void getStudentsFoundOverview_multipleStudents_formatsPlural() {
        assertEquals("Found 24 students.", Messages.getStudentsFoundOverview(24));
    }

}
