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
    public void parse_unqualifiedTerms_throwsParseException() {
        for (String input : List.of("Jason", "Jason Tan", "Jason -t Lab", "\"Jason Tan\"", "\"\"", "-")) {
            assertParseFailure(parser, input, FindCommandParser.MESSAGE_UNQUALIFIED_TERM);
        }
    }

    @Test
    public void parse_validArgs_returnsFindCommand() {
        FindCommand expected = new FindCommand(new DetailsContainsKeywordsPredicate(
                Map.of(Field.NAME, List.of("Jason", "Tan"), Field.TAG, List.of("Lab"))));
        assertParseSuccess(parser, "-n Jason -t Lab -n Tan", expected);
        assertParseSuccess(parser, " \n -n\u00a0Jason \n\t -t Lab -n Tan\u3000", expected);
    }

    @Test
    public void parse_doubleQuotedPhrase_normalizesWhitespaceAndUnicode() {
        FindCommand expected = new FindCommand(new DetailsContainsKeywordsPredicate(
                Map.of(Field.NAME, List.of("Jason Tan"))));
        for (String input : List.of("-n \"Jason Tan\"", "-n \" Jason   Tan \"", "--name=\"Jason\u00a0Tan\"")) {
            assertParseSuccess(parser, input, expected);
        }
        FindCommand stripped = new FindCommand(new DetailsContainsKeywordsPredicate(
                Map.of(Field.TAG, List.of("Lab3"))));
        assertParseSuccess(parser, "-t \"Lab\u200b3\"", stripped);
    }

    @Test
    public void parse_apostrophes_areLiteralCharacters() {
        FindCommand expected = new FindCommand(new DetailsContainsKeywordsPredicate(
                Map.of(Field.NAME, List.of("O'Brien"))));
        assertParseSuccess(parser, "-n O'Brien", expected);
        assertParseSuccess(parser, "-n \"O'Brien\"", expected);
        FindCommand literalQuotes = new FindCommand(new DetailsContainsKeywordsPredicate(
                Map.of(Field.NAME, List.of("'Jason'"))));
        assertParseSuccess(parser, "-n 'Jason'", literalQuotes);
        assertParseFailure(parser, "-n 'Jason Tan'", String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "Tan'"));
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
