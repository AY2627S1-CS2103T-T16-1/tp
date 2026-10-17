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

    /**
     * How far from a command word opening with another character a typed word may be. One
     * edit is the whole of the difference between the two, so that edit is the first
     * character itself and the rest of the word already agrees.
     */
    private static final int MAX_EDITS_FROM_ANOTHER_OPENING = 1;

    /** How many equally likely words are worth naming before a list of them stops being help. */
    private static final int MAX_SUGGESTIONS = 3;

    private CommandWords() {} // this class only holds the words and the matching

    /**
     * Returns the command words {@code typedWord} is a likely attempt at, in the alphabetical
     * order of {@link #COMMAND_WORDS}. The list is empty when the word stands for none of
     * them, and when so many of them are equally likely that naming them all helps nobody.
     * Case is ignored, so {@code LIST} is read as an attempt at {@code list}.
     *
     * <p>A word that opens a command word is read as that command word being typed: {@code
     * del} stands for {@code delete} rather than for {@code help}, which is the nearer of the
     * two by edits alone. What was typed is right as far as it goes, which says more about
     * what was meant than how many edits away something else sits. Only a word that opens
     * nothing is measured for edits.
     *
     * <p>A word measured for edits has to open with the character the command word opens
     * with, unless the one edit between them is that character. The first character of a
     * command is the one a typist is least likely to get wrong, so a word two edits away that
     * disagrees on it as well has little to do with the command.
     *
     * <p>Words equally likely are all returned rather than one of them being chosen. A word
     * that stands for several commands says nothing about which was meant, and naming one of
     * them would dress a guess up as help; naming them all says what is known.
     *
     * <p>A suggestion is only ever reported back. None of them is acted on, because acting on
     * a guess would remove a student the User never named.
     */
    public static List<String> suggestFor(String typedWord) {
        requireNonNull(typedWord);

        String typed = typedWord.toLowerCase(Locale.ROOT);
        List<String> opened = wordsOpenedBy(typed);
        List<String> suggestions = opened.isEmpty() ? wordsNearestTo(typed) : opened;
        return suggestions.size() > MAX_SUGGESTIONS ? List.of() : suggestions;
    }

    /**
     * Returns the command words {@code typed} is the opening of, which is every one of them
     * when {@code typed} is empty.
     */
    private static List<String> wordsOpenedBy(String typed) {
        return COMMAND_WORDS.stream()
                .filter(commandWord -> commandWord.startsWith(typed))
                .toList();
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
        int allowedEdits = opensTheSameWay(commandWord, typed) ? MAX_EDITS : MAX_EDITS_FROM_ANOTHER_OPENING;
        return StringUtil.editDistance(typed, commandWord) <= allowedEdits;
    }

    /**
     * Returns true if {@code commandWord} and {@code typed} open with the same character.
     * {@code typed} has been folded to lower case, as the command words are written, so the
     * two characters are compared in the one case.
     */
    private static boolean opensTheSameWay(String commandWord, String typed) {
        return !typed.isEmpty() && commandWord.charAt(0) == typed.charAt(0);
    }

}
