package seedu.tab.ui;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.parser.CommandTokenizer;

/**
 * Splits a command into the runs of text that are coloured differently, so that the command
 * word, the options and the values are told apart as the User types.
 *
 * <p>This is only about appearance. It never rejects anything: a command is coloured while it
 * is still half-typed, so an unclosed quote or an unknown word is ordinary here and is left to
 * the parser to complain about when the command is submitted. Where a token ends, though, it
 * asks the same questions the tokenizer does: a command pasted from a web page carries a
 * non-breaking space, which separates tokens for the parser, and a backslash escapes only the
 * two characters that mean something to it. Deciding either differently here would colour a
 * command as something other than what running it does.
 */
public class CommandHighlighter {

    /** The part a run of text plays in the command, which decides how it is coloured. */
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
     * @param text What the User typed, exactly, so that joining every span returns the command.
     * @param kind How it should be coloured.
     */
    public record Span(String text, Kind kind) {}

    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';

    private static final String OPTION_MARKER = "-";

    private CommandHighlighter() {} // this class only colours commands

    /**
     * Returns the spans of {@code command}, in order and covering every character of it, so
     * that the coloured text reads exactly as it was typed.
     */
    public static List<Span> highlight(String command) {
        requireNonNull(command);

        List<Span> spans = new ArrayList<>();
        int index = 0;
        boolean isFirstWord = true;

        while (index < command.length()) {
            if (StringUtil.isWhitespace(command.charAt(index))) {
                int start = index;
                while (index < command.length() && StringUtil.isWhitespace(command.charAt(index))) {
                    index++;
                }
                spans.add(new Span(command.substring(start, index), Kind.SPACE));
                continue;
            }

            int start = index;
            boolean wasQuoted = false;
            boolean isQuoted = false;
            while (index < command.length()
                    && (isQuoted || !StringUtil.isWhitespace(command.charAt(index)))) {
                char current = command.charAt(index);
                if (current == ESCAPE && index + 1 < command.length()
                        && CommandTokenizer.isEscapable(command.charAt(index + 1))) {
                    index++;
                } else if (current == QUOTE) {
                    isQuoted = !isQuoted;
                    wasQuoted = true;
                }
                index++;
            }

            String word = command.substring(start, index);
            spans.add(new Span(word, kindOf(word, wasQuoted, isFirstWord)));
            isFirstWord = false;
        }

        return spans;
    }

    /**
     * Returns the part {@code word} plays. A quoted word is always a value, which is how a
     * value opening with the option marker is told from an option.
     */
    private static Kind kindOf(String word, boolean wasQuoted, boolean isFirstWord) {
        if (isFirstWord) {
            return Kind.COMMAND_WORD;
        }
        if (wasQuoted) {
            return Kind.QUOTED_VALUE;
        }
        if (word.startsWith(OPTION_MARKER) && word.length() > OPTION_MARKER.length()) {
            return Kind.OPTION;
        }
        return Kind.VALUE;
    }
}
