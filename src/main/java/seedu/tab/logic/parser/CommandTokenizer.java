package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Splits the arguments of a command into tokens the way a shell does: on whitespace, with
 * double quotes grouping a value that holds spaces.
 *
 * <p>Quoting is what lets a value hold a character the parser would otherwise read: a name
 * with spaces, or one opening with the hyphen that marks a flag. A quote character cannot
 * itself appear in a value, since there is no escape.
 */
public class CommandTokenizer {

    public static final String MESSAGE_UNCLOSED_QUOTE =
            "A quoted value is never closed. Add the closing \" or remove the opening one.";

    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';

    private CommandTokenizer() {} // this class only splits arguments

    /**
     * One token of a command's arguments.
     *
     * @param value The text of the token, with the quotes that grouped it removed.
     * @param isQuoted Whether a quote took part in it, which marks it as a value rather than
     *     something the parser may read as a flag.
     */
    public record Token(String value, boolean isQuoted) {}

    /**
     * Returns the tokens in {@code arguments}, with the quotes that group them removed.
     *
     * @throws ParseException if a quote is opened and never closed.
     */
    public static List<Token> tokenize(String arguments) throws ParseException {
        requireNonNull(arguments);

        List<Token> tokens = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean isQuoted = false;
        boolean wasQuoted = false;
        // a token can be empty yet real, which is what "" spells, so its presence is tracked apart
        boolean hasToken = false;

        for (int i = 0; i < arguments.length(); i++) {
            char current = arguments.charAt(i);
            if (current == ESCAPE && i + 1 < arguments.length() && isEscapable(arguments.charAt(i + 1))) {
                value.append(arguments.charAt(i + 1));
                hasToken = true;
                i++;
            } else if (current == QUOTE) {
                isQuoted = !isQuoted;
                wasQuoted = true;
                hasToken = true;
            } else if (!isQuoted && Character.isWhitespace(current)) {
                if (hasToken) {
                    tokens.add(new Token(value.toString(), wasQuoted));
                    value.setLength(0);
                    wasQuoted = false;
                    hasToken = false;
                }
            } else {
                value.append(current);
                hasToken = true;
            }
        }

        if (isQuoted) {
            throw new ParseException(MESSAGE_UNCLOSED_QUOTE);
        }
        if (hasToken) {
            tokens.add(new Token(value.toString(), wasQuoted));
        }
        return tokens;
    }

    /**
     * Returns true if {@code character} is one a backslash may escape. Only the two characters
     * that mean something to the tokenizer are escapable, so a backslash before anything else
     * stays in the value rather than quietly disappearing.
     */
    private static boolean isEscapable(char character) {
        return character == QUOTE || character == ESCAPE;
    }
}
