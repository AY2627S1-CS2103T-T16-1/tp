package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.util.ValueMessages;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.model.student.NusStudentId;

/**
 * As we are only doing white-box testing, our test cases do not cover path variations
 * outside of the DeleteCommand code. For example, inputs "1" and "1 abc" take the
 * same path through the DeleteCommand, and therefore we test only one of them.
 * The path variation for those two cases occurs inside the ParserUtil, and
 * therefore should be covered by the ParserUtilTest.
 */
public class DeleteCommandParserTest {

    private static final String USAGE_MESSAGE =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE);

    private DeleteCommandParser parser = new DeleteCommandParser();

    @Test
    public void parse_digitLeadingIdWithoutOption_reportsExplicitIdCommand() {
        assertParseFailure(parser, "1ABC", invalidIndexWithIdHint("1ABC"));
        assertTrue(invalidIndexWithIdHint("1ABC").contains("use delete --id 1ABC"));
    }

    @Test
    public void parse_validArgs_returnsDeleteCommand() {
        assertParseSuccess(parser, "1", new DeleteCommand(INDEX_FIRST_STUDENT));
    }

    @Test
    public void parse_validArgsWithWhitespace_returnsDeleteCommand() {
        assertParseSuccess(parser, "  1  ", new DeleteCommand(INDEX_FIRST_STUDENT));
        assertParseSuccess(parser, " \t 1 \n ", new DeleteCommand(INDEX_FIRST_STUDENT));
    }

    @Test
    public void parse_indexAroundUnicodeWhitespace_returnsDeleteCommand() {
        // a command pasted from a web page carries the non-breaking space the page used, and
        // a Chinese input method types a full-width space in place of the space bar
        assertParseSuccess(parser, "\u00A01", new DeleteCommand(INDEX_FIRST_STUDENT));
        assertParseSuccess(parser, "1\u00A0", new DeleteCommand(INDEX_FIRST_STUDENT));
        assertParseSuccess(parser, "\u30001\u3000", new DeleteCommand(INDEX_FIRST_STUDENT));
        assertParseSuccess(parser, " \u00A01\u3000 ", new DeleteCommand(INDEX_FIRST_STUDENT));
    }

    @Test
    public void parse_quotedIndex_returnsDeleteCommand() {
        // the index is read as one token however it is grouped, as the preamble of edit is
        assertParseSuccess(parser, "\"1\"", new DeleteCommand(INDEX_FIRST_STUDENT));
    }

    @Test
    public void parse_quotedIndexHoldingTwoNumbers_throwsParseException() {
        assertParseFailure(parser, "\"1 2\"", ParserUtil.MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_unclosedQuote_throwsUnclosedQuoteMessage() {
        assertParseFailure(parser, "\"1", CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }

    @Test
    public void parse_emptyArgs_throwsParseException() {
        assertParseFailure(parser, "", DeleteCommand.MESSAGE_USAGE);
        assertParseFailure(parser, "   ", DeleteCommand.MESSAGE_USAGE);
    }

    @Test
    public void parse_quotedEmptyTarget_throwsUsageMessage() {
        for (String input : List.of("\"\"", "\"   \"", "\"\" \"\"")) {
            assertParseFailure(parser, input, DeleteCommand.MESSAGE_USAGE);
        }
    }

    @Test
    public void parse_invalidTargetBeforeExtraValue_throwsInvalidIndexMessage() {
        for (String input : List.of("\"\" !", "! extra")) {
            assertParseFailure(parser, input, ParserUtil.MESSAGE_INVALID_INDEX);
        }
    }

    @Test
    public void parse_repeatedIdOption_throwsDuplicateOptionMessage() {
        for (String input : List.of("--id A --id B", "--id A -i B", "-i=A -i=B")) {
            String marker = input.startsWith("--id") ? "--id" : "-i";
            assertParseFailure(parser, input,
                    "Options given more than once: " + marker + ". Give each listed option only once.");
        }
    }

    @Test
    public void parse_miscasedIdOption_suggestsLowercaseOption() {
        assertParseFailure(parser, "--ID A01234567X", "There is no --ID option. Did you mean --id?");
        assertParseFailure(parser, "-I A01234567X", "There is no -I option. Did you mean -i?");
    }

    @Test
    public void parse_zeroIndex_throwsParseException() {
        // the reason stands on its own, as edit and tag report theirs, rather than under the
        // usage block, and a target that reads as an ID still names the --id command for it
        String expected = invalidIndexWithIdHint("0");
        assertParseFailure(parser, "0", expected);
        assertParseFailure(parser, "  0  ", expected);
        assertEquals("Index must be a positive integer. "
                + "If \"0\" is an NUS Student ID, use delete --id 0.", expected);
    }

    @Test
    public void parse_negativeIndex_throwsParseException() {
        // a marker followed by a digit is a negative number; tokenizeWithIndex reports both
        // the index reason and the unknown-flag hint, so the user sees why it fails either way
        String expected = ParserUtil.MESSAGE_INVALID_INDEX + "\n"
                + String.format(Messages.MESSAGE_UNKNOWN_FLAG, Messages.shorten("-1"));
        assertParseFailure(parser, "-1", expected);
        String expected10 = ParserUtil.MESSAGE_INVALID_INDEX + "\n"
                + String.format(Messages.MESSAGE_UNKNOWN_FLAG, Messages.shorten("-10"));
        assertParseFailure(parser, "-10", expected10);
    }

    @Test
    public void parse_negativeDecimal_reportsInvalidIndexAndOption() {
        assertParseFailure(parser, "-1.5", ParserUtil.MESSAGE_INVALID_INDEX
                + "\nThere is no -1.5 option.");
        assertParseFailure(parser, "\"-1.5\"", ParserUtil.MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_nonIntegerIndex_throwsParseException() {
        for (String target : List.of("1.5", "+1", "1,5", "1-2")) {
            assertParseFailure(parser, target, ParserUtil.MESSAGE_INVALID_INDEX);
        }
    }

    @Test
    public void parse_loneOption_throwsUsageMessage() {
        // an unknown option is reported as such
        assertParseFailure(parser, "--follow-up", "There is no --follow-up option.");
        // a known value-taking option without its value reports the missing value with the marker used
        String missingValueMsgForI = "-i NUS_STUDENT_ID needs a value after it. "
                + "A value opening with a hyphen goes in double quotes.";
        String missingValueMsgForId = "--id NUS_STUDENT_ID needs a value after it. "
                + "A value opening with a hyphen goes in double quotes.";
        assertParseFailure(parser, "-i", missingValueMsgForI);
        assertParseFailure(parser, "--id", missingValueMsgForId);
    }

    @Test
    public void parse_nusStudentId_returnsDeleteCommand() {
        // an NUS Student ID is any string of letters and digits, so any target opening with
        // a letter that holds one is read as an ID rather than as a mistyped index
        assertParseSuccess(parser, "abc", deleteById("abc"));
        assertParseSuccess(parser, "A01234567X", deleteById("A01234567X"));
    }

    @Test
    public void parse_nusStudentIdIgnoresCase_returnsDeleteCommand() {
        // NusStudentId normalizes its value, so a lowercase ID names the same student
        assertParseSuccess(parser, "a01234567x", deleteById("A01234567X"));
    }

    @Test
    public void parse_quotedNusStudentId_returnsDeleteCommand() {
        assertParseSuccess(parser, "\"A01234567X\"", deleteById("A01234567X"));
    }

    @Test
    public void parse_invalidNusStudentId_throwsParseException() {
        assertParseFailure(parser, "A0123-456", Messages.getErrorMessageForInvalidValue(
                NusStudentId.FIELD_NAME, "A0123-456", NusStudentId.MESSAGE_NON_ALPHANUMERIC));
        String tooLong = "A" + "1".repeat(NusStudentId.MAX_LENGTH);
        assertParseFailure(parser, tooLong, Messages.getErrorMessageForInvalidValue(
                NusStudentId.FIELD_NAME, tooLong, NusStudentId.MESSAGE_TOO_LONG));
    }

    @Test
    public void parse_invalidArgs_throwsParseException() {
        // a target that does not open with a letter reads as a mistyped index, not as a
        // candidate NUS Student ID, so the index reason is the one reported
        assertParseFailure(parser, "!@#", ParserUtil.MESSAGE_INVALID_INDEX);
    }

    @Test
    public void parse_overflowIndex_throwsParseException() {
        assertParseFailure(parser, "2147483648", "Index \"2147483648\" exceeds the largest supported index "
                + "(2147483647). Use an index from the displayed list. "
                + "If \"2147483648\" is an NUS Student ID, use delete --id 2147483648.");
    }

    @Test
    public void parse_extraneousTokens_throwsParseException() {
        assertParseFailure(parser, "1 extra", valueAfterIndexMessage("extra"));
        assertParseFailure(parser, "1 2", valueAfterIndexMessage("2"));
        // the wording the User Guide quotes, so that the two stay in step
        assertEquals("Unexpected value after INDEX: \"2\". "
                + "delete accepts one index or NUS Student ID and nothing else.",
                valueAfterIndexMessage("2"));
    }

    @Test
    public void parse_severalExtraneousTokens_namesEveryOneOfThem() {
        assertParseFailure(parser, "1 extra 2", valueAfterIndexMessage("extra 2"));
    }

    @Test
    public void parse_extraneousTokensAfterLetterLeadingTarget_namesTheIdForm() {
        // a target opening with a letter is read as an NUS Student ID, so a user who typed
        // a name is not told they typed an index
        String expected = valueAfterTargetMessage(ParserUtil.FIELD_NUS_STUDENT_ID, "Yeoh");
        assertParseFailure(parser, "Alex Yeoh", expected);
        assertParseFailure(parser, "A01234567X Yeoh", expected);
        assertTrue(expected.startsWith("Unexpected value after NUS_STUDENT_ID: \"Yeoh\"."));
    }

    @Test
    public void parse_bareHyphenAfterTheIndex_namesItAsTheExtraValue() {
        // a bare hyphen is spelled like an option without being one, and delete takes it
        // as the extra value rather than showing the usage message an option would
        assertParseFailure(parser, "1 -", valueAfterIndexMessage("-"));
    }

    @Test
    public void parse_negativeNumberAfterTheIndex_namesItAsTheExtraValue() {
        // a negative number after the target is read as an unknown flag (consistent with tag/edit)
        assertParseFailure(parser, "1 -1", "There is no -1 option.");
    }

    @Test
    public void parse_longExtraneousToken_shortensItInTheFeedback() {
        String extra = "x".repeat(ValueMessages.MAX_QUOTED_VALUE_LENGTH + 1);
        assertParseFailure(parser, "1 " + extra,
                valueAfterIndexMessage("x".repeat(ValueMessages.MAX_QUOTED_VALUE_LENGTH) + "…"));
    }

    /** The reply {@code delete} gives for a target that reads as an unusable index. */
    private static String invalidIndexWithIdHint(String value) {
        return ParserUtil.MESSAGE_INVALID_INDEX + " "
                + Messages.getIdTargetHint(DeleteCommand.ID_TARGET_COMMAND, value);
    }

    /** The reply {@code delete} gives for the value it found after a target read as an index. */
    private static String valueAfterIndexMessage(String value) {
        return valueAfterTargetMessage(DeleteCommand.FIELD_INDEX, value);
    }

    /** The reply {@code delete} gives for the value it found after a target named {@code fieldName}. */
    private static String valueAfterTargetMessage(String fieldName, String value) {
        return String.format(ParserUtil.MESSAGE_VALUE_AFTER_TARGET_ONLY, fieldName, value,
                DeleteCommand.COMMAND_WORD);
    }

    @Test
    public void parse_nusStudentIdFlag_returnsDeleteCommand() {
        // the option names the ID explicitly, so an ID opening with a digit is not read as an index
        assertParseSuccess(parser, "--id 1ABC", deleteById("1ABC"));
        assertParseSuccess(parser, "-i 1ABC", deleteById("1ABC"));
        assertParseSuccess(parser, "--id=1ABC", deleteById("1ABC"));
        assertParseSuccess(parser, "-i=A01234567X", deleteById("A01234567X"));

        // the value is normalized the same way as anywhere else
        assertParseSuccess(parser, "--id a01234567x", deleteById("A01234567X"));
    }

    @Test
    public void parse_invalidNusStudentIdFlagValue_throwsParseException() {
        assertParseFailure(parser, "--id A0123-456", Messages.getErrorMessageForInvalidValue(
                NusStudentId.FIELD_NAME, "A0123-456", NusStudentId.MESSAGE_NON_ALPHANUMERIC));
        // an empty value after = is reported as missing value, not as invalid ID
        String missingValueMsg = "--id NUS_STUDENT_ID needs a value after it. "
                + "A value opening with a hyphen goes in double quotes.";
        assertParseFailure(parser, "--id=", missingValueMsg);
    }

    @Test
    public void parse_valueJoinedToAnotherMarker_readsTheTokenAsTheTarget() {
        // an equals sign joins a value to an option, so a token that opens any other way
        // is still read as the target itself
        assertParseFailure(parser, "1=2", ParserUtil.MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "a=b", Messages.getErrorMessageForInvalidValue(
                NusStudentId.FIELD_NAME, "a=b", NusStudentId.MESSAGE_NON_ALPHANUMERIC));
    }

    @Test
    public void parse_malformedNusStudentIdFlag_throwsParseException() {
        // the option takes exactly one value and leaves no room for a second target
        String missingValueMsg = "--id NUS_STUDENT_ID needs a value after it. "
                + "A value opening with a hyphen goes in double quotes.";
        assertParseFailure(parser, "--id", missingValueMsg);
        // extra token after flag value is reported as value after flags (no preamble in this case)
        assertParseFailure(parser, "--id 1ABC extra",
                "\"extra\" does not belong to any option. Options that take a value take one token, "
                        + "and a value holding spaces goes in double quotes. "
                        + "If the command takes a name or index without an option, put it before all options.");
        assertParseFailure(parser, "1 --id A01234567X", DeleteCommand.MESSAGE_MULTIPLE_TARGETS);
        assertParseFailure(parser, "--id A01234567X --follow-up", "There is no --follow-up option.");

        // a quoted token is a value, not an option, as everywhere else in a command
        // the combined preamble "--id 1ABC" is not a valid index
        assertParseFailure(parser, "\"--id\" 1ABC", ParserUtil.MESSAGE_INVALID_INDEX);
        assertParseFailure(parser, "\"--id=1ABC\"", ParserUtil.MESSAGE_INVALID_INDEX);
    }

    /** Returns the delete command that names the student holding {@code nusStudentId}. */
    private static DeleteCommand deleteById(String nusStudentId) {
        return new DeleteCommand(StudentTarget.byNusStudentId(new NusStudentId(nusStudentId)));
    }
}
