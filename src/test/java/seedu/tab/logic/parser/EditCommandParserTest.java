package seedu.tab.logic.parser;

import static seedu.tab.logic.commands.CommandTestUtil.EMAIL_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.EMAIL_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.FLAG_NAME_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.FOLLOW_UP;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_EMAIL;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_FLAG_NAME;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_PHONE;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_TAG;
import static seedu.tab.logic.commands.CommandTestUtil.NO_TAGS;
import static seedu.tab.logic.commands.CommandTestUtil.PHONE_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.PREAMBLE_WHITESPACE;
import static seedu.tab.logic.commands.CommandTestUtil.TAG_FRIEND;
import static seedu.tab.logic.commands.CommandTestUtil.TAG_HUSBAND;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_EMAIL_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_NAME_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_PHONE_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_FRIEND;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static seedu.tab.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;
import static seedu.tab.testutil.TypicalIndexes.INDEX_THIRD_STUDENT;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.Phone;
import seedu.tab.model.tag.Tag;
import seedu.tab.testutil.EditStudentDescriptorBuilder;

public class EditCommandParserTest {

    private EditCommandParser parser = new EditCommandParser();

    @Test
    public void parse_allFieldsSpecified_success() {
        Index targetIndex = INDEX_SECOND_STUDENT;
        String userInput = targetIndex.getOneBased() + PHONE_BOB + TAG_HUSBAND
                + EMAIL_AMY + FLAG_NAME_AMY + TAG_FRIEND;

        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName(VALID_NAME_AMY)
                .withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_AMY)
                .withTags(VALID_TAG_HUSBAND, VALID_TAG_FRIEND).build();

        assertParseSuccess(parser, userInput, new EditCommand(targetIndex, descriptor));
    }

    @Test
    public void parse_someFieldsSpecified_success() {
        Index targetIndex = INDEX_FIRST_STUDENT;
        String userInput = PREAMBLE_WHITESPACE + targetIndex.getOneBased() + PHONE_BOB + EMAIL_AMY;

        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_AMY).build();

        assertParseSuccess(parser, userInput, new EditCommand(targetIndex, descriptor));
    }

    @Test
    public void parse_oneFieldSpecified_success() {
        Index targetIndex = INDEX_THIRD_STUDENT;

        EditStudentDescriptor name = new EditStudentDescriptorBuilder().withName(VALID_NAME_AMY).build();
        assertParseSuccess(parser, targetIndex.getOneBased() + FLAG_NAME_AMY,
                new EditCommand(targetIndex, name));

        EditStudentDescriptor phone = new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_AMY).build();
        assertParseSuccess(parser, targetIndex.getOneBased() + PHONE_AMY,
                new EditCommand(targetIndex, phone));

        EditStudentDescriptor email = new EditStudentDescriptorBuilder().withEmail(VALID_EMAIL_AMY).build();
        assertParseSuccess(parser, targetIndex.getOneBased() + EMAIL_AMY,
                new EditCommand(targetIndex, email));

        EditStudentDescriptor tag = new EditStudentDescriptorBuilder().withTags(VALID_TAG_FRIEND).build();
        assertParseSuccess(parser, targetIndex.getOneBased() + TAG_FRIEND,
                new EditCommand(targetIndex, tag));
    }

    @Test
    public void parse_nameHoldingAPrefix_succeeds() {
        // the defect this command's options exist to remove: "p/ Smith" used to be read as a
        // phone number, so a name add accepts could not be typed into edit at all
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder()
                .withName("Ravi s/o Kumaran").build();
        assertParseSuccess(parser, "1 " + FLAG_NAME + " \"Ravi s/o Kumaran\"",
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));

        EditStudentDescriptor johnSmith = new EditStudentDescriptorBuilder()
                .withName("John p/ Smith").build();
        assertParseSuccess(parser, "1 " + FLAG_NAME + " \"John p/ Smith\"",
                new EditCommand(INDEX_FIRST_STUDENT, johnSmith));
    }

    @Test
    public void parse_unquotedNameOfSeveralWords_isRefused() {
        // a name is marked by an option here rather than given before them, so it takes one
        // token and needs quotes, exactly as a tag does
        assertParseFailure(parser, "1 " + FLAG_NAME + " Amy Bee",
                String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "Bee"));
    }

    @Test
    public void parse_valueHoldingSpacesInQuotes_success() {
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder()
                .withPhone("+65 9123 4567").withTags("Lab 3").build();
        assertParseSuccess(parser, "1 " + FLAG_PHONE + " \"+65 9123 4567\" " + FLAG_TAG + " \"Lab 3\"",
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
    }

    @Test
    public void parse_escapedQuoteOrBackslashInValue_staysInTheValue() {
        EditStudentDescriptor quoted = new EditStudentDescriptorBuilder()
                .withName("Dwayne \"The Rock\" Johnson").build();
        assertParseSuccess(parser, "1 " + FLAG_NAME + " \"Dwayne \\\"The Rock\\\" Johnson\"",
                new EditCommand(INDEX_FIRST_STUDENT, quoted));

        EditStudentDescriptor backslash = new EditStudentDescriptorBuilder()
                .withTags("T1\\T2").build();
        assertParseSuccess(parser, "1 " + FLAG_TAG + " \"T1\\\\T2\"",
                new EditCommand(INDEX_FIRST_STUDENT, backslash));
    }

    @Test
    public void parse_nonBreakingSpace_isReadAsASeparator() {
        // pasting from a web page or a chat message must not report the phone as missing with
        // "-p 91234567" plainly there in the command
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder()
                .withPhone(VALID_PHONE_BOB).build();
        assertParseSuccess(parser, " 1 -p " + VALID_PHONE_BOB,
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
    }

    @Test
    public void parse_resetTags_success() {
        Index targetIndex = INDEX_THIRD_STUDENT;
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withTags().build();

        // an explicitly empty value asks for the tags to be cleared, where a bare -t is an error
        assertParseSuccess(parser, targetIndex.getOneBased() + NO_TAGS,
                new EditCommand(targetIndex, descriptor));
    }

    @Test
    public void parse_followUpAloneOrWithAnotherEdit_success() {
        EditStudentDescriptor toggleOnly = new EditStudentDescriptorBuilder()
                .withFlagToggled(true).build();
        assertParseSuccess(parser, "1" + FOLLOW_UP,
                new EditCommand(INDEX_FIRST_STUDENT, toggleOnly));

        // the short alias marks the same option
        assertParseSuccess(parser, "1 -f", new EditCommand(INDEX_FIRST_STUDENT, toggleOnly));

        EditStudentDescriptor toggleAndPhone = new EditStudentDescriptorBuilder()
                .withPhone(VALID_PHONE_BOB).withFlagToggled(true).build();
        assertParseSuccess(parser, "1" + PHONE_BOB + FOLLOW_UP,
                new EditCommand(INDEX_FIRST_STUDENT, toggleAndPhone));
    }

    @Test
    public void parse_followUpWithValue_saysItBelongsToNoOption() {
        assertParseFailure(parser, "1" + FOLLOW_UP + " true",
                String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "true"));
    }

    @Test
    public void parse_missingIndex_namesItAsMissing() {
        assertParseFailure(parser, FLAG_NAME_AMY,
                Messages.getErrorMessageForMissingFields(EditCommand.FIELD_INDEX));
    }

    @Test
    public void parse_missingIndexAndField_reportsBothAtOnce() {
        assertParseFailure(parser, "",
                Messages.getErrorMessageForMissingFields(EditCommand.FIELD_INDEX)
                        + "\n" + EditCommand.MESSAGE_NOT_EDITED);
    }

    @Test
    public void parse_noFieldSpecified_failure() {
        assertParseFailure(parser, "1", EditCommand.MESSAGE_NOT_EDITED);
    }

    @Test
    public void parse_invalidIndex_failure() {
        // zero, and anything that is not a positive integer, is not an index
        assertParseFailure(parser, "0" + FLAG_NAME_AMY, ParserUtil.MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "abc" + FLAG_NAME_AMY, ParserUtil.MESSAGE_INVALID_INDEX);

        // a token before the options that is not part of the index
        assertParseFailure(parser, "1 some random string" + FLAG_NAME_AMY,
                ParserUtil.MESSAGE_INVALID_INDEX);

        // a negative index reads as an unknown option, since -5 is shaped like one
        assertParseFailure(parser, "-5" + FLAG_NAME_AMY,
                String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-5"));
    }

    @Test
    public void parse_invalidIndexAndInvalidField_reportsBothAtOnce() {
        assertParseFailure(parser, "0" + INVALID_PHONE,
                ParserUtil.MESSAGE_INVALID_INDEX + "\n"
                        + Messages.getErrorMessageForInvalidValue("Phone", "12",
                                Phone.getFailureReason("12")));
    }

    @Test
    public void parse_invalidValue_failure() {
        assertParseFailure(parser, "1" + INVALID_FLAG_NAME,
                Messages.getErrorMessageForInvalidValue("Name", "---", Name.getFailureReason("---")));
        assertParseFailure(parser, "1" + INVALID_PHONE,
                Messages.getErrorMessageForInvalidValue("Phone", "12", Phone.getFailureReason("12")));
        assertParseFailure(parser, "1" + INVALID_EMAIL,
                Messages.getErrorMessageForInvalidValue("Email", "bob!yahoo",
                        Email.getFailureReason("bob!yahoo")));
        assertParseFailure(parser, "1" + INVALID_TAG,
                Messages.getErrorMessageForInvalidValue("Tag", "---", Tag.getFailureReason("---")));
    }

    @Test
    public void parse_severalInvalidValues_reportsThemAllAtOnce() {
        assertParseFailure(parser, "1" + INVALID_FLAG_NAME + INVALID_EMAIL,
                Messages.getErrorMessageForInvalidValue("Name", "---", Name.getFailureReason("---"))
                        + "\n" + Messages.getErrorMessageForInvalidValue("Email", "bob!yahoo",
                                Email.getFailureReason("bob!yahoo")));
    }

    @Test
    public void parse_emptyTagBesideAnotherTag_isRefused() {
        // -t "" clears the tags only on its own; together with a tag it is an empty value
        assertParseFailure(parser, "1" + TAG_FRIEND + NO_TAGS,
                Messages.getErrorMessageForInvalidValue("Tag", "", Tag.getFailureReason("")));
        assertParseFailure(parser, "1" + NO_TAGS + TAG_FRIEND,
                Messages.getErrorMessageForInvalidValue("Tag", "", Tag.getFailureReason("")));
    }

    @Test
    public void parse_repeatedSingleValuedField_failure() {
        assertParseFailure(parser, "1" + PHONE_AMY + PHONE_BOB,
                Messages.getErrorMessageForDuplicateFlags(FLAG_PHONE));

        assertParseFailure(parser, "1" + FOLLOW_UP + FOLLOW_UP,
                Messages.getErrorMessageForDuplicateFlags(FLAG_FOLLOW_UP));

        // a repeated option is refused before any field is read, since it leaves it unclear
        // which value belongs to the field
        assertParseFailure(parser, "1" + INVALID_PHONE + PHONE_BOB,
                Messages.getErrorMessageForDuplicateFlags(FLAG_PHONE));

        assertParseFailure(parser, "1" + PHONE_AMY + EMAIL_AMY + PHONE_BOB + EMAIL_BOB,
                Messages.getErrorMessageForDuplicateFlags(FLAG_PHONE, FLAG_EMAIL));
    }

    @Test
    public void parse_unknownFlag_isRefusedOnItsOwn() {
        assertParseFailure(parser, "1" + PHONE_BOB + " -z something",
                String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-z"));
    }

    @Test
    public void parse_flagWithoutValue_namesTheFieldItNeeds() {
        assertParseFailure(parser, "1" + PHONE_BOB + " -e",
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_EMAIL.getLabel()));

        // an option in the value position means the one before it was left empty, rather than
        // "-e" being stored as a tag
        assertParseFailure(parser, "1 -t -e a@b.com",
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_TAG.getLabel()));
    }

    @Test
    public void parse_unclosedQuote_isReportedAsSuch() {
        assertParseFailure(parser, "1 " + FLAG_NAME + " \"Amy Bee" + PHONE_BOB,
                CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }
}
