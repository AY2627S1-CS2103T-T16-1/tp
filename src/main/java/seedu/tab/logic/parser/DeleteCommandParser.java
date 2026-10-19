package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.List;

import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.StudentTarget;
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
     * <p>The target is read as one token, the way {@code edit} reads its own preamble, so
     * that a value pasted from a web page or typed under a Chinese input method is read
     * as the value it looks like however the whitespace around it is spelled. The token is
     * then read as a student's address by {@link ParserUtil#parseStudentTarget}: a position
     * in the displayed list, or an NUS Student ID.
     *
     * @throws ParseException if the user input does not conform to the expected format
     */
    public DeleteCommand parse(String args) throws ParseException {
        requireNonNull(args);
        List<Token> tokens = CommandTokenizer.tokenize(args);
        if (tokens.size() != 1) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        }
        return new DeleteCommand(readTarget(tokens));
    }

    /**
     * Returns the target {@code tokens} holds. {@code delete} takes one and only one token:
     * no token where a student was asked for, or several where a single one names a student,
     * leave no target to be read.
     *
     * @throws ParseException if {@code tokens} does not hold exactly one usable target.
     */
    private static StudentTarget readTarget(List<Token> tokens) throws ParseException {
        return ParserUtil.parseStudentTarget(tokens.get(0).value(), DeleteCommand.MESSAGE_USAGE);
    }

}
