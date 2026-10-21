package seedu.tab.logic.parser;

import static seedu.tab.logic.commands.CommandTestUtil.EMAIL_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.EMAIL_AT_MAX_LENGTH;
import static seedu.tab.logic.commands.CommandTestUtil.EMAIL_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.EMAIL_OVER_MAX_LENGTH;
import static seedu.tab.logic.commands.CommandTestUtil.FLAG_NAME_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.FOLLOW_UP;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_EMAIL;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_FLAG_NAME;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_NUS_STUDENT_ID;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_PHONE;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_TAG;
import static seedu.tab.logic.commands.CommandTestUtil.MESSAGE_EMAIL_OVER_MAX_LENGTH;
import static seedu.tab.logic.commands.CommandTestUtil.NO_TAGS;
import static seedu.tab.logic.commands.CommandTestUtil.NUS_STUDENT_ID_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.PHONE_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.PREAMBLE_WHITESPACE;
import static seedu.tab.logic.commands.CommandTestUtil.TAG_FRIEND;
import static seedu.tab.logic.commands.CommandTestUtil.TAG_HUSBAND;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_EMAIL_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_EMAIL_AT_MAX_LENGTH;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_NAME_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_NUS_STUDENT_ID_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_PHONE_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_FRIEND;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static seedu.tab.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;
import static seedu.tab.testutil.TypicalIndexes.INDEX_THIRD_STUDENT;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.tag.Tag;
import seedu.tab.testutil.EditStudentDescriptorBuilder;

public class EditCommandParserTest {

    private EditCommandParser parser = new EditCommandParser();

    @Test
    public void parse_allFieldsSpecified_success() {
        Index targetIndex = INDEX_SECOND_STUDENT;
        String userInput = targetIndex.getOneBased() + PHONE_BOB + TAG_HUSBAND
                + EMAIL_AMY + NUS_STUDENT_ID_AMY + FLAG_NAME_AMY + TAG_FRIEND;

        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName(VALID_NAME_AMY)
                .withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_AMY)
                .withNusStudentId(VALID_NUS_STUDENT_ID_AMY)
                .withTags(VALID_TAG_HUSBAND, VALID_TAG_FRIEND).build();

        assertParseSuccess(parser, userInput, new EditCommand(targetIndex, descriptor));
    }

    @Test
    public void parse_longAndMixedAliases_success() {
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName(VALID_NAME_AMY)
                .withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_AMY)
                .withTags(VALID_TAG_HUSBAND, VALID_TAG_FRIEND).build();

        assertParseSuccess(parser, "2 --name \"" + VALID_NAME_AMY + "\""
                + " --phone " + VALID_PHONE_BOB
                + " --email " + VALID_EMAIL_AMY
                + " --tag " + VALID_TAG_HUSBAND
                + " -t " + VALID_TAG_FRIEND,
                new EditCommand(INDEX_SECOND_STUDENT, descriptor));
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

        EditStudentDescriptor nusStudentId = new EditStudentDescriptorBuilder()
                .withNusStudentId(VALID_NUS_STUDENT_ID_AMY).build();
        assertParseSuccess(parser, targetIndex.getOneBased() + NUS_STUDENT_ID_AMY,
                new EditCommand(targetIndex, nusStudentId));

        EditStudentDescriptor tag = new EditStudentDescriptorBuilder().withTags(VALID_TAG_FRIEND).build();
        assertParseSuccess(parser, targetIndex.getOneBased() + TAG_FRIEND,
                new EditCommand(targetIndex, tag));
    }

    @Test
    public void parse_nusStudentIdTarget_success() {
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_AMY).build();
        assertParseSuccess(parser, "A01234567X" + PHONE_AMY,
                new EditCommand(StudentTarget.byNusStudentId(new NusStudentId("A01234567X")), descriptor));
    }

    @Test
    public void parse_lowerCaseNusStudentIdTarget_usesNormalizedId() {
        // NusStudentId normalizes its value, so a case variant names the same student
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_AMY).build();
        assertParseSuccess(parser, "a01234567x" + PHONE_AMY,
                new EditCommand(StudentTarget.byNusStudentId(new NusStudentId("A01234567X")), descriptor));
    }

    @Test
    public void parse_idTargetWithIdOption_changesTheIdOfTheStudentFoundById() {
        // --id still names the field to change, so the old ID is the target and the new
        // ID is the value
        EditStudentDescriptor descriptor = new EditStudentDescriptor();
        descriptor.setNusStudentId(new NusStudentId("A7654321Y"));
        assertParseSuccess(parser, "A01234567X --id A7654321Y",
                new EditCommand(StudentTarget.byNusStudentId(new NusStudentId("A01234567X")), descriptor));
    }

    @Test
    public void parse_nusStudentIdUsingLongOptionOrAlias_setsNormalizedId() {
        EditStudentDescriptor descriptor = new EditStudentDescriptor();
        descriptor.setNusStudentId(new NusStudentId("A01234567X"));

        assertParseSuccess(parser, "1 --id a01234567x",
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
        assertParseSuccess(parser, "1 -i A01234567X",
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
    }

    @Test
    public void parse_emptyOrWhitespaceOnlyNusStudentId_clearsIt() {
        EditStudentDescriptor descriptor = new EditStudentDescriptor();
        descriptor.clearNusStudentId();

        assertParseSuccess(parser, "1 --id \"\"",
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
        assertParseSuccess(parser, "1 -i \"\"",
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
        assertParseSuccess(parser, "1 --id \"   \"",
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
    }

    @Test
    public void parse_quotedEmptyValuesJoinedByEquals_clearOptionalFieldsAndTags() {
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder()
                .withoutEmail().withoutNusStudentId().withTags().build();

        assertParseSuccess(parser, "1 --email=\"\" -i=\"\" --tag=\"\"",
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
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
    public void parse_clearEmailAloneOrWithOtherFields_success() {
        EditStudentDescriptor cleared = new EditStudentDescriptorBuilder().withoutEmail().build();
        assertParseSuccess(parser, "1 -e \"\"", new EditCommand(INDEX_FIRST_STUDENT, cleared));
        assertParseSuccess(parser, "1 --email \"\"", new EditCommand(INDEX_FIRST_STUDENT, cleared));
        assertParseSuccess(parser, "1 -e \"   \"", new EditCommand(INDEX_FIRST_STUDENT, cleared));
        assertParseSuccess(parser, "\u00A01\u00A0-e\u00A0\"\"\u00A0",
                new EditCommand(INDEX_FIRST_STUDENT, cleared));
        EditStudentDescriptor combined = new EditStudentDescriptorBuilder().withoutEmail()
                .withPhone(VALID_PHONE_BOB).withTags().withFlag(true).build();
        assertParseSuccess(parser, "1 -e \"\" -p " + VALID_PHONE_BOB + " -t \"\" -f",
                new EditCommand(INDEX_FIRST_STUDENT, combined));
    }

    @Test
    public void parse_mistypedEmailClear_reportsSpecificProblem() {
        for (String input : List.of("1 -e", "1 -e ", "1 -e\u00A0", "1 -e -p 91234567")) {
            assertParseFailure(parser, input,
                    String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_EMAIL.getLabel()));
        }
        assertParseFailure(parser, "1 -e \"\" -e a@b.com",
                Messages.getErrorMessageForDuplicateFlags("-e"));
        assertParseFailure(parser, "1 -n \"\" -e \"\"",
                Messages.getErrorMessageForInvalidValue("Name", "", Name.getFailureReason("")));
        assertParseFailure(parser, "1 -p \"\" -e \"\"",
                Messages.getErrorMessageForInvalidValue("Phone", "", Phone.getFailureReason("")));
    }

    @Test
    public void parse_resetTags_success() {
        Index targetIndex = INDEX_THIRD_STUDENT;
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withTags().build();

        // an explicitly empty value asks for the tags to be cleared, where a bare -t is an error
        assertParseSuccess(parser, targetIndex.getOneBased() + NO_TAGS,
                new EditCommand(targetIndex, descriptor));
        assertParseSuccess(parser, targetIndex.getOneBased() + " --tag \"\"",
                new EditCommand(targetIndex, descriptor));
    }

    @Test
    public void parse_theSameTagGivenTwice_keepsOneTag() {
        // naming a tag twice asks for one tag rather than for two copies of it
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withTags(VALID_TAG_FRIEND).build();

        assertParseSuccess(parser, "1" + TAG_FRIEND + TAG_FRIEND,
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
    }

    @Test
    public void parse_followUpAloneOrWithAnotherEdit_success() {
        EditStudentDescriptor flagOnly = new EditStudentDescriptorBuilder()
                .withFlag(true).build();
        assertParseSuccess(parser, "1" + FOLLOW_UP,
                new EditCommand(INDEX_FIRST_STUDENT, flagOnly));

        // the short alias marks the same option
        assertParseSuccess(parser, "1 -f", new EditCommand(INDEX_FIRST_STUDENT, flagOnly));

        EditStudentDescriptor flagAndPhone = new EditStudentDescriptorBuilder()
                .withPhone(VALID_PHONE_BOB).withFlag(true).build();
        assertParseSuccess(parser, "1" + PHONE_BOB + FOLLOW_UP,
                new EditCommand(INDEX_FIRST_STUDENT, flagAndPhone));
    }

    @Test
    public void parse_noFollowUpAloneOrWithOtherEdits_success() {
        EditStudentDescriptor clearOnly = new EditStudentDescriptorBuilder().withFlag(false).build();
        assertParseSuccess(parser, "1 --no-follow-up", new EditCommand(INDEX_FIRST_STUDENT, clearOnly));
        assertParseSuccess(parser, "1 -nf", new EditCommand(INDEX_FIRST_STUDENT, clearOnly));
        assertParseSuccess(parser, "  1\u00a0-nf  ", new EditCommand(INDEX_FIRST_STUDENT, clearOnly));
        assertParseSuccess(parser, "  1\u00a0--no-follow-up  ", new EditCommand(INDEX_FIRST_STUDENT, clearOnly));
        EditStudentDescriptor clearAndPhone = new EditStudentDescriptorBuilder()
                .withPhone(VALID_PHONE_BOB).withFlag(false).build();
        assertParseSuccess(parser, "1 --no-follow-up" + PHONE_BOB,
                new EditCommand(INDEX_FIRST_STUDENT, clearAndPhone));
        assertParseSuccess(parser, "1 -nf" + PHONE_BOB,
                new EditCommand(INDEX_FIRST_STUDENT, clearAndPhone));
    }

    @Test
    public void parse_contradictoryFollowUpOptions_failure() {
        for (String options : new String[] {"--follow-up --no-follow-up", "--no-follow-up --follow-up",
            "-f --no-follow-up", "--no-follow-up -f -p 81112222", "--follow-up -nf", "-nf --follow-up",
            "-f -nf", "-nf -f -p 81112222"}) {
            assertParseFailure(parser, "1 " + options, Messages.MESSAGE_CONFLICTING_FOLLOW_UP);
        }
    }

    @Test
    public void parse_noFollowUpWithValueOrRepeated_failure() {
        for (String value : new String[] {"false", "true", "\"\"", "\"   \""}) {
            assertParseFailure(parser, "1 --no-follow-up " + value,
                    String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, value.replace("\"", "")));
            assertParseFailure(parser, "1 -nf " + value,
                    String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, value.replace("\"", "")));
        }
        assertParseFailure(parser, "1 --no-follow-up --no-follow-up",
                Messages.getErrorMessageForDuplicateFlags("--no-follow-up"));
        assertParseFailure(parser, "1 -nf -nf",
                Messages.getErrorMessageForDuplicateFlags("-nf"));
        assertParseFailure(parser, "1 --no-follow-up -nf",
                Messages.getErrorMessageForDuplicateFlags("--no-follow-up"));
        assertParseFailure(parser, "1 -nf --no-follow-up",
                Messages.getErrorMessageForDuplicateFlags("-nf"));
        assertParseFailure(parser, "1 --follow-up -f",
                Messages.getErrorMessageForDuplicateFlags("--follow-up"));
        assertParseFailure(parser, "1 --NO-FOLLOW-UP",
                String.format(Messages.MESSAGE_UNKNOWN_FLAG, "--NO-FOLLOW-UP"));
    }

    @Test
    public void parse_followUpWithValue_saysItBelongsToNoOption() {
        assertParseFailure(parser, "1" + FOLLOW_UP + " true",
                String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "true"));
    }

    @Test
    public void parse_missingIndex_namesItAsMissing() {
        assertParseFailure(parser, FLAG_NAME_AMY,
                Messages.getErrorMessageForMissingFields(EditCommand.FIELD_TARGET));
    }

    @Test
    public void parse_missingIndexAndField_reportsBothAtOnce() {
        assertParseFailure(parser, "",
                Messages.getErrorMessageForMissingFields(EditCommand.FIELD_TARGET)
                        + "\n" + EditCommand.MESSAGE_NOT_EDITED);
    }

    @Test
    public void parse_missingTarget_namesEveryTargetForm() {
        // a user who gives only options, expecting -i/--id to choose the student, is shown
        // that an index and an NUS Student ID are both valid targets
        assertParseFailure(parser, "--id 1A2B3C -p 81112222",
                "Missing required field(s): INDEX or NUS_STUDENT_ID");
    }

    @Test
    public void parse_noFieldSpecified_failure() {
        assertParseFailure(parser, "1", EditCommand.MESSAGE_NOT_EDITED);
    }

    @Test
    public void parse_invalidTarget_failure() {
        // zero, and anything that is not a positive integer, is not an index and reads
        // as a mistyped one rather than as a candidate NUS Student ID
        assertParseFailure(parser, "0" + FLAG_NAME_AMY,
                String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE));

        // a letter-first target that is not a valid NUS Student ID keeps the field-level reason
        assertParseFailure(parser, "A0123-456" + FLAG_NAME_AMY,
                Messages.getErrorMessageForInvalidValue(NusStudentId.FIELD_NAME, "A0123-456",
                        NusStudentId.getFailureReason("A0123-456")));

        // a digit-leading ID cannot be a target: the plain form reads as an index, and
        // --id names the field to set rather than the student to edit
        assertParseFailure(parser, "1A2B3C" + FLAG_NAME_AMY,
                String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE));

        // a token before the options that is not part of the target
        assertParseFailure(parser, "1 some random string" + FLAG_NAME_AMY,
                String.format(ParserUtil.MESSAGE_VALUE_AFTER_INDEX, "INDEX", "some random string"));

        // an unquoted negative number is invalid as both an index and an option
        assertParseFailure(parser, "-5" + FLAG_NAME_AMY,
                ParserUtil.MESSAGE_INVALID_INDEX + "\n" + String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-5"));
    }

    @Test
    public void parse_invalidTargetAndInvalidField_reportsBothAtOnce() {
        assertParseFailure(parser, "0" + INVALID_PHONE,
                String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE) + "\n"
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
        assertParseFailure(parser, "1" + INVALID_NUS_STUDENT_ID,
                Messages.getErrorMessageForInvalidValue(NusStudentId.FIELD_NAME, "A0123-456",
                        NusStudentId.getFailureReason("A0123-456")));
    }

    @Test
    public void parse_emailAtTheLengthLimit_success() {
        Index targetIndex = INDEX_FIRST_STUDENT;
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder()
                .withEmail(VALID_EMAIL_AT_MAX_LENGTH).build();

        assertParseSuccess(parser, targetIndex.getOneBased() + EMAIL_AT_MAX_LENGTH,
                new EditCommand(targetIndex, descriptor));
    }

    @Test
    public void parse_emailOneCharacterOverTheLengthLimit_failure() {
        // edit enters an address the same way add does, so it holds the same limit
        assertParseFailure(parser, "1" + EMAIL_OVER_MAX_LENGTH, MESSAGE_EMAIL_OVER_MAX_LENGTH);
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
                Messages.getErrorMessageForDuplicateFlags("-p"));

        assertParseFailure(parser, "1" + FOLLOW_UP + FOLLOW_UP,
                Messages.getErrorMessageForDuplicateFlags("--follow-up"));

        // a repeated option is refused before any field is read, since it leaves it unclear
        // which value belongs to the field
        assertParseFailure(parser, "1" + INVALID_PHONE + PHONE_BOB,
                Messages.getErrorMessageForDuplicateFlags("-p"));

        assertParseFailure(parser, "1" + PHONE_AMY + EMAIL_AMY + PHONE_BOB + EMAIL_BOB,
                Messages.getErrorMessageForDuplicateFlags("-p", "-e"));

        assertParseFailure(parser, "1 -n \"Amy Bee\" --name \"Bob Choo\"",
                Messages.getErrorMessageForDuplicateFlags("-n"));

        assertParseFailure(parser, "1" + PHONE_AMY + " --phone " + VALID_PHONE_BOB,
                Messages.getErrorMessageForDuplicateFlags("-p"));

        assertParseFailure(parser, "1" + EMAIL_AMY + " --email bob@example.com",
                Messages.getErrorMessageForDuplicateFlags("-e"));

        assertParseFailure(parser, "1 --name \"Amy Bee\" --name \"Bob Choo\"",
                Messages.getErrorMessageForDuplicateFlags("--name"));

        assertParseFailure(parser, "1 --id A01234567X -i A76543210Y",
                Messages.getErrorMessageForDuplicateFlags("--id"));
    }

    @Test
    public void parse_unknownFlag_isRefusedOnItsOwn() {
        assertParseFailure(parser, "1" + PHONE_BOB + " -z something",
                String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-z"));
        assertParseFailure(parser, "1" + PHONE_BOB + " --unknown something",
                String.format(Messages.MESSAGE_UNKNOWN_FLAG, "--unknown"));
    }

    @Test
    public void parse_flagWithoutValue_namesTheFieldItNeeds() {
        assertParseFailure(parser, "1" + PHONE_BOB + " -e",
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_EMAIL.getLabel()));
        assertParseFailure(parser, "1 --id",
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_NUS_STUDENT_ID.getLabel()));

        // an option in the value position means the one before it was left empty, rather than
        // "-e" being stored as a tag
        assertParseFailure(parser, "1 -t -e a@b.com",
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_TAG.getLabel()));

        assertParseFailure(parser, "1 --email",
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_EMAIL.getLabel("--email")));

        assertParseFailure(parser, "1 --tag --email a@b.com",
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_TAG.getLabel("--tag")));
    }

    @Test
    public void parse_unclosedQuote_isReportedAsSuch() {
        assertParseFailure(parser, "1 " + FLAG_NAME + " \"Amy Bee" + PHONE_BOB,
                CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }
}
