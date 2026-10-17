package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.ExitCommand;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.commands.HelpCommand;
import seedu.tab.logic.commands.ListCommand;

/**
 * The words TAB answers to, and which of them a word it does not answer to was likely meant to
 * be.
 */
public class CommandWords {

    /**
     * Every command word, in alphabetical order, which is also the order a tie between two
     * equally near words is settled in.
     *
     * <p>{@code StudentBookParser} dispatches on these same words. The two are held to each
     * other by a test rather than by the compiler, because a word missing from here would
     * otherwise be suggested to nobody while still working when typed.
     */
    public static final List<String> COMMAND_WORDS = List.of(
            AddCommand.COMMAND_WORD,
            ClearCommand.COMMAND_WORD,
            DeleteCommand.COMMAND_WORD,
            EditCommand.COMMAND_WORD,
            ExitCommand.COMMAND_WORD,
            FindCommand.COMMAND_WORD,
            HelpCommand.COMMAND_WORD,
            ListCommand.COMMAND_WORD);

    /** The longest a command word can be while two edits would leave too little of it. */
    private static final int SHORT_COMMAND_WORD_LENGTH = 4;
    private static final int MAX_EDITS = 2;
    private static final int MAX_EDITS_FOR_SHORT_COMMAND_WORD = 1;

    /**
     * Returns the command word {@code typedWord} was most likely meant to be, or nothing when
     * it is too far from every one of them for a guess to be worth making. Case is ignored, so
     * {@code LIST} is read as an attempt at {@code list}.
     *
     * <p>The nearest word wins, and the alphabetically first of several equally near ones, so
     * that the same typo is always answered with the same word. A near miss is only ever
     * reported back: no suggestion is acted on, because a word two edits from {@code delete}
     * is a guess, and guessing wrong would remove a student the User never named.
     */
    public static Optional<String> findClosestTo(String typedWord) {
        requireNonNull(typedWord);

        String typed = typedWord.toLowerCase(Locale.ROOT);
        return COMMAND_WORDS.stream()
                .filter(commandWord -> isLikelyAttemptAt(commandWord, typed))
                .min(Comparator.comparingInt((String commandWord) -> StringUtil.editDistance(typed, commandWord))
                        .thenComparing(Comparator.naturalOrder()));
    }

    /**
     * Returns true if {@code typed} is near enough to {@code commandWord} to be a mistyping of
     * it. The lengths are compared first, which settles a pasted line against every command
     * word without measuring it against any of them.
     */
    private static boolean isLikelyAttemptAt(String commandWord, String typed) {
        int allowedEdits = allowedEditsFor(commandWord);
        if (Math.abs(commandWord.length() - typed.length()) > allowedEdits) {
            return false;
        }
        return StringUtil.editDistance(typed, commandWord) <= allowedEdits;
    }

    /**
     * Returns how far from {@code commandWord} a word may be and still be read as a mistyping
     * of it. Two edits in a word of four letters or fewer change half of it, which leaves too
     * little of the word to tell a slip from a word the User meant to type, so a short command
     * word is only ever suggested for a single slip.
     */
    private static int allowedEditsFor(String commandWord) {
        return commandWord.length() > SHORT_COMMAND_WORD_LENGTH ? MAX_EDITS : MAX_EDITS_FOR_SHORT_COMMAND_WORD;
    }

}
