package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.tab.logic.Messages;
import seedu.tab.logic.parser.CommandTokenizer.Token;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Reads the arguments of a command as a name followed by flagged fields, the way a shell reads
 * an operand followed by options.
 *
 * <p>The tokens before the first flag are the preamble, joined back together with single
 * spaces, which each command reads as whatever it takes in that position. Every flag after it
 * takes the one token that follows it. A token belonging to no flag, an unknown flag, and a
 * flag left without a value are each reported for what they are.
 */
public class FlagTokenizer {

    private static final String FLAG_MARKER = "-";

    private FlagTokenizer() {} // this class only reads arguments

    /**
     * Returns the preamble and the flag values in {@code arguments}, which is tokenized
     * first so that a quoted value may hold spaces or open with the flag marker.
     *
     * @throws ParseException if a quote is left open, or a token belongs to no known flag.
     */
    public static FlagArgumentMap tokenize(String arguments, Flag... knownFlags) throws ParseException {
        requireNonNull(arguments);

        List<Token> tokens = CommandTokenizer.tokenize(arguments);
        // a caller that names the same marker twice means one flag, not a broken collector
        Map<String, Flag> flagsByMarker = Stream.of(knownFlags)
                .collect(Collectors.toMap(Flag::getFlag, Function.identity(), (first, second) -> first));

        FlagArgumentMap parsed = new FlagArgumentMap();
        int firstFlag = readPreamble(tokens, parsed);
        readFlags(tokens, firstFlag, flagsByMarker, parsed);
        return parsed;
    }

    /**
     * Reads the tokens before the first flag into the preamble of {@code parsed}, and returns
     * the index of the token that ended it.
     */
    private static int readPreamble(List<Token> tokens, FlagArgumentMap parsed) {
        List<String> preambleTokens = new ArrayList<>();
        int index = 0;
        while (index < tokens.size() && !isFlag(tokens.get(index))) {
            preambleTokens.add(tokens.get(index).value());
            index++;
        }
        parsed.setPreamble(String.join(" ", preambleTokens));
        return index;
    }

    /**
     * Reads each flag from {@code index} onwards together with the token that follows it.
     */
    private static void readFlags(List<Token> tokens, int index, Map<String, Flag> flagsByMarker,
            FlagArgumentMap parsed) throws ParseException {
        while (index < tokens.size()) {
            Token token = tokens.get(index);
            if (!isFlag(token)) {
                throw new ParseException(String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, token.value()));
            }

            Flag flag = flagsByMarker.get(token.value());
            if (flag == null) {
                throw new ParseException(String.format(Messages.MESSAGE_UNKNOWN_FLAG, token.value()));
            }
            // an option in the value position means the one before it was left empty, not that
            // the user wants to store "-e" as a tag
            if (index + 1 >= tokens.size() || isFlag(tokens.get(index + 1))) {
                throw new ParseException(String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, flag.getLabel()));
            }

            parsed.put(flag, tokens.get(index + 1).value());
            index += 2;
        }
    }

    /**
     * Returns true if {@code token} marks a field rather than being a value. A quoted token is
     * always a value, which is how a name opening with the flag marker is given.
     */
    private static boolean isFlag(Token token) {
        return !token.isQuoted()
                && token.value().startsWith(FLAG_MARKER)
                && token.value().length() > FLAG_MARKER.length();
    }
}
