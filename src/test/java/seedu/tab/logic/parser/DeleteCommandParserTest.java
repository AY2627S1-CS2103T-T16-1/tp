package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.DeleteCommand;

/**
 * As we are only doing white-box testing, our test cases do not cover path variations
 * outside of the DeleteCommand code. For example, inputs "1" and "1 abc" take the
 * same path through the DeleteCommand, and therefore we test only one of them.
 * The path variation for those two cases occurs inside the ParserUtil, and
 * therefore should be covered by the ParserUtilTest.
 */
public class DeleteCommandParserTest {

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
    public void parse_invalidArgs_throwsParseException() {
        assertParseFailure(parser, "a", String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        assertParseFailure(parser, "abc", String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
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
        assertParseFailure(parser, "1 extra", valueAfterIndexMessage("extra"));
        assertParseFailure(parser, "1 2", valueAfterIndexMessage("2"));
    }

    @Test
    public void parse_severalExtraneousTokens_namesEveryOneOfThem() {
        assertParseFailure(parser, "1 extra 2", valueAfterIndexMessage("extra 2"));
    }

    @Test
    public void parse_longExtraneousToken_shortensItInTheFeedback() {
        String extra = "x".repeat(Messages.MAX_QUOTED_VALUE_LENGTH + 1);
        assertParseFailure(parser, "1 " + extra,
                valueAfterIndexMessage("x".repeat(Messages.MAX_QUOTED_VALUE_LENGTH) + "…"));
    }

    /** The reply {@code delete} gives for the value it found after the index. */
    private static String valueAfterIndexMessage(String value) {
        return String.format(ParserUtil.MESSAGE_VALUE_AFTER_INDEX_ONLY, DeleteCommand.FIELD_INDEX,
                value, DeleteCommand.COMMAND_WORD);
    }
}
