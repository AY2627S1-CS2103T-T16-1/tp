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

        FlagArgumentMap arguments = FlagTokenizer.tokenizeWithIndexTokens(tokens, FLAG_NUS_STUDENT_ID);
        arguments.verifyNoDuplicateFlagsFor(FLAG_NUS_STUDENT_ID);
        verifyNoExtraTargets(tokens.getFirst(), arguments.getPreamble());

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
    private static void verifyNoExtraTargets(Token firstToken, String preamble) throws ParseException {
        String firstValue = firstToken.value();
        if (preamble.length() <= firstValue.length() || firstValue.isEmpty()) {
            return;
        }
        char first = firstValue.charAt(0);
        if (Character.isDigit(first) || Character.isLetter(first)) {
            String values = preamble.substring(firstValue.length() + 1);
            throw new ParseException(String.format(ParserUtil.MESSAGE_VALUE_AFTER_TARGET_ONLY,
                    ParserUtil.getTargetFieldName(firstValue, DeleteCommand.FIELD_INDEX),
                    Messages.shorten(values), DeleteCommand.COMMAND_WORD));
        }
    }

}
