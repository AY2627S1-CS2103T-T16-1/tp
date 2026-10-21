package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.List;
import java.util.logging.Logger;

import seedu.tab.commons.core.LogsCenter;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.Command;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.ExitCommand;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.commands.HelpCommand;
import seedu.tab.logic.commands.ListCommand;
import seedu.tab.logic.commands.TagCommand;
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
     * <p>Invisible format characters are stripped first, by the same rule that normalizes a
     * stored field value. A direction mark or a zero-width space pasted before the command
     * word or an index otherwise leaves a word that looks right but matches no command, or an
     * index reported as not a positive integer.
     *
     * <p>Whatever separated the two is handed on as a single space, so that a command means
     * the same thing whichever one was typed. Only the add command reads its arguments with
     * the tokenizer; the rest look for an ordinary space and would otherwise keep the
     * separator as the first character of their arguments.
     *
     * <p>The command word is read in any case, because no two commands differ only in case
     * and so a capitalised word can only mean the one command. The fold reaches the first
     * word alone: {@code Find -n Alex} searches for {@code Alex} with its capital, and the word
     * is quoted back as typed when it is no command at all.
     *
     * @param userInput full user input string
     * @return the command based on the user input
     * @throws ParseException if the user input does not conform to the expected format
     */
    public Command parseCommand(String userInput) throws ParseException {
        String line = StringUtil.stripFormatCharacters(userInput);
        int wordStart = indexOfNextValue(line, 0);
        if (wordStart == NOT_FOUND) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
        }
        int wordEnd = indexOfNextSeparator(line, wordStart);
        int argumentsStart = indexOfNextValue(line, wordEnd);

        final String typedWord = line.substring(wordStart, wordEnd);
        final String commandWord = CommandWords.foldCase(typedWord);
        final String arguments = argumentsStart == NOT_FOUND
                ? ""
                : ARGUMENT_SEPARATOR + line.substring(argumentsStart);

        // Note to developers: Change LOG_LEVEL in LogsCenter to enable lower level (i.e., FINE, FINER and lower)
        // log messages such as the one below.
        // Lower level log messages are used sparingly to minimize noise in the code.
        logger.fine("Command word: " + commandWord + "; Arguments: " + arguments);

        return switch (commandWord) {
            case AddCommand.COMMAND_WORD -> new AddCommandParser().parse(arguments);
            case EditCommand.COMMAND_WORD -> new EditCommandParser().parse(arguments);
            case TagCommand.COMMAND_WORD -> new TagCommandParser().parse(arguments);
            case DeleteCommand.COMMAND_WORD -> new DeleteCommandParser().parse(arguments);
            case ClearCommand.COMMAND_WORD -> parseNoArgumentCommand(commandWord, arguments, new ClearCommand());
            case FindCommand.COMMAND_WORD -> new FindCommandParser().parse(arguments);
            case ListCommand.COMMAND_WORD -> parseNoArgumentCommand(commandWord, arguments, new ListCommand());
            case ExitCommand.COMMAND_WORD -> parseNoArgumentCommand(commandWord, arguments, new ExitCommand());
            case HelpCommand.COMMAND_WORD -> parseNoArgumentCommand(commandWord, arguments, new HelpCommand());
            default -> {
                logger.finer("This user input caused a ParseException: " + userInput);
                throw new ParseException(describeUnknownCommand(typedWord));
            }
        };
    }

    /**
     * Returns what to tell the User about a word that is no command, naming the word they
     * typed and the commands it was likely meant to be. Those are only ever named: running
     * one would act on a word the User never typed, and a word two edits from {@code delete}
     * is a guess even when it is the only guess going.
     */
    private static String describeUnknownCommand(String commandWord) {
        List<String> suggestions = CommandWords.suggestFor(commandWord);
        if (suggestions.isEmpty()) {
            return Messages.getErrorMessageForUnknownCommand(commandWord);
        }
        return Messages.getErrorMessageForMistypedCommand(commandWord, suggestions);
    }

    /**
     * Rejects unexpected input before a command with no arguments can execute.
     * Whitespace-only input has already been reduced to an empty argument string.
     */
    private static Command parseNoArgumentCommand(String commandWord, String arguments, Command command)
            throws ParseException {
        if (!arguments.isEmpty()) {
            throw new ParseException(Messages.getErrorMessageForUnexpectedArguments(
                    commandWord, arguments.substring(ARGUMENT_SEPARATOR.length())));
        }
        return command;
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
