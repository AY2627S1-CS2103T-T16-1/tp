package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.tab.logic.Messages;
import seedu.tab.logic.parser.CommandTokenizer.Token;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Reads the arguments of a command as a preamble followed by flagged fields, the way a shell
 * reads an operand followed by options.
 *
 * <p>The tokens before the first flag are the preamble, joined back together with single
 * spaces, which each command reads as whatever it takes in that position. Every value-taking
 * flag after it takes the one token that follows it, or the value joined to it by an equals
 * sign, while a presence-only flag consumes no value. A token belonging to no flag, an unknown
 * flag, and a value-taking flag left without a value are each reported for what they are.
 */
public class FlagTokenizer {

    private static final String FLAG_MARKER = "-";
    private static final char VALUE_SEPARATOR = '=';

    private record AttachedFlagValue(Flag flag, String marker, String value) {}

    private FlagTokenizer() {} // this class only reads arguments

    /**
     * Returns the preamble and the flag values in {@code arguments}, which is tokenized
     * first so that a quoted value may hold spaces or open with the flag marker.
     *
     * @throws ParseException if a quote is left open, or a token belongs to no known flag.
     */
    public static FlagArgumentMap tokenize(String arguments, Flag... knownFlags) throws ParseException {
        requireNonNull(arguments);

        return readTokens(CommandTokenizer.tokenize(arguments), knownFlags);
    }

    /**
     * Reads arguments whose preamble is an index. An unquoted negative number could be an
     * index or an option, so reports both reasons it is rejected before reading the flags.
     *
     * @throws ParseException if the index looks like an invalid option or tokenization fails.
     */
    public static FlagArgumentMap tokenizeWithIndex(String arguments, Flag... knownFlags) throws ParseException {
        List<Token> tokens = CommandTokenizer.tokenize(arguments);
        if (!tokens.isEmpty()) {
            Token first = tokens.getFirst();
            if (!first.isQuoted() && first.value().matches("-[0-9]+(?:\\.[0-9]+)?")) {
                throw new ParseException(ParserUtil.MESSAGE_INVALID_INDEX + "\n"
                        + String.format(Messages.MESSAGE_UNKNOWN_FLAG, Messages.shorten(first.value())));
            }
        }
        return readTokens(tokens, knownFlags);
    }

    /** Reads already split tokens, sharing flag handling between index and name preambles. */
    private static FlagArgumentMap readTokens(List<Token> tokens, Flag... knownFlags) throws ParseException {
        // a caller that names the same marker twice means one flag, not a broken collector
        Map<String, Flag> flagsByMarker = Stream.of(knownFlags)
                .flatMap(flag -> flag.getMarkers().stream().map(marker -> Map.entry(marker, flag)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first));

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
        while (index < tokens.size() && !isOption(tokens.get(index))) {
            preambleTokens.add(tokens.get(index).value());
            index++;
        }
        parsed.setPreamble(String.join(" ", preambleTokens));
        return index;
    }

    /**
     * Reads each flag from {@code index} onwards, together with its value when it takes one.
     */
    private static void readFlags(List<Token> tokens, int index, Map<String, Flag> flagsByMarker,
            FlagArgumentMap parsed) throws ParseException {
        while (index < tokens.size()) {
            index = readFlag(tokens, index, flagsByMarker, parsed);
        }
    }

    /** Reads the flag at {@code index} and returns the index after its optional value. */
    private static int readFlag(List<Token> tokens, int index, Map<String, Flag> flagsByMarker,
            FlagArgumentMap parsed) throws ParseException {
        Token token = tokens.get(index);
        AttachedFlagValue attached = getAttachedFlagValue(token, flagsByMarker);
        if (attached != null) {
            parsed.put(attached.flag(), attached.marker(), attached.value());
            return index + 1;
        }

        Flag flag = getFlag(token, flagsByMarker);
        if (!flag.takesValue()) {
            parsed.put(flag, token.value(), "");
            return index + 1;
        }
        // an option in the value position means the one before it was left empty, not that
        // the user wants to store "-e" as a tag
        if (index + 1 >= tokens.size() || isOption(tokens.get(index + 1))) {
            throw new ParseException(String.format(
                    Messages.MESSAGE_FLAG_WITHOUT_VALUE, flag.getLabel(token.value())));
        }

        parsed.put(flag, token.value(), tokens.get(index + 1).value());
        return index + 2;
    }

    /** Returns the known flag named by {@code token}. */
    private static Flag getFlag(Token token, Map<String, Flag> flagsByMarker) throws ParseException {
        if (!isOption(token)) {
            throw new ParseException(String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, token.value()));
        }

        Flag flag = flagsByMarker.get(token.value());
        if (flag == null) {
            throw new ParseException(String.format(Messages.MESSAGE_UNKNOWN_FLAG, token.value()));
        }
        return flag;
    }

    /**
     * Returns true if {@code token} marks a field rather than being a value. A quoted token is
     * always a value, which is how a value opening with the flag marker is given.
     */
    private static boolean isFlag(Token token) {
        return !token.isQuoted()
                && token.value().startsWith(FLAG_MARKER)
                && token.value().length() > FLAG_MARKER.length();
    }

    /** Returns true if {@code token} looks like either supported option form. */
    private static boolean isOption(Token token) {
        return isFlag(token) || isAttachedOption(token);
    }

    /** Returns true if {@code token} has an unquoted option marker before an equals sign. */
    private static boolean isAttachedOption(Token token) {
        int separator = token.value().indexOf(VALUE_SEPARATOR);
        return !token.beginsQuoted()
                && token.value().startsWith(FLAG_MARKER)
                && separator > FLAG_MARKER.length();
    }

    /** Returns the known value-taking flag and value joined in {@code token}, if any. */
    private static AttachedFlagValue getAttachedFlagValue(Token token, Map<String, Flag> flagsByMarker) {
        if (!isAttachedOption(token)) {
            return null;
        }

        int separator = token.value().indexOf(VALUE_SEPARATOR);
        String marker = token.value().substring(0, separator);
        Flag flag = flagsByMarker.get(marker);
        if (flag == null || !flag.takesValue()) {
            return null;
        }
        return new AttachedFlagValue(flag, marker, token.value().substring(separator + 1));
    }
}
