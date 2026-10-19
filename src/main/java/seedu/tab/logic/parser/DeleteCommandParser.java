package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;

import java.util.List;
import java.util.Optional;

import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.logic.parser.CommandTokenizer.Token;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a new DeleteCommand object
 */
public class DeleteCommandParser implements Parser<DeleteCommand> {

    private static final char VALUE_SEPARATOR = '=';

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
     * <p>An NUS Student ID that opens with a digit cannot be told from an index that way, so
     * the {@code --id} option, spelled as {@code add} and {@code edit} spell it, names the ID
     * explicitly instead.
     *
     * @throws ParseException if the user input does not conform to the expected format
     */
    public DeleteCommand parse(String args) throws ParseException {
        requireNonNull(args);
        List<Token> tokens = CommandTokenizer.tokenize(args);
        if (tokens.size() == 1) {
            return new DeleteCommand(readTarget(tokens.getFirst()));
        }
        if (tokens.size() == 2 && isNusStudentIdFlag(tokens.getFirst())) {
            return new DeleteCommand(readNusStudentIdTarget(tokens.get(1).value()));
        }
        throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
    }

    /**
     * Returns the target {@code token} holds: the NUS Student ID attached to its option, as in
     * {@code --id=A0123456X}, or else a position in the displayed list or an NUS Student ID,
     * as {@link ParserUtil#parseStudentTarget} reads it.
     *
     * @throws ParseException if {@code token} does not name a student.
     */
    private static StudentTarget readTarget(Token token) throws ParseException {
        Optional<String> attachedId = getAttachedNusStudentId(token);
        if (attachedId.isPresent()) {
            return readNusStudentIdTarget(attachedId.get());
        }
        return ParserUtil.parseStudentTarget(token.value(), DeleteCommand.MESSAGE_USAGE);
    }

    /**
     * Returns the target naming the student who holds the NUS Student ID {@code value}. The
     * option says which field the value belongs to, so an ID opening with a digit is read as
     * an ID rather than as a mistyped index.
     *
     * @throws ParseException if {@code value} is not a valid NUS Student ID.
     */
    private static StudentTarget readNusStudentIdTarget(String value) throws ParseException {
        return StudentTarget.byNusStudentId(ParserUtil.parseNusStudentId(value));
    }

    /** Returns true if {@code token} is one of the markers that name the NUS Student ID option. */
    private static boolean isNusStudentIdFlag(Token token) {
        return !token.isQuoted() && FLAG_NUS_STUDENT_ID.getMarkers().contains(token.value());
    }

    /**
     * Returns the value joined to the NUS Student ID option by an equals sign in {@code token},
     * or nothing when the token is not that option. A quoted token is a value, not an option.
     */
    private static Optional<String> getAttachedNusStudentId(Token token) {
        if (token.beginsQuoted()) {
            return Optional.empty();
        }
        int separator = token.value().indexOf(VALUE_SEPARATOR);
        if (separator <= 0) {
            return Optional.empty();
        }
        String marker = token.value().substring(0, separator);
        if (!FLAG_NUS_STUDENT_ID.getMarkers().contains(marker)) {
            return Optional.empty();
        }
        return Optional.of(token.value().substring(separator + 1));
    }

}
