package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.TagCommand;
import seedu.tab.model.tag.Tag;

public class TagCommandParserTest {

    private final TagCommandParser parser = new TagCommandParser();

    @Test
    public void parse_validValues_success() {
        for (String marker : List.of("-a", "--add")) {
            for (String value : List.of("T1", "Lab 3", "组 1", "CS2103T (AY26/27)", "-Lab",
                    "-a", "--add", "x".repeat(1000))) {
                assertParseSuccess(parser, " 1 " + marker + " \"  " + value + "  \" ",
                        new TagCommand(INDEX_FIRST_STUDENT, new Tag(value)));
            }
        }
        assertParseSuccess(parser, "\u00a01\t--add \"Lab\u3000 3\"",
                new TagCommand(INDEX_FIRST_STUDENT, new Tag("Lab 3")));
        assertParseSuccess(parser, "1 -a \"Lab \\\"A\\\"\\\\B\"",
                new TagCommand(INDEX_FIRST_STUDENT, new Tag("Lab \"A\"\\B")));
    }

    @Test
    public void parse_missingFields_reportsEachField() {
        assertParseFailure(parser, "", "Missing required field(s): INDEX\nMissing required field(s): -a TAG");
        assertParseFailure(parser, "1", "Missing required field(s): -a TAG");
        assertParseFailure(parser, "-a T1", "Missing required field(s): INDEX");
        assertParseFailure(parser, "\"\" -a T1", "Missing required field(s): INDEX");
    }

    @Test
    public void parse_invalidIndex_reportsIndex() {
        for (String index : List.of("0", "-1", "abc", "1.5", "2147483648", "999999999999999999999", "1 2")) {
            // Quote negative values so that they are indexes rather than unknown options.
            assertParseFailure(parser, "\"" + index + "\" -a T1", ParserUtil.MESSAGE_INVALID_INDEX);
        }
        assertParseSuccess(parser, "2147483647 -a T1",
                new TagCommand(Index.fromOneBased(Integer.MAX_VALUE), new Tag("T1")));
    }

    @Test
    public void parse_invalidTag_reportsValueAndReason() {
        for (String value : List.of("", "   ", "\u00a0\u3000", "---", "🙂")) {
            assertParseFailure(parser, "1 -a \"" + value + "\"",
                    Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, value.trim(),
                            "it holds no letter or number"));
        }
        assertParseFailure(parser, "0 -a \"\"",
                ParserUtil.MESSAGE_INVALID_INDEX + "\nTag \"\" is not valid: it holds no letter or number");
    }

    @Test
    public void parse_mistypedOptions_reportsSpecificProblem() {
        for (String marker : List.of("-a", "--add")) {
            assertParseFailure(parser, "1 " + marker,
                    marker + " TAG needs a value after it. A value opening with a hyphen goes in double quotes.");
            for (String repeatedMarker : List.of("-a", "--add")) {
                assertParseFailure(parser, "1 " + marker + " T1 " + repeatedMarker + " T2",
                        Messages.getErrorMessageForDuplicateFlags(marker));
            }
        }
        assertParseFailure(parser, "1 -a -p 123", String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, "-a TAG"));
        assertParseFailure(parser, "1 --adds T1", "There is no --adds option.");
        assertParseFailure(parser, "1 -a Lab 3", String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "3"));
        assertParseFailure(parser, "1 -a \"Lab 3", CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }

    @Test
    public void parse_nonAddOptions_rejected() {
        for (String marker : List.of("-t", "--tag", "-A", "--ADD")) {
            assertParseFailure(parser, "1 " + marker + " T1",
                    String.format(Messages.MESSAGE_UNKNOWN_FLAG, marker));
        }
    }

    @Test
    public void parse_addOptionInOtherCommands_rejected() {
        for (String marker : List.of("-a", "--add")) {
            String expected = String.format(Messages.MESSAGE_UNKNOWN_FLAG, marker);
            assertParseFailure(new AddCommandParser(), "Alice -p 91234567 " + marker + " T1", expected);
            assertParseFailure(new EditCommandParser(), "1 " + marker + " T1", expected);
        }
    }
}
