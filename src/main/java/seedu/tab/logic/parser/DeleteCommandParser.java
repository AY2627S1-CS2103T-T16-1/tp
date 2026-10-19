package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.List;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.parser.CommandTokenizer.Token;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a new DeleteCommand object
 */
public class DeleteCommandParser implements Parser<DeleteCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the DeleteCommand
     * and returns a DeleteCommand object for execution.
     *
     * <p>The index is read as one token, the way {@code edit} reads its own preamble, so
     * that an index pasted from a web page or typed under a Chinese input method is read
     * as the number it looks like however the whitespace around it is spelled.
     *
     * @throws ParseException if the user input does not conform to the expected format
     */
    public DeleteCommand parse(String args) throws ParseException {
        List<Token> tokens = CommandTokenizer.tokenize(args);
        try {
            return new DeleteCommand(readIndex(tokens));
        } catch (ParseException pe) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE), pe);
        }
    }

    /**
     * Returns the index {@code tokens} holds. {@code delete} takes one and only one token:
     * no token where an index was asked for, or several where a single one names a student,
     * leave no index to be read.
     *
     * @throws ParseException if {@code tokens} does not hold exactly one valid index.
     */
    private static Index readIndex(List<Token> tokens) throws ParseException {
        if (tokens.size() != 1) {
            throw new ParseException(ParserUtil.MESSAGE_INVALID_INDEX);
        }
        return ParserUtil.parseIndex(tokens.get(0).value());
    }

}
