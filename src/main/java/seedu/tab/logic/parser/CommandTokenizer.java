package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Splits the arguments of a command into tokens the way a shell does: on whitespace, with
 * double quotes grouping a value that holds spaces.
 *
 * <p>Quoting is what lets a value hold a character the parser would otherwise read: an
 * option value with spaces, or any value opening with the hyphen that marks an option. A
 * backslash escapes a quote or another backslash, so a value may hold either of them; a
 * backslash before anything else stays where it is.
 */
public class CommandTokenizer {

    public static final String MESSAGE_UNCLOSED_QUOTE =
            "A quoted value is never closed. Add the closing \" or remove the opening one.";

    /** Quote grouping an argument value. */
    public static final char QUOTE = '"';
    /** Escape for a quote or another escape character. */
    public static final char ESCAPE = '\\';

    /** Prefix identifying an unquoted option. */
    public static final String OPTION_MARKER = "-";
    /** Separator attaching an option value to its marker. */
    public static final char VALUE_SEPARATOR = '=';

    private static final char LEFT_CURLY_QUOTE = '“';
    private static final char RIGHT_CURLY_QUOTE = '”';
    private static final char FULL_WIDTH_QUOTE = '＂';

    private CommandTokenizer() {} // this class only splits arguments

    /**
     * One token of a command's arguments.
     *
     * @param value The text of the token, with the quotes that grouped it removed.
     * @param isQuoted Whether a quote took part in it, which normally marks it as a value
     *     rather than something the parser may read as a flag.
     * @param beginsQuoted Whether the token began with a quote, before quotes were removed.
     */
    public record Token(String value, boolean isQuoted, boolean beginsQuoted) {}

    /**
     * Returns the tokens in {@code arguments}, with the quotes that group them removed.
     *
     * @throws ParseException if a quote is opened and never closed.
     */
    public static List<Token> tokenize(String arguments) throws ParseException {
        requireNonNull(arguments);
        return new TokenScanner(arguments).readTokens();
    }

    /**
     * Returns whether the character groups a quoted value. Quote styles are interchangeable
     * so a pasted or partially converted pair behaves like straight double quotes.
     */
    public static boolean isQuote(char character) {
        return character == QUOTE || character == LEFT_CURLY_QUOTE
                || character == RIGHT_CURLY_QUOTE || character == FULL_WIDTH_QUOTE;
    }

    /**
     * Returns true if {@code character} is one a backslash may escape. Only characters
     * that mean something to the tokenizer are escapable, so a backslash before anything else
     * stays in the value rather than quietly disappearing. The coloring asks the same
     * question, so that what the User sees and what the parser reads agree on where a token
     * ends.
     */
    public static boolean isEscapable(char character) {
        return isQuote(character) || character == ESCAPE;
    }
    /**
     * Keeps the quote, escape and token state for one argument scan.
     */
    private static final class TokenScanner {
        private final String arguments;
        private final List<Token> tokens = new ArrayList<>();
        private final StringBuilder value = new StringBuilder();
        private int index;
        private boolean isQuoted;
        private boolean wasQuoted;
        private boolean beganQuoted;
        // Empty quotes form a token even when the buffer contains no characters.
        private boolean hasToken;

        private TokenScanner(String arguments) {
            this.arguments = arguments;
        }

        private List<Token> readTokens() throws ParseException {
            while (index < arguments.length()) {
                readCharacter();
            }
            if (isQuoted) {
                throw new ParseException(MESSAGE_UNCLOSED_QUOTE);
            }
            finishToken();
            return tokens;
        }

        private void readCharacter() {
            char current = arguments.charAt(index);
            if (current == ESCAPE && index + 1 < arguments.length()
                    && isEscapable(arguments.charAt(index + 1))) {
                append(arguments.charAt(++index));
            } else if (isQuote(current)) {
                acceptQuote();
            } else if (!isQuoted && StringUtil.isWhitespace(current)) {
                finishToken();
            } else {
                append(current);
            }
            index++;
        }

        private void append(char character) {
            value.append(character);
            hasToken = true;
        }

        private void acceptQuote() {
            if (!hasToken) {
                beganQuoted = true;
            }
            isQuoted = !isQuoted;
            wasQuoted = true;
            hasToken = true;
        }

        private void finishToken() {
            if (!hasToken) {
                return;
            }
            tokens.add(new Token(value.toString(), wasQuoted, beganQuoted));
            value.setLength(0);
            wasQuoted = false;
            beganQuoted = false;
            hasToken = false;
        }
    }

}
