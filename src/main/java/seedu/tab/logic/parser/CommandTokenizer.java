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

    private static final char QUOTE = '"';
    private static final char ESCAPE = '\\';

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

        List<Token> tokens = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean isQuoted = false;
        boolean wasQuoted = false;
        boolean beganQuoted = false;
        // a token can be empty yet real, which is what "" spells, so its presence is tracked apart
        boolean hasToken = false;

        for (int i = 0; i < arguments.length(); i++) {
            char current = arguments.charAt(i);
            if (current == ESCAPE && i + 1 < arguments.length() && isEscapable(arguments.charAt(i + 1))) {
                value.append(arguments.charAt(i + 1));
                hasToken = true;
                i++;
            } else if (current == QUOTE) {
                if (!hasToken) {
                    beganQuoted = true;
                }
                isQuoted = !isQuoted;
                wasQuoted = true;
                hasToken = true;
            } else if (!isQuoted && StringUtil.isWhitespace(current)) {
                if (hasToken) {
                    tokens.add(new Token(value.toString(), wasQuoted, beganQuoted));
                    value.setLength(0);
                    wasQuoted = false;
                    beganQuoted = false;
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
            tokens.add(new Token(value.toString(), wasQuoted, beganQuoted));
        }
        return tokens;
    }

    /**
     * Returns true if {@code character} is one a backslash may escape. Only the two characters
     * that mean something to the tokenizer are escapable, so a backslash before anything else
     * stays in the value rather than quietly disappearing. The colouring asks the same
     * question, so that what the User sees and what the parser reads agree on where a token
     * ends.
     */
    public static boolean isEscapable(char character) {
        return character == QUOTE || character == ESCAPE;
    }
}
