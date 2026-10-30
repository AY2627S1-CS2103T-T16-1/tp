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
import seedu.tab.logic.commands.TagCommand;

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
            ListCommand.COMMAND_WORD,
            TagCommand.COMMAND_WORD);

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

    /** A command word a typed word may stand for, with how far the typed word is from it. */
    private record Candidate(String word, int editsFromTyped) {}

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
        String typed = foldCase(typedWord);
        List<String> matchingWords = findWordsStartingWith(typed);
        List<String> suggestions = matchingWords.isEmpty() ? findNearestWords(typed) : matchingWords;
        return suggestions.size() > MAX_SUGGESTIONS ? List.of() : suggestions;
    }

    /**
     * Returns {@code typedWord} in the case the command words are written in, so that a word
     * reaches the same command and the same suggestions whichever case it was typed in. The
     * fold ignores the User's locale, which in a Turkish one would turn {@code LIST} into a
     * word TAB answers to in neither place.
     */
    public static String foldCase(String typedWord) {
        requireNonNull(typedWord);

        return typedWord.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the command words {@code typed} is the opening of, which is every one of them
     * when {@code typed} is empty.
     */
    private static List<String> findWordsStartingWith(String typed) {
        return COMMAND_WORDS.stream()
                .filter(commandWord -> commandWord.startsWith(typed))
                .toList();
    }

    /**
     * Returns the command words {@code typed} is nearest to, among those near enough to be a
     * mistyping of one, or an empty list when none of them is.
     */
    private static List<String> findNearestWords(String typed) {
        List<Candidate> candidates = COMMAND_WORDS.stream()
                .filter(word -> Math.abs(word.length() - typed.length()) <= MAX_EDITS)
                .map(word -> new Candidate(word, StringUtil.computeEditDistance(typed, word)))
                .filter(candidate -> isLikelyAttemptAt(candidate.word(), typed, candidate.editsFromTyped()))
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }
        int nearestDistance = candidates.stream().mapToInt(Candidate::editsFromTyped).min().orElseThrow();
        return candidates.stream()
                .filter(candidate -> candidate.editsFromTyped() == nearestDistance)
                .map(Candidate::word)
                .toList();
    }

    /**
     * Returns true if the computed distance is within the allowance for the two initial characters.
     * The caller excludes incompatible lengths before calculating the distance.
     */
    private static boolean isLikelyAttemptAt(String commandWord, String typed, int distance) {
        int allowedEdits = hasSameInitialCharacter(commandWord, typed) ? MAX_EDITS : MAX_EDITS_FROM_ANOTHER_OPENING;
        return distance <= allowedEdits;
    }

    /**
     * Returns true if {@code commandWord} and {@code typed} open with the same character.
     * {@code typed} has been folded to lower case, as the command words are written, so the
     * two characters are compared in the one case.
     *
     * <p>{@code typed} holds something, because every command word opens with the empty
     * string, so an empty one is answered by {@link #findWordsStartingWith} and never measured here.
     */
    private static boolean hasSameInitialCharacter(String commandWord, String typed) {
        return commandWord.charAt(0) == typed.charAt(0);
    }

}
