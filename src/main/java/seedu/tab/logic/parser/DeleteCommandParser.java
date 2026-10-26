package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CommandTokenizer.OPTION_MARKER;
import static seedu.tab.logic.parser.CommandTokenizer.VALUE_SEPARATOR;

import java.util.List;
import java.util.Optional;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.logic.parser.CommandTokenizer.Token;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.NusStudentId;

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
     * <p>An NUS Student ID that opens with a digit cannot be told from an index that way, so
     * the {@code --id} option, spelled as {@code add} and {@code edit} spell it, names the ID
     * explicitly instead.
     *
     * <p>More than one token with no option spelled among them leaves the tokens after the
     * first named to the user, so that the part to remove is clear, while a token spelled the
     * way an option is spelled takes the usage message, which shows every form delete reads.
     *
     * @throws ParseException if the user input does not conform to the expected format
     */
    public DeleteCommand parse(String args) throws ParseException {
        requireNonNull(args);
        List<Token> tokens = CommandTokenizer.tokenize(args);
        if (tokens.isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        }
        if (tokens.size() == 1) {
            return new DeleteCommand(readTarget(tokens.getFirst()));
        }
        if (tokens.size() == 2 && isNusStudentIdFlag(tokens.getFirst())) {
            return new DeleteCommand(readNusStudentIdTarget(tokens.get(1).value()));
        }
        if (tokens.stream().noneMatch(DeleteCommandParser::looksLikeOption)) {
            throw new ParseException(extraneousValueMessage(tokens));
        }
        throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
    }

    /**
     * Returns the target {@code token} holds: the NUS Student ID attached to its option, as in
     * {@code --id=A0123456X}, or else a position in the displayed list or an NUS Student ID,
     * as {@link ParserUtil#parseStudentTarget} reads it.
     *
     * <p>A target that is not a usable index keeps the concise reason it was refused, as
     * {@code edit} and {@code tag} report theirs. A value that reads as a valid NUS Student
     * ID names the {@code --id} command that addresses it, because such a value plausibly
     * was meant as an ID rather than as an index.
     *
     * @throws ParseException if {@code token} does not name a student.
     */
    private static StudentTarget readTarget(Token token) throws ParseException {
        Optional<String> attachedId = getAttachedNusStudentId(token);
        if (attachedId.isPresent()) {
            return readNusStudentIdTarget(attachedId.get());
        }
        if (looksLikeOption(token)) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE));
        }
        try {
            return ParserUtil.parseStudentTarget(token.value(), DeleteCommand.MESSAGE_USAGE);
        } catch (ParseException e) {
            if (NusStudentId.isValidNusStudentId(token.value())) {
                throw new ParseException(e.getMessage() + " "
                        + Messages.getIdTargetHint(DeleteCommand.COMMAND_WORD, token.value()), e);
            }
            throw e;
        }
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
     * Returns true if {@code token} is spelled the way an option is spelled, whatever quotes
     * grouped it. {@code delete} reads an option as one of its own forms rather than as the
     * value a second token would be, so the usage message, not the reply naming the value
     * after the index, is the one that helps.
     *
     * <p>A marker followed by a digit, as in {@code -1}, is a negative number rather than an
     * option, and reads as a mistyped index: the reason that index is unusable helps more
     * than the forms of a command the user already spelled correctly.
     */
    private static boolean looksLikeOption(Token token) {
        String value = token.value();
        if (!value.startsWith(OPTION_MARKER) || value.length() <= OPTION_MARKER.length()) {
            return false;
        }
        return !Character.isDigit(value.charAt(OPTION_MARKER.length()));
    }

    /**
     * Reports the tokens left after the index. The recovery hint names {@code delete} rather
     * than the one {@code edit} and {@code tag} use, because those commands take field values
     * and {@code delete} takes none: telling a user to put the value after an option suggests
     * a command delete cannot run.
     */
    private static String extraneousValueMessage(List<Token> tokens) {
        List<Token> values = tokens.subList(1, tokens.size());
        return String.format(ParserUtil.MESSAGE_VALUE_AFTER_INDEX_ONLY, DeleteCommand.FIELD_INDEX,
                Messages.shorten(String.join(" ", values.stream().map(Token::value).toList())),
                DeleteCommand.COMMAND_WORD);
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
