package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.logic.parser.FindCommandParser.MESSAGE_EMPTY_KEYWORD;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate.Field;

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

    @Test
    public void parse_fieldAliasesAndAttachedValues_returnsFindCommand() {
        Map<Field, List<String>> terms = Map.of(Field.NAME, List.of("Jason"), Field.TAG, List.of("Lab Tutorial"),
                Field.PHONE, List.of("9123"), Field.EMAIL, List.of("nus.edu"), Field.NUS_STUDENT_ID, List.of("a0123"));
        FindCommand expected = new FindCommand(new DetailsContainsKeywordsPredicate(terms));
        assertParseSuccess(parser, "-n Jason -t \"Lab Tutorial\" -p 9123 -e nus.edu --id a0123", expected);
        assertParseSuccess(parser, "--name Jason --tag \"Lab Tutorial\" --phone 9123 --email nus.edu -i a0123",
                expected);
        assertParseSuccess(parser, "-n=\"Jason\" -t=\"Lab Tutorial\" -p=9123 -e=nus.edu --id=a0123", expected);
    }

    @Test
    public void parse_repeatedSelectors_keepsEveryCondition() {
        FindCommand expected = new FindCommand(new DetailsContainsKeywordsPredicate(Map.of(
                Field.NAME, List.of("Jason", "Lim"), Field.TAG, List.of("lab", "tutorial"))));
        assertParseSuccess(parser, "-n Jason --name Lim -t lab --tag tutorial", expected);
    }

    @Test
    public void parse_fieldValues_normalizesWithoutRestrictingSymbols() {
        for (String value : List.of("s/o", "a/l", "O'Neil", "组 1", "-Lab", "say \"hi\"", "path\\to",
                "x".repeat(1000), "912345678901234567890")) {
            String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
            FindCommand expected = new FindCommand(new DetailsContainsKeywordsPredicate(
                    Map.of(Field.TAG, List.of(value))));
            assertParseSuccess(parser, "-t \"" + escaped + "\"", expected);
        }
        FindCommand expected = new FindCommand(new DetailsContainsKeywordsPredicate(
                Map.of(Field.TAG, List.of("café Lab"))));
        assertParseSuccess(parser, "--tag=\" cafe\u0301\u00a0  Lab \"", expected);
    }

    @Test
    public void parse_invalidFieldSyntax_reportsSpecificProblem() {
        for (String input : List.of("-t", "-t -n Jason", "--tag=")) {
            assertParseFailure(parser, input, String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE,
                    CliFlags.FLAG_TAG.getLabel(input.startsWith("--") ? "--tag" : "-t")));
        }
        for (String input : List.of("-t \"\"", "-t \"   \"", "--tag=\"\"", "-n \"\u200b\"")) {
            assertParseFailure(parser, input, MESSAGE_EMPTY_KEYWORD);
        }
        assertParseFailure(parser, "--unknown friend", String.format(Messages.MESSAGE_UNKNOWN_FLAG, "--unknown"));
        assertParseFailure(parser, "-T friend", String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-T"));
        assertParseFailure(parser, "-f follow-up", String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-f"));
        assertParseFailure(parser, "-t lab tutorial", String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "tutorial"));
        assertParseFailure(parser, "Jason -t lab", FindCommandParser.MESSAGE_UNQUALIFIED_TERM);
        assertParseFailure(parser, "\"\" -t lab", FindCommandParser.MESSAGE_UNQUALIFIED_TERM);
        assertParseFailure(parser, "-t \"Lab", CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }

}
