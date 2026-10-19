package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import org.junit.jupiter.api.Test;

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
        assertParseFailure(parser, "\"1 2\"", String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                DeleteCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_unclosedQuote_throwsUnclosedQuoteMessage() {
        assertParseFailure(parser, "\"1", CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }

    @Test
    public void parse_emptyArgs_throwsParseException() {
        assertParseFailure(parser, "", String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        assertParseFailure(parser, "   ", String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_zeroIndex_throwsParseException() {
        assertParseFailure(parser, "0", String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        assertParseFailure(parser, "  0  ", String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_negativeIndex_throwsParseException() {
        assertParseFailure(parser, "-1", String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        assertParseFailure(parser, "-10", String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
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
        // candidate NUS Student ID, so it is refused with the usage message
        assertParseFailure(parser, "!@#", String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_overflowIndex_throwsParseException() {
        assertParseFailure(parser, Long.toString(Integer.MAX_VALUE + 1L),
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        assertParseFailure(parser, Long.toString(Long.MAX_VALUE),
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_extraneousTokens_throwsParseException() {
        assertParseFailure(parser, "1 extra",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        assertParseFailure(parser, "1 2",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
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
        assertParseFailure(parser, "--id=", Messages.getErrorMessageForInvalidValue(
                NusStudentId.FIELD_NAME, "", NusStudentId.MESSAGE_EMPTY));
    }

    @Test
    public void parse_malformedNusStudentIdFlag_throwsParseException() {
        // the option takes exactly one value and leaves no room for a second target
        assertParseFailure(parser, "--id", USAGE_MESSAGE);
        assertParseFailure(parser, "--id 1ABC extra", USAGE_MESSAGE);
        assertParseFailure(parser, "1 --id A01234567X", USAGE_MESSAGE);
        assertParseFailure(parser, "--id A01234567X --follow-up", USAGE_MESSAGE);

        // a quoted token is a value, not an option, as everywhere else in a command
        assertParseFailure(parser, "\"--id\" 1ABC", USAGE_MESSAGE);
        assertParseFailure(parser, "\"--id=1ABC\"", USAGE_MESSAGE);
    }

    /** Returns the delete command that names the student holding {@code nusStudentId}. */
    private static DeleteCommand deleteById(String nusStudentId) {
        return new DeleteCommand(StudentTarget.byNusStudentId(new NusStudentId(nusStudentId)));
    }
}
