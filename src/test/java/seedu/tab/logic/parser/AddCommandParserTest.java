package seedu.tab.logic.parser;

import static seedu.tab.logic.commands.CommandTestUtil.ADD_EMAIL_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_EMAIL_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_FOLLOW_UP;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_INVALID_EMAIL;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_INVALID_NAME;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_INVALID_PHONE;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_INVALID_TAG;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_NAME_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_NAME_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_PHONE_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_TAG_FRIEND;
import static seedu.tab.logic.commands.CommandTestUtil.ADD_TAG_HUSBAND;
import static seedu.tab.logic.commands.CommandTestUtil.PREAMBLE_WHITESPACE;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_FRIEND;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.testutil.TypicalStudents.AMY;
import static seedu.tab.testutil.TypicalStudents.BOB;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.AddCommand;
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

        // leading whitespace
        assertParseSuccess(parser, PREAMBLE_WHITESPACE + ADD_NAME_BOB + ADD_PHONE_BOB + ADD_EMAIL_BOB
                + ADD_TAG_FRIEND, new AddCommand(expectedStudent));

        // multiple tags - all accepted
        Student expectedStudentMultipleTags = new StudentBuilder(BOB).withTags(VALID_TAG_FRIEND, VALID_TAG_HUSBAND)
                .build();
        assertParseSuccess(parser,
                ADD_NAME_BOB + ADD_PHONE_BOB + ADD_EMAIL_BOB + ADD_TAG_HUSBAND + ADD_TAG_FRIEND,
                new AddCommand(expectedStudentMultipleTags));
    }

    @Test
    public void parse_unquotedNameOfSeveralWords_success() {
        // quoting is only needed for a value that would otherwise be read as something else
        Student expected = new StudentBuilder(BOB).withTags().build();
        assertParseSuccess(parser, " " + BOB.getName().fullName + ADD_PHONE_BOB + ADD_EMAIL_BOB,
                new AddCommand(expected));
    }

    @Test
    public void parse_optionalFieldsMissing_success() {
        // zero tags
        Student expectedStudent = new StudentBuilder(AMY).withTags().build();
        assertParseSuccess(parser, ADD_NAME_AMY + ADD_PHONE_AMY + ADD_EMAIL_AMY,
                new AddCommand(expectedStudent));

        // no email, which is the student the optional email exists to let a tutor record
        Student withoutEmail = new StudentBuilder(AMY).withTags().withoutEmail().build();
        assertParseSuccess(parser, ADD_NAME_AMY + ADD_PHONE_AMY, new AddCommand(withoutEmail));

        // no email and tags together
        Student tagged = new StudentBuilder(AMY).withTags(VALID_TAG_FRIEND).withoutEmail().build();
        assertParseSuccess(parser, ADD_NAME_AMY + ADD_PHONE_AMY + ADD_TAG_FRIEND,
                new AddCommand(tagged));
    }

    @Test
    public void parse_followUpOptionPresent_buildsFlaggedStudent() {
        Student expected = new StudentBuilder(BOB).withTags(VALID_TAG_FRIEND).withFlag(true).build();

        assertParseSuccess(parser, ADD_NAME_BOB + ADD_FOLLOW_UP + ADD_PHONE_BOB
                + ADD_EMAIL_BOB + ADD_TAG_FRIEND, new AddCommand(expected));
        assertParseSuccess(parser, ADD_NAME_BOB + ADD_PHONE_BOB + ADD_EMAIL_BOB
                + ADD_TAG_FRIEND + ADD_FOLLOW_UP, new AddCommand(expected));
    }

    @Test
    public void parse_compulsoryFieldMissing_failure() {
        // a field is left out entirely, so that each case fails for one reason only

        // missing name
        assertParseFailure(parser, ADD_PHONE_BOB + ADD_EMAIL_BOB,
                Messages.getErrorMessageForMissingFields(AddCommand.FIELD_NAME));

        // missing phone
        assertParseFailure(parser, ADD_NAME_BOB + ADD_EMAIL_BOB,
                Messages.getErrorMessageForMissingFields(FLAG_PHONE.getLabel()));

        // several fields missing -> every one of them is named, in the order of the format
        assertParseFailure(parser, ADD_TAG_FRIEND,
                Messages.getErrorMessageForMissingFields(AddCommand.FIELD_NAME, FLAG_PHONE.getLabel()));

        // nothing at all
        assertParseFailure(parser, " ",
                Messages.getErrorMessageForMissingFields(AddCommand.FIELD_NAME, FLAG_PHONE.getLabel()));
    }

    @Test
    public void parse_invalidValue_failure() {
        // invalid name
        assertParseFailure(parser, ADD_INVALID_NAME + ADD_PHONE_BOB + ADD_EMAIL_BOB + ADD_TAG_HUSBAND,
                Messages.getErrorMessageForInvalidValue(Name.FIELD_NAME, "---", Name.getFailureReason("---")));

        // invalid phone
        assertParseFailure(parser, ADD_NAME_BOB + ADD_INVALID_PHONE + ADD_EMAIL_BOB + ADD_TAG_HUSBAND,
                Messages.getErrorMessageForInvalidValue(Phone.FIELD_NAME, "12", Phone.getFailureReason("12")));

        // invalid email
        assertParseFailure(parser, ADD_NAME_BOB + ADD_PHONE_BOB + ADD_INVALID_EMAIL + ADD_TAG_HUSBAND,
                Messages.getErrorMessageForInvalidValue(Email.FIELD_NAME, "bob!yahoo",
                        Email.getFailureReason("bob!yahoo")));

        // invalid tag
        assertParseFailure(parser, ADD_NAME_BOB + ADD_PHONE_BOB + ADD_EMAIL_BOB + ADD_INVALID_TAG + ADD_TAG_FRIEND,
                Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, "---", Tag.getFailureReason("---")));
    }

    @Test
    public void parse_severalInvalidValues_reportsEveryOne() {
        // a user who mistyped four fields learns all four at once, rather than one per attempt
        String expected = String.join("\n",
                Messages.getErrorMessageForInvalidValue(Name.FIELD_NAME, "---", Name.getFailureReason("---")),
                Messages.getErrorMessageForInvalidValue(Phone.FIELD_NAME, "12", Phone.getFailureReason("12")),
                Messages.getErrorMessageForInvalidValue(Email.FIELD_NAME, "john@x",
                        Email.getFailureReason("john@x")),
                Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, "---", Tag.getFailureReason("---")));

        assertParseFailure(parser, " \"---\" -p 12 -e john@x -t \"---\"", expected);
    }

    @Test
    public void parse_missingFieldAndInvalidValue_reportsBoth() {
        // a missing field and a bad value are different complaints and arrive together
        String expected = Messages.getErrorMessageForMissingFields(FLAG_PHONE.getLabel())
                + "\n"
                + Messages.getErrorMessageForInvalidValue(Email.FIELD_NAME, "nope",
                        Email.getFailureReason("nope"));

        assertParseFailure(parser, ADD_NAME_BOB + " -e nope", expected);
    }

    @Test
    public void parse_severalInvalidTags_reportsEveryOne() {
        String expected = Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, "---",
                        Tag.getFailureReason("---"))
                + "\n"
                + Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, "***", Tag.getFailureReason("***"));

        assertParseFailure(parser, ADD_NAME_BOB + ADD_PHONE_BOB + ADD_EMAIL_BOB + " -t \"---\" -t ***", expected);
    }

    @Test
    public void parse_missingFieldIsNotAlsoReportedAsUnparseable() {
        // a field that is absent is reported once, as missing, and not a second time as a value
        assertParseFailure(parser, ADD_NAME_BOB + ADD_EMAIL_BOB,
                Messages.getErrorMessageForMissingFields(FLAG_PHONE.getLabel()));
    }

    @Test
    public void parse_repeatedSingleValuedFlag_isRefusedOnItsOwn() {
        // the shape of the command is settled before any field is read, so the bad email
        // alongside the repeated phone is not collected with it
        assertParseFailure(parser, ADD_NAME_BOB + ADD_PHONE_AMY + ADD_PHONE_BOB + " -e nope",
                Messages.getErrorMessageForDuplicateFlags(FLAG_PHONE));

        assertParseFailure(parser, ADD_NAME_BOB + ADD_PHONE_BOB + ADD_EMAIL_AMY + ADD_EMAIL_BOB,
                Messages.getErrorMessageForDuplicateFlags(FLAG_EMAIL));

        assertParseFailure(parser, ADD_NAME_BOB + ADD_PHONE_BOB + ADD_FOLLOW_UP + ADD_FOLLOW_UP,
                Messages.getErrorMessageForDuplicateFlags(FLAG_FOLLOW_UP));
    }

    @Test
    public void parse_unknownFlag_isRefusedOnItsOwn() {
        assertParseFailure(parser, ADD_NAME_BOB + ADD_PHONE_BOB + " -z something",
                String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-z"));
    }

    @Test
    public void parse_flagWithoutValue_namesTheFieldItNeeds() {
        assertParseFailure(parser, ADD_NAME_BOB + ADD_PHONE_BOB + " -e",
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_EMAIL.getLabel()));
    }

    @Test
    public void parse_unclosedQuote_isReportedAsSuch() {
        assertParseFailure(parser, " \"Bob Choo" + ADD_PHONE_BOB,
                CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }

    @Test
    public void parse_valueAfterTheFlagsBegan_saysItBelongsToNoOption() {
        // the name needs no quotes, but it does have to be given before the options
        assertParseFailure(parser, " Ravi" + ADD_PHONE_BOB + " Kumaran",
                String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "Kumaran"));

        assertParseFailure(parser, ADD_NAME_BOB + ADD_PHONE_BOB + ADD_FOLLOW_UP + " true",
                String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "true"));
    }

    @Test
    public void parse_nonBreakingSpace_isReadAsASeparator() {
        // pasting from a web page or a chat message used to report the phone as missing, with
        // "-p 91234567" plainly there in the command
        Student expected = new StudentBuilder().withName("John Doe").withPhone(VALID_PHONE_BOB)
                .withoutEmail().withTags().build();
        assertParseSuccess(parser, " John\u00A0Doe\u00A0-p\u00A0" + VALID_PHONE_BOB,
                new AddCommand(expected));
    }

    @Test
    public void parse_nameWithSlash_succeeds() {
        // no field is marked by a slash any more, so a slash needs nothing done to it
        Student son = new StudentBuilder().withName("Ravi s/o Kumaran").withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_BOB).withTags().build();
        assertParseSuccess(parser, " \"Ravi s/o Kumaran\"" + ADD_PHONE_BOB + ADD_EMAIL_BOB,
                new AddCommand(son));

        Student daughter = new StudentBuilder().withName("Anita d/o Rajan").withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_BOB).withTags().build();
        assertParseSuccess(parser, " \"Anita d/o Rajan\"" + ADD_PHONE_BOB + ADD_EMAIL_BOB,
                new AddCommand(daughter));
    }

    @Test
    public void parse_malaysianNameWithSlash_succeeds() {
        // a/l and a/p are ordinary components of a Malaysian name
        Student son = new StudentBuilder().withName("Abdul a/l Rahman").withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_BOB).withTags().build();
        assertParseSuccess(parser, " \"Abdul a/l Rahman\"" + ADD_PHONE_BOB + ADD_EMAIL_BOB,
                new AddCommand(son));

        Student daughter = new StudentBuilder().withName("Siti a/p Ahmad").withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_BOB).withTags().build();
        assertParseSuccess(parser, " \"Siti a/p Ahmad\"" + ADD_PHONE_BOB + ADD_EMAIL_BOB,
                new AddCommand(daughter));
    }

    @Test
    public void parse_nameOpeningWithAHyphen_succeedsWhenQuoted() {
        // quoting is the whole reason the name can come first and still hold a leading hyphen
        Student expected = new StudentBuilder().withName("-Ahmad").withPhone(VALID_PHONE_BOB)
                .withoutEmail().withTags(VALID_TAG_FRIEND).build();
        assertParseSuccess(parser, " \"-Ahmad\"" + ADD_PHONE_BOB + ADD_TAG_FRIEND,
                new AddCommand(expected));
    }

    @Test
    public void parse_tagHoldingSpaces_succeedsWhenQuoted() {
        Student expected = new StudentBuilder(BOB).withTags("Lab 3").build();
        assertParseSuccess(parser, ADD_NAME_BOB + ADD_PHONE_BOB + ADD_EMAIL_BOB + " -t \"Lab 3\"",
                new AddCommand(expected));
    }
}
