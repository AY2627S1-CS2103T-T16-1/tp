package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.ADDRESS_DESC_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.EMAIL_DESC_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_ADDRESS_DESC;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_EMAIL_DESC;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_NAME_DESC;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_PHONE_DESC;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_TAG_DESC;
import static seedu.tab.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.NAME_DESC_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.PHONE_DESC_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.PREAMBLE_NON_EMPTY;
import static seedu.tab.logic.commands.CommandTestUtil.PREAMBLE_WHITESPACE;
import static seedu.tab.logic.commands.CommandTestUtil.TAG_DESC_FRIEND;
import static seedu.tab.logic.commands.CommandTestUtil.TAG_DESC_HUSBAND;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_FRIEND;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_TAG;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalStudents.AMY;
import static seedu.tab.testutil.TypicalStudents.BOB;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.Address;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;
import seedu.tab.testutil.StudentBuilder;

public class AddCommandParserTest {
    private AddCommandParser parser = new AddCommandParser();

    @Test
    public void parse_allFieldsPresent_success() {
        Student expectedStudent = new StudentBuilder(BOB).withTags(VALID_TAG_FRIEND).build();

        // whitespace only preamble
        assertParseSuccess(parser, PREAMBLE_WHITESPACE + NAME_DESC_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB
                + ADDRESS_DESC_BOB + TAG_DESC_FRIEND, new AddCommand(expectedStudent));


        // multiple tags - all accepted
        Student expectedStudentMultipleTags = new StudentBuilder(BOB).withTags(VALID_TAG_FRIEND, VALID_TAG_HUSBAND)
                .build();
        assertParseSuccess(parser,
                NAME_DESC_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB + ADDRESS_DESC_BOB + TAG_DESC_HUSBAND + TAG_DESC_FRIEND,
                new AddCommand(expectedStudentMultipleTags));
    }

    @Test
    public void parse_repeatedNonTagValue_failure() {
        String validExpectedStudentString = NAME_DESC_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB
                + ADDRESS_DESC_BOB + TAG_DESC_FRIEND;

        // multiple names
        assertParseFailure(parser, NAME_DESC_AMY + validExpectedStudentString,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME));

        // multiple phones
        assertParseFailure(parser, PHONE_DESC_AMY + validExpectedStudentString,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE));

        // multiple emails
        assertParseFailure(parser, EMAIL_DESC_AMY + validExpectedStudentString,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_EMAIL));

        // multiple addresses
        assertParseFailure(parser, ADDRESS_DESC_AMY + validExpectedStudentString,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_ADDRESS));

        // multiple fields repeated
        assertParseFailure(parser,
                validExpectedStudentString + PHONE_DESC_AMY + EMAIL_DESC_AMY + NAME_DESC_AMY + ADDRESS_DESC_AMY
                        + validExpectedStudentString,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME, PREFIX_ADDRESS, PREFIX_EMAIL, PREFIX_PHONE));

        // invalid value followed by valid value

        // invalid name
        assertParseFailure(parser, INVALID_NAME_DESC + validExpectedStudentString,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME));

        // invalid email
        assertParseFailure(parser, INVALID_EMAIL_DESC + validExpectedStudentString,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_EMAIL));

        // invalid phone
        assertParseFailure(parser, INVALID_PHONE_DESC + validExpectedStudentString,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE));

        // invalid address
        assertParseFailure(parser, INVALID_ADDRESS_DESC + validExpectedStudentString,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_ADDRESS));

        // valid value followed by invalid value

        // invalid name
        assertParseFailure(parser, validExpectedStudentString + INVALID_NAME_DESC,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_NAME));

        // invalid email
        assertParseFailure(parser, validExpectedStudentString + INVALID_EMAIL_DESC,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_EMAIL));

        // invalid phone
        assertParseFailure(parser, validExpectedStudentString + INVALID_PHONE_DESC,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE));

        // invalid address
        assertParseFailure(parser, validExpectedStudentString + INVALID_ADDRESS_DESC,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_ADDRESS));
    }

    @Test
    public void parse_optionalFieldsMissing_success() {
        // zero tags
        Student expectedStudent = new StudentBuilder(AMY).withTags().build();
        assertParseSuccess(parser, NAME_DESC_AMY + PHONE_DESC_AMY + EMAIL_DESC_AMY + ADDRESS_DESC_AMY,
                new AddCommand(expectedStudent));
    }

    @Test
    public void parse_compulsoryFieldMissing_failure() {
        // a field is left out entirely, so that each case fails for one reason only

        // missing name
        assertParseFailure(parser, PHONE_DESC_BOB + EMAIL_DESC_BOB + ADDRESS_DESC_BOB,
                Messages.getErrorMessageForMissingPrefixes(PREFIX_NAME));

        // missing phone
        assertParseFailure(parser, NAME_DESC_BOB + EMAIL_DESC_BOB + ADDRESS_DESC_BOB,
                Messages.getErrorMessageForMissingPrefixes(PREFIX_PHONE));

        // missing email
        assertParseFailure(parser, NAME_DESC_BOB + PHONE_DESC_BOB + ADDRESS_DESC_BOB,
                Messages.getErrorMessageForMissingPrefixes(PREFIX_EMAIL));

        // missing address
        assertParseFailure(parser, NAME_DESC_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB,
                Messages.getErrorMessageForMissingPrefixes(PREFIX_ADDRESS));

        // several fields missing -> every one of them is named, in the order of the format
        assertParseFailure(parser, NAME_DESC_BOB + ADDRESS_DESC_BOB,
                Messages.getErrorMessageForMissingPrefixes(PREFIX_PHONE, PREFIX_EMAIL));

        // nothing at all
        assertParseFailure(parser, " ",
                Messages.getErrorMessageForMissingPrefixes(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL,
                        PREFIX_ADDRESS));
    }

    @Test
    public void parse_valueBeforeTheFirstPrefix_failsAsAFormatError() {
        // text before the first prefix is not a missing field; the command itself is malformed
        assertParseFailure(parser, VALID_NAME_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB + ADDRESS_DESC_BOB,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_invalidValue_failure() {
        // invalid name
        assertParseFailure(parser, INVALID_NAME_DESC + PHONE_DESC_BOB + EMAIL_DESC_BOB + ADDRESS_DESC_BOB
                + TAG_DESC_HUSBAND + TAG_DESC_FRIEND,
                        Messages.getErrorMessageForInvalidValue("Name",
                                "---", Name.getFailureReason("---")));

        // invalid phone
        assertParseFailure(parser, NAME_DESC_BOB + INVALID_PHONE_DESC + EMAIL_DESC_BOB + ADDRESS_DESC_BOB
                + TAG_DESC_HUSBAND + TAG_DESC_FRIEND,
                        Messages.getErrorMessageForInvalidValue("Phone",
                                "12", Phone.getFailureReason("12")));

        // invalid email
        assertParseFailure(parser, NAME_DESC_BOB + PHONE_DESC_BOB + INVALID_EMAIL_DESC + ADDRESS_DESC_BOB
                + TAG_DESC_HUSBAND + TAG_DESC_FRIEND, Messages.getErrorMessageForInvalidValue("Email", "bob!yahoo",
                        Email.getFailureReason("bob!yahoo")));

        // invalid address
        assertParseFailure(parser, NAME_DESC_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB + INVALID_ADDRESS_DESC
                + TAG_DESC_HUSBAND + TAG_DESC_FRIEND,
                        Messages.getErrorMessageForInvalidValue("Address",
                                "", Address.getFailureReason("")));

        // invalid tag
        assertParseFailure(parser, NAME_DESC_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB + ADDRESS_DESC_BOB
                + INVALID_TAG_DESC + TAG_DESC_FRIEND,
                Messages.getErrorMessageForInvalidValue("Tag", "---", Tag.getFailureReason("---")));

        // two invalid values, both reported
        assertParseFailure(parser, INVALID_NAME_DESC + PHONE_DESC_BOB + EMAIL_DESC_BOB + INVALID_ADDRESS_DESC,
                Messages.getErrorMessageForInvalidValue(Name.FIELD_NAME, "---", Name.getFailureReason("---"))
                        + "\n"
                        + Messages.getErrorMessageForInvalidValue(Address.FIELD_NAME, "",
                                Address.getFailureReason("")));

        // non-empty preamble
        assertParseFailure(parser, PREAMBLE_NON_EMPTY + NAME_DESC_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB
                + ADDRESS_DESC_BOB + TAG_DESC_HUSBAND + TAG_DESC_FRIEND,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_nameWithSlash_succeeds() {
        // s/ is not a command prefix, so these names survive tokenizing end to end
        Student expected = new StudentBuilder().withName("Ravi s/o Kumaran").withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_BOB).withAddress(VALID_ADDRESS_BOB).withTags().build();
        assertParseSuccess(parser, " " + PREFIX_NAME + "Ravi s/o Kumaran" + PHONE_DESC_BOB
                + EMAIL_DESC_BOB + ADDRESS_DESC_BOB, new AddCommand(expected));

        Student daughter = new StudentBuilder().withName("Anita d/o Rajan").withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_BOB).withAddress(VALID_ADDRESS_BOB).withTags().build();
        assertParseSuccess(parser, " " + PREFIX_NAME + "Anita d/o Rajan" + PHONE_DESC_BOB
                + EMAIL_DESC_BOB + ADDRESS_DESC_BOB, new AddCommand(daughter));
    }

    @Test
    public void parse_nameContainingACommandPrefix_failsUntilTheTokenizerIsReplaced() {
        // Malaysian names use A/L and A/P, which the tokenizer reads as the address prefix. The
        // model accepts these names; the prefix syntax is what rejects them. Pinned here so that
        // replacing the tokenizer with positional arguments and flags is seen to fix it.
        assertTrue(Name.isValidName("Abdul a/l Rahman"));

        String command = " " + PREFIX_NAME + "Abdul a/l Rahman" + PHONE_DESC_BOB + EMAIL_DESC_BOB
                + ADDRESS_DESC_BOB;
        assertThrows(ParseException.class, () -> parser.parse(command));
    }

    @Test
    public void parse_severalInvalidValues_reportsEveryOne() {
        // the point of the change: a user who mistyped four fields learns all four at once,
        // rather than one per attempt
        String expected = String.join("\n",
                Messages.getErrorMessageForInvalidValue(Name.FIELD_NAME, "---", Name.getFailureReason("---")),
                Messages.getErrorMessageForInvalidValue(Phone.FIELD_NAME, "12", Phone.getFailureReason("12")),
                Messages.getErrorMessageForInvalidValue(Email.FIELD_NAME, "john@x",
                        Email.getFailureReason("john@x")),
                Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, "---", Tag.getFailureReason("---")));

        assertParseFailure(parser, " " + PREFIX_NAME + "---" + " " + PREFIX_PHONE + "12"
                + " " + PREFIX_EMAIL + "john@x" + ADDRESS_DESC_BOB + " " + PREFIX_TAG + "---", expected);
    }

    @Test
    public void parse_missingFieldAndInvalidValue_reportsBoth() {
        // a missing field and a bad value are different complaints and arrive together
        String expected = Messages.getErrorMessageForMissingPrefixes(PREFIX_EMAIL)
                + "\n"
                + Messages.getErrorMessageForInvalidValue(Phone.FIELD_NAME, "12", Phone.getFailureReason("12"));

        assertParseFailure(parser, NAME_DESC_BOB + " " + PREFIX_PHONE + "12" + ADDRESS_DESC_BOB, expected);
    }

    @Test
    public void parse_severalInvalidTags_reportsEveryOne() {
        String expected = Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, "---",
                        Tag.getFailureReason("---"))
                + "\n"
                + Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, "***", Tag.getFailureReason("***"));

        assertParseFailure(parser, NAME_DESC_BOB + PHONE_DESC_BOB + EMAIL_DESC_BOB + ADDRESS_DESC_BOB
                + " " + PREFIX_TAG + "---" + " " + PREFIX_TAG + "***", expected);
    }

    @Test
    public void parse_valueBeforeTheFirstPrefix_isRefusedOnItsOwn() {
        // the shape of the command is settled before any field is read, so a bad phone
        // alongside a preamble is not collected with it
        assertParseFailure(parser, PREAMBLE_NON_EMPTY + NAME_DESC_BOB + " " + PREFIX_PHONE + "12"
                        + EMAIL_DESC_BOB + ADDRESS_DESC_BOB,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_repeatedSingleValuedPrefix_isRefusedOnItsOwn() {
        assertParseFailure(parser, NAME_DESC_BOB + PHONE_DESC_AMY + PHONE_DESC_BOB
                        + " " + PREFIX_EMAIL + "nope" + ADDRESS_DESC_BOB,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE));
    }

    @Test
    public void parse_missingFieldIsNotAlsoReportedAsUnparseable() {
        // a field that is absent is reported once, as missing, and not a second time as a value
        assertParseFailure(parser, NAME_DESC_BOB + PHONE_DESC_BOB + ADDRESS_DESC_BOB,
                Messages.getErrorMessageForMissingPrefixes(PREFIX_EMAIL));
    }
}
