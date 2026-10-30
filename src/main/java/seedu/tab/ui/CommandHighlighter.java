package seedu.tab.ui;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.parser.CommandTokenizer.ESCAPE;
import static seedu.tab.logic.parser.CommandTokenizer.OPTION_MARKER;
import static seedu.tab.logic.parser.CommandTokenizer.VALUE_SEPARATOR;

import java.util.ArrayList;
import java.util.List;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.parser.CommandTokenizer;

/**
 * Splits a command into the runs of text that are colored differently, so that the command
 * word, the options and the values are told apart as the user types.
 *
 * <p>This is only about appearance. It never rejects anything: a command is colored while it
 * is still half-typed, so an unclosed quote or an unknown word is ordinary here and is left to
 * the parser to complain about when the command is submitted. Where a token ends, though, it
 * asks the same questions the parser does: the command word ends at the first separator
 * whatever the quotes, a command pasted from a web page carries a non-breaking space, which
 * separates tokens for the parser, a backslash escapes only the characters that mean
 * something to it, and an option carrying its value after an equals sign is two things rather
 * than one. Deciding any of them differently here would color a command as something other
 * than what running it does.
 */
public class CommandHighlighter {

    /**
     * The part a run of text plays in the command, which decides how it is colored.
     */
    public enum Kind {
        COMMAND_WORD,
        OPTION,
        QUOTED_VALUE,
        VALUE,
        SPACE
    }

    /**
     * One run of text and the part it plays.
     *
     * @param text What the user typed, exactly, so that joining every span returns the command.
     * @param kind How it should be colored.
     */
    public record Span(String text, Kind kind) {}


    private static final int NOT_FOUND = -1;

    private CommandHighlighter() {} // this class only colors commands

    /**
     * Returns the spans of {@code command}, in order and covering every character of it, so
     * that the colored text reads exactly as it was typed.
     */
    public static List<Span> highlight(String command) {
        requireNonNull(command);

        List<Span> spans = new ArrayList<>();
        int index = addSeparators(command, 0, spans);
        if (index < command.length()) {
            index = addCommandWord(command, index, spans);
        }
        while (index < command.length()) {
            index = addSeparators(command, index, spans);
            if (index < command.length()) {
                index = addArgument(command, index, spans);
            }
        }
        return spans;
    }

    /**
     * Adds the run of separators at {@code start}, if there is one, and returns where it ends.
     */
    private static int addSeparators(String command, int start, List<Span> spans) {
        int index = start;
        while (index < command.length() && StringUtil.isWhitespace(command.charAt(index))) {
            index++;
        }
        if (index > start) {
            spans.add(new Span(command.substring(start, index), Kind.SPACE));
        }
        return index;
    }

    /**
     * Adds the command word at {@code start} and returns where it ends.
     *
     * <p>It ends at the first separator whatever the quotes, because that is where
     * {@code StudentBookParser} ends it. Reading it with the quote rule the values use would
     * draw {@code add"Ravi s/o Kumaran"} as one command word, and then the reply would report
     * the unknown command {@code add"Ravi}, splitting the line where no color ever did.
     */
    private static int addCommandWord(String command, int start, List<Span> spans) {
        int index = start;
        while (index < command.length() && !StringUtil.isWhitespace(command.charAt(index))) {
            index++;
        }
        spans.add(new Span(command.substring(start, index), Kind.COMMAND_WORD));
        return index;
    }

    /**
     * Adds the argument word at {@code start} and returns where it ends. An option carrying
     * its value after an equals sign becomes two spans, as the parser reads it.
     */
    private static int addArgument(String command, int start, List<Span> spans) {
        int markerEnd = findAttachedMarkerEnd(command, start);
        if (markerEnd == NOT_FOUND) {
            return addWord(command, start, spans, false);
        }
        spans.add(new Span(command.substring(start, markerEnd), Kind.OPTION));
        return addWord(command, markerEnd, spans, true);
    }

    /**
     * Returns where the marker of an option carrying its value after an equals sign ends,
     * counting the equals sign itself, or {@code NOT_FOUND} when the word at {@code start} is
     * not such an option.
     *
     * <p>{@code FlagTokenizer} reads {@code --tag="Lab 3"} as the option {@code --tag} and the
     * value {@code Lab 3}, so coloring the whole of it as one quoted value shows a value
     * where the parser sees an option. The marker is only taken when it is written plainly: a
     * quote or a backslash within it moves the equals sign the parser counts to, and a word
     * written that way is left whole rather than split in the wrong place.
     */
    private static int findAttachedMarkerEnd(String command, int start) {
        if (!CommandTokenizer.isOptionMarker(command.charAt(start))) {
            return NOT_FOUND;
        }
        for (int index = start + 1; index < command.length()
                && !StringUtil.isWhitespace(command.charAt(index)); index++) {
            char current = command.charAt(index);
            if (CommandTokenizer.isQuote(current) || current == ESCAPE) {
                return NOT_FOUND;
            }
            if (current == VALUE_SEPARATOR) {
                return index - start > OPTION_MARKER.length() ? index + 1 : NOT_FOUND;
            }
        }
        return NOT_FOUND;
    }

    /**
     * Adds the word at {@code start} and returns where it ends. An empty word adds nothing,
     * which is what an option left with no value after its equals sign leaves behind.
     *
     * @param command The input being highlighted.
     * @param start The first character of this word.
     * @param spans The highlighted spans receiving the word.
     * @param isAttachedValue Whether the word follows an option's equals sign, which makes it
     *     that option's value whatever it is written like.
     */
    private static int addWord(String command, int start, List<Span> spans, boolean isAttachedValue) {
        int index = start;
        boolean isQuoted = false;
        boolean wasQuoted = false;
        while (index < command.length()
                && (isQuoted || !StringUtil.isWhitespace(command.charAt(index)))) {
            char current = command.charAt(index);
            if (current == ESCAPE && index + 1 < command.length()
                    && CommandTokenizer.isEscapable(command.charAt(index + 1))) {
                index++;
            } else if (CommandTokenizer.isQuote(current)) {
                isQuoted = !isQuoted;
                wasQuoted = true;
            }
            index++;
        }

        String word = command.substring(start, index);
        if (!word.isEmpty()) {
            spans.add(new Span(word, determineKind(word, wasQuoted, isAttachedValue)));
        }
        return index;
    }

    /**
     * Returns the part {@code word} plays. A quoted word is always a value, which is how a
     * value opening with the option marker is told from an option, and so is a word the parser
     * has already read as the value of the option before it.
     */
    private static Kind determineKind(String word, boolean wasQuoted, boolean isAttachedValue) {
        if (wasQuoted) {
            return Kind.QUOTED_VALUE;
        }
        if (!isAttachedValue && word.length() > OPTION_MARKER.length()
                && CommandTokenizer.isOptionMarker(word.charAt(0))) {
            return Kind.OPTION;
        }
        return Kind.VALUE;
    }
}
