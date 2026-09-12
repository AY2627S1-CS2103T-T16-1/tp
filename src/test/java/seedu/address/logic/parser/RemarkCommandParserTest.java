package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_REMARK;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.RemarkCommand;
import seedu.address.model.person.Remark;

public class RemarkCommandParserTest {

    private static final String MESSAGE_INVALID_FORMAT =
            String.format(MESSAGE_INVALID_COMMAND_FORMAT, RemarkCommand.MESSAGE_USAGE);

    private RemarkCommandParser parser = new RemarkCommandParser();

    @Test
    public void parse_indexSpecified_success() {
        Index targetIndex = INDEX_FIRST_PERSON;
        String userInput = targetIndex.getOneBased() + " " + PREFIX_REMARK + "Likes to swim";

        assertParseSuccess(parser, userInput, new RemarkCommand(targetIndex, new Remark("Likes to swim")));
    }

    @Test
    public void parse_noRemark_success() {
        Index targetIndex = INDEX_FIRST_PERSON;
        String userInput = targetIndex.getOneBased() + " " + PREFIX_REMARK;

        assertParseSuccess(parser, userInput, new RemarkCommand(targetIndex, new Remark("")));
    }

    @Test
    public void parse_missingIndex_failure() {
        assertParseFailure(parser, PREFIX_REMARK + "Likes to swim", MESSAGE_INVALID_FORMAT);
    }

    @Test
    public void parse_invalidIndex_failure() {
        assertParseFailure(parser, "0 " + PREFIX_REMARK + "Likes to swim", MESSAGE_INVALID_FORMAT);
    }

    @Test
    public void parse_emptyArgs_failure() {
        assertParseFailure(parser, "", MESSAGE_INVALID_FORMAT);
    }

}
