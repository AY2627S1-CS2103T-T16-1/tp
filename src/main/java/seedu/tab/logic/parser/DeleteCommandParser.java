package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;

import java.util.List;
import java.util.Optional;

import seedu.tab.logic.Messages;
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
     * <p>An NUS Student ID that opens with a digit cannot be told from an index that way, so
     * the {@code --id} option, spelled as {@code add} and {@code edit} spell it, names the ID
     * explicitly instead.
     *
     * @throws ParseException if the user input does not conform to the expected format.
     */
    @Override
    public DeleteCommand parse(String args) throws ParseException {
        requireNonNull(args);
        List<Token> tokens = CommandTokenizer.tokenize(args);
        if (tokens.isEmpty()) {
            throw new ParseException(DeleteCommand.MESSAGE_USAGE);
        }

        // Check for negative index in first token (like tokenizeWithIndex does)
        Token firstToken = tokens.getFirst();
        if (!firstToken.isQuoted() && firstToken.value().matches("-[0-9]+(?:\\.[0-9]+)?")) {
            // Report negative index error with unknown flag hint, as tokenizeWithIndex does
            throw new ParseException(ParserUtil.MESSAGE_INVALID_INDEX + "\n"
                    + String.format(Messages.MESSAGE_UNKNOWN_FLAG, Messages.shorten(firstToken.value())));
        }

        // Find the first flag token to detect extraneous values in preamble
        int firstFlagIndex = findFirstFlagIndex(tokens);
        if (firstFlagIndex > 1) {
            // Multiple tokens before first flag - check if first token is a valid target
            String firstValue = tokens.getFirst().value();
            if (isValidTargetStart(firstValue)) {
                String fieldName = Character.isLetter(firstValue.charAt(0))
                        ? ParserUtil.FIELD_NUS_STUDENT_ID
                        : DeleteCommand.FIELD_INDEX;
                throw new ParseException(formatExtraneousValueMessage(tokens.subList(1, firstFlagIndex), fieldName));
            }
        }

        // Use FlagTokenizer for proper flag error messages
        FlagArgumentMap arguments = FlagTokenizer.tokenizeTokens(tokens, FLAG_NUS_STUDENT_ID);
        arguments.verifyNoDuplicateFlagsFor(FLAG_NUS_STUDENT_ID);

        String preamble = arguments.getPreamble().trim();
        Optional<String> idValue = arguments.getValue(FLAG_NUS_STUDENT_ID);

        if (preamble.isEmpty() && idValue.isEmpty()) {
            throw new ParseException(DeleteCommand.MESSAGE_USAGE);
        }
        if (!preamble.isEmpty() && idValue.isPresent()) {
            throw new ParseException(DeleteCommand.MESSAGE_MULTIPLE_TARGETS);
        }

        StudentTarget target;
        if (idValue.isPresent()) {
            target = StudentTarget.byNusStudentId(ParserUtil.parseNusStudentId(idValue.get()));
        } else {
            target = parsePreambleTarget(preamble);
        }
        return new DeleteCommand(target);
    }

    /** Returns the index of the first token that looks like a flag, or tokens.size() if none. */
    private static int findFirstFlagIndex(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (!token.isQuoted() && token.value().startsWith(CommandTokenizer.OPTION_MARKER)
                    && token.value().length() > CommandTokenizer.OPTION_MARKER.length()) {
                return i;
            }
        }
        return tokens.size();
    }

    /** Returns true if {@code value} could be a valid target (index or letter-leading NUS Student ID). */
    private static boolean isValidTargetStart(String value) {
        if (value.isEmpty()) {
            return false;
        }
        char first = value.charAt(0);
        return Character.isDigit(first) || Character.isLetter(first);
    }

    /**
     * Returns the target the preamble names, offering {@code --id} when the value
     * refused as an index holds an NUS Student ID: a target starting with a digit reads as an
     * index, so the User who meant such an ID has no other way to reach their student.
     *
     * @throws ParseException if {@code preamble} does not name a student.
     */
    private static StudentTarget parsePreambleTarget(String preamble) throws ParseException {
        try {
            return ParserUtil.parseStudentTarget(preamble, DeleteCommand.MESSAGE_USAGE);
        } catch (ParseException e) {
            throw ParserUtil.withIdTargetHint(e, preamble, DeleteCommand.ID_TARGET_COMMAND);
        }
    }

    /**
     * Reports the tokens left after the target, naming the target by the form it was read as
     * rather than by the index form alone, so that a user who typed a name is not told they
     * typed an index. The recovery hint names {@code delete} rather than the one {@code edit}
     * and {@code tag} use, because those commands take field values and {@code delete} takes
     * none: telling a user to put the value after an option suggests a command delete cannot
     * run.
     */
    private static String formatExtraneousValueMessage(List<Token> extraTokens, String fieldName) {
        String values = extraTokens.stream().map(Token::value).reduce((a, b) -> a + " " + b).orElse("");
        return String.format(ParserUtil.MESSAGE_VALUE_AFTER_TARGET_ONLY,
                fieldName,
                Messages.shorten(values),
                DeleteCommand.COMMAND_WORD);
    }

}
