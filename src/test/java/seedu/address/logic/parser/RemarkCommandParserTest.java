package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.RemarkCommand;
import seedu.address.model.person.Remark;

public class RemarkCommandParserTest {

    private static final String MESSAGE_INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);

    private final RemarkCommandParser parser = new RemarkCommandParser();

    @Test
    public void parse_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    public void parse_validRemark_success() {
        RemarkCommand expectedCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Likes baseball"));
        assertParseSuccess(parser, "1 r/Likes baseball", expectedCommand);
        assertParseSuccess(parser, "  1  r/  Likes baseball  ", expectedCommand);
    }

    @Test
    public void parse_emptyRemark_success() {
        RemarkCommand expectedCommand = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(""));
        assertParseSuccess(parser, "1 r/", expectedCommand);
        assertParseSuccess(parser, "1 r/   ", expectedCommand);
        assertParseSuccess(parser, "1", expectedCommand);
    }

    @Test
    public void parse_otherPrefixesInRemark_preservesText() {
        String remark = "Call p/91234567 about n/John & café visits!";
        assertParseSuccess(parser, "1 r/" + remark, new RemarkCommand(INDEX_FIRST_PERSON, new Remark(remark)));
    }

    @Test
    public void parse_repeatedRemark_usesLastValue() {
        assertParseSuccess(parser, "1 r/Likes baseball r/Likes swimming",
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Likes swimming")));
        assertParseSuccess(parser, "1 r/Likes baseball r/",
                new RemarkCommand(INDEX_FIRST_PERSON, new Remark("")));
    }

    @Test
    public void parse_missingIndex_failure() {
        assertParseFailure(parser, "", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, " r/Likes baseball", MESSAGE_INVALID_FORMAT);
    }

    @Test
    public void parse_invalidPreamble_failure() {
        assertParseFailure(parser, "0 r/Likes baseball", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "-1 r/Likes baseball", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "abc r/Likes baseball", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "2147483648 r/Likes baseball", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "1 Likes baseball", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "1 2 r/Likes baseball", MESSAGE_INVALID_FORMAT);
        assertParseFailure(parser, "1 p/91234567", MESSAGE_INVALID_FORMAT);
    }
}
