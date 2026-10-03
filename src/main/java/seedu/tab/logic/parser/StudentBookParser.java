package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.util.logging.Logger;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.Command;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.ExitCommand;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.commands.HelpCommand;
import seedu.tab.logic.commands.ListCommand;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Parses user input.
 */
public class StudentBookParser {

    private static final int NOT_FOUND = -1;

    /**
     * What the separators between the command word and its arguments are handed on as. The
     * parsers below read the rest of the line themselves, and most of them know only this one,
     * so giving them anything else makes a command mean different things by which space was
     * typed.
     */
    private static final String ARGUMENT_SEPARATOR = " ";
    private static final Logger logger = LogsCenter.getLogger(StudentBookParser.class);

    /**
     * Parses user input into command for execution.
     *
     * <p>The command word is separated from its arguments by {@link StringUtil#isWhitespace},
     * the same rule the tokenizer splits the arguments on. Spelling the rule a second time as
     * a pattern is what let a command pasted from a web page be refused as unknown.
     *
     * <p>Whatever separated the two is handed on as a single space, so that a command means
     * the same thing whichever one was typed. Only the add command reads its arguments with
     * the tokenizer; the rest look for an ordinary space and would otherwise keep the
     * separator as the first character of their arguments.
     *
     * @param userInput full user input string
     * @return the command based on the user input
     * @throws ParseException if the user input does not conform to the expected format
     */
    public Command parseCommand(String userInput) throws ParseException {
        int wordStart = indexOfNextValue(userInput, 0);
        if (wordStart == NOT_FOUND) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
        }
        int wordEnd = indexOfNextSeparator(userInput, wordStart);
        int argumentsStart = indexOfNextValue(userInput, wordEnd);

        final String commandWord = userInput.substring(wordStart, wordEnd);
        final String arguments = argumentsStart == NOT_FOUND
                ? ""
                : ARGUMENT_SEPARATOR + userInput.substring(argumentsStart);

        // Note to developers: Change LOG_LEVEL in LogsCenter to enable lower level (i.e., FINE, FINER and lower)
        // log messages such as the one below.
        // Lower level log messages are used sparingly to minimize noise in the code.
        logger.fine("Command word: " + commandWord + "; Arguments: " + arguments);

        return switch (commandWord) {
            case AddCommand.COMMAND_WORD -> new AddCommandParser().parse(arguments);
            case EditCommand.COMMAND_WORD -> new EditCommandParser().parse(arguments);
            case DeleteCommand.COMMAND_WORD -> new DeleteCommandParser().parse(arguments);
            case ClearCommand.COMMAND_WORD -> new ClearCommand();
            case FindCommand.COMMAND_WORD -> new FindCommandParser().parse(arguments);
            case ListCommand.COMMAND_WORD -> new ListCommand();
            case ExitCommand.COMMAND_WORD -> new ExitCommand();
            case HelpCommand.COMMAND_WORD -> new HelpCommand();
            default -> {
                logger.finer("This user input caused a ParseException: " + userInput);
                throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
            }
        };
    }


    /**
     * Returns the index of the first character at or after {@code from} that separates nothing,
     * or {@code NOT_FOUND} when the rest of {@code input} is all separators.
     */
    private static int indexOfNextValue(String input, int from) {
        for (int i = from; i < input.length(); i++) {
            if (!StringUtil.isWhitespace(input.charAt(i))) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    /**
     * Returns the index of the first separator at or after {@code from}, or the end of
     * {@code input} when there is none.
     */
    private static int indexOfNextSeparator(String input, int from) {
        for (int i = from; i < input.length(); i++) {
            if (StringUtil.isWhitespace(input.charAt(i))) {
                return i;
            }
        }
        return input.length();
    }

}
