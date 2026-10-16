package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.logic.parser.FindCommandParser.MESSAGE_EMPTY_KEYWORD;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.commands.FindCommand;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;

public class FindCommandParserTest {

    private FindCommandParser parser = new FindCommandParser();

    @Test
    public void parse_emptyArg_throwsParseException() {
        assertParseFailure(parser, "     ",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
    }

    @Test
    public void parse_emptyOrBlankQuotedTerm_throwsParseException() {
        // empty quoted string
        assertParseFailure(parser, "\"\"", MESSAGE_EMPTY_KEYWORD);

        // whitespace-only quoted string
        assertParseFailure(parser, "\"   \"", MESSAGE_EMPTY_KEYWORD);

        // zero-width space inside quotes
        assertParseFailure(parser, "\"\u200B\"", MESSAGE_EMPTY_KEYWORD);

        // mixed valid keyword with empty quoted string
        assertParseFailure(parser, "Alice \"\"", MESSAGE_EMPTY_KEYWORD);

        // mixed valid keyword with whitespace-only quoted string
        assertParseFailure(parser, "Alice \"   \" Bob", MESSAGE_EMPTY_KEYWORD);
    }

    @Test
    public void parse_unclosedQuote_throwsParseException() {
        assertParseFailure(parser, "\"Lab 3", CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
        assertParseFailure(parser, "Alice \"Lab 3", CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }

    @Test
    public void parse_validArgs_returnsFindCommand() {
        // no leading and trailing whitespaces
        FindCommand expectedFindCommand =
                new FindCommand(new DetailsContainsKeywordsPredicate(List.of("Alice", "Bob")));
        assertParseSuccess(parser, "Alice Bob", expectedFindCommand);

        // multiple whitespaces between keywords
        assertParseSuccess(parser, " \n Alice \n \t Bob  \t", expectedFindCommand);

        // single quoted phrase
        FindCommand expectedPhraseCommand =
                new FindCommand(new DetailsContainsKeywordsPredicate(List.of("Lab 3")));
        assertParseSuccess(parser, "\"Lab 3\"", expectedPhraseCommand);

        // quoted phrase with surrounding whitespace inside the quotes trimmed
        assertParseSuccess(parser, "\"  Lab 3  \"", expectedPhraseCommand);

        // quoted phrase with multiple internal spaces collapsed
        assertParseSuccess(parser, "\"Lab   3\"", expectedPhraseCommand);

        // quoted phrase with internal non-breaking space collapsed to normal space
        assertParseSuccess(parser, "\"Lab\u00A03\"", expectedPhraseCommand);

        // zero-width spaces stripped
        FindCommand expectedStrippedZeroWidthCommand =
                new FindCommand(new DetailsContainsKeywordsPredicate(List.of("Lab3")));
        assertParseSuccess(parser, "\"Lab\u200B3\"", expectedStrippedZeroWidthCommand);

        // decomposed unicode characters composed to NFC
        FindCommand expectedComposedCommand =
                new FindCommand(new DetailsContainsKeywordsPredicate(List.of("caf\u00E9")));
        assertParseSuccess(parser, "\"caf\u0065\u0301\"", expectedComposedCommand);

        // mixed unquoted words and quoted phrase
        FindCommand expectedMixedCommand =
                new FindCommand(new DetailsContainsKeywordsPredicate(List.of("Alice", "Lab 3", "Bob")));
        assertParseSuccess(parser, "Alice \"Lab 3\" Bob", expectedMixedCommand);

        // escaped quotes within quoted phrase
        FindCommand expectedEscapedQuotesCommand =
                new FindCommand(new DetailsContainsKeywordsPredicate(List.of("say \"hi\"")));
        assertParseSuccess(parser, "\"say \\\"hi\\\"\"", expectedEscapedQuotesCommand);

        // escaped backslash
        FindCommand expectedEscapedBackslashCommand =
                new FindCommand(new DetailsContainsKeywordsPredicate(List.of("path\\to")));
        assertParseSuccess(parser, "\"path\\\\to\"", expectedEscapedBackslashCommand);

        // unicode whitespace between arguments
        assertParseSuccess(parser, "Alice\u00A0\"Lab 3\"\u3000Bob", expectedMixedCommand);
    }

}
