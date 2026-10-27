package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.logic.parser.FindCommandParser.MESSAGE_EMPTY_KEYWORD;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public void parse_unqualifiedTermAfterAnOption_readsLikeOneBeforeTheOptions() {
        // find takes no term at the front, so the reply that names its own grammar is the one
        // that applies wherever the bare term sits
        for (String input : List.of("-n alex yeoh", "-t lab tutorial", "-n 'Jason Tan'")) {
            assertParseFailure(parser, input, FindCommandParser.MESSAGE_UNQUALIFIED_TERM);
        }
        assertParseSuccess(parser, "-n alex -n yeoh", new FindCommand(new DetailsContainsKeywordsPredicate(
                Map.of(Field.NAME, List.of("alex", "yeoh")))));
        assertParseSuccess(parser, "-n \"alex yeoh\"", new FindCommand(new DetailsContainsKeywordsPredicate(
                Map.of(Field.NAME, List.of("alex yeoh")))));
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
        assertParseFailure(parser, "-n 'Jason Tan'", FindCommandParser.MESSAGE_UNQUALIFIED_TERM);
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
        // a term after the options is the same mistake as one before them, so it reads the same
        assertParseFailure(parser, "-t lab tutorial", FindCommandParser.MESSAGE_UNQUALIFIED_TERM);
        assertParseFailure(parser, "Jason -t lab", FindCommandParser.MESSAGE_UNQUALIFIED_TERM);
        assertParseFailure(parser, "\"\" -t lab", FindCommandParser.MESSAGE_UNQUALIFIED_TERM);
        assertParseFailure(parser, "-t \"Lab", CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }

    @Test
    public void parse_followUpSelectors_acceptsAliasesAloneOrWithFields() {
        for (String flag : List.of("-f", "--follow-up", "-nf", "--no-follow-up")) {
            boolean isFlagged = flag.equals("-f") || flag.equals("--follow-up");
            FindCommand statusOnly = new FindCommand(
                    new DetailsContainsKeywordsPredicate(Map.of(), Optional.of(isFlagged)));
            assertParseSuccess(parser, flag, statusOnly);
            FindCommand combined = new FindCommand(new DetailsContainsKeywordsPredicate(
                    Map.of(Field.NAME, List.of("Jason"), Field.TAG, List.of("Lab")), Optional.of(isFlagged)));
            assertParseSuccess(parser, flag + " -n Jason -t Lab", combined);
            assertParseSuccess(parser, "-n Jason -t Lab " + flag, combined);
        }
    }

    @Test
    public void parse_invalidStatusAndEmptyTerm_reportsStatusFirst() {
        assertParseFailure(parser, "-n \"\" -f -nf", Messages.MESSAGE_CONFLICTING_FOLLOW_UP);
        assertParseFailure(parser, "-n \"\" -f --follow-up -nf",
                Messages.getErrorMessageForDuplicateFlags("-f"));
    }

    @Test
    public void parse_invalidStatusSelectors_reportsSpecificProblem() {
        for (String input : List.of("-f -nf", "--no-follow-up --follow-up", "-nf -n Jason -f")) {
            assertParseFailure(parser, input, Messages.MESSAGE_CONFLICTING_FOLLOW_UP);
        }
        for (String input : List.of("-f -f", "-f --follow-up")) {
            assertParseFailure(parser, input, Messages.getErrorMessageForDuplicateFlags("-f"));
        }
        assertParseFailure(parser, "-nf --no-follow-up", Messages.getErrorMessageForDuplicateFlags("-nf"));
        assertParseFailure(parser, "-f true", String.format(Messages.MESSAGE_FLAG_TAKES_NO_VALUE, "-f"));
        assertParseFailure(parser, "-nf=none", String.format(Messages.MESSAGE_FLAG_TAKES_NO_VALUE, "-nf"));
        assertParseFailure(parser, "-f -n", String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, "-n NAME"));
    }

}
