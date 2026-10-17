package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Locale;

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
     * Every command word, in alphabetical order, which is also the order several equally
     * likely ones are named in.
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

    /** How far from a command word a typed word may be and still be read as a mistyping of it. */
    private static final int MAX_EDITS = 2;

    /** How many equally likely words are worth naming before a list of them stops being help. */
    private static final int MAX_SUGGESTIONS = 3;

    /**
     * Returns the command words {@code typedWord} is a likely attempt at: those nearest to
     * it, in the alphabetical order of {@link #COMMAND_WORDS}. The list is empty when the
     * word is too far from every command word, and when so many of them are equally near
     * that naming them all helps nobody. Case is ignored, so {@code LIST} is read as an
     * attempt at {@code list}.
     *
     * <p>Words equally near the typed one are all returned rather than one of them being
     * chosen. A word two edits from several commands says nothing about which was meant, and
     * naming one of them would dress a guess up as help; naming them all says what is known.
     *
     * <p>A suggestion is only ever reported back. None of them is acted on, because acting on
     * a guess would remove a student the User never named.
     */
    public static List<String> suggestFor(String typedWord) {
        requireNonNull(typedWord);

        List<String> nearest = wordsNearestTo(typedWord.toLowerCase(Locale.ROOT));
        return nearest.size() > MAX_SUGGESTIONS ? List.of() : nearest;
    }

    /**
     * Returns the command words {@code typed} is nearest to, among those near enough to be a
     * mistyping of one, or an empty list when none of them is.
     */
    private static List<String> wordsNearestTo(String typed) {
        List<String> nearMisses = COMMAND_WORDS.stream()
                .filter(commandWord -> isLikelyAttemptAt(commandWord, typed))
                .toList();
        if (nearMisses.isEmpty()) {
            return List.of();
        }

        int nearestDistance = nearMisses.stream()
                .mapToInt(commandWord -> StringUtil.editDistance(typed, commandWord))
                .min()
                .orElseThrow();
        return nearMisses.stream()
                .filter(commandWord -> StringUtil.editDistance(typed, commandWord) == nearestDistance)
                .toList();
    }

    /**
     * Returns true if {@code typed} is near enough to {@code commandWord} to be a mistyping of
     * it. The lengths are compared first, which settles a pasted line against every command
     * word without measuring it against any of them.
     */
    private static boolean isLikelyAttemptAt(String commandWord, String typed) {
        if (Math.abs(commandWord.length() - typed.length()) > MAX_EDITS) {
            return false;
        }
        return StringUtil.editDistance(typed, commandWord) <= MAX_EDITS;
    }

}
