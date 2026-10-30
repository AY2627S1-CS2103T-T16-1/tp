package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.tab.logic.parser.CommandTokenizer.OPTION_MARKER;
import static seedu.tab.logic.parser.CommandTokenizer.VALUE_SEPARATOR;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.tab.logic.Messages;
import seedu.tab.logic.parser.CommandTokenizer.Token;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.logic.parser.exceptions.UnknownFlagException;
import seedu.tab.logic.parser.exceptions.ValueAfterFlagsException;

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
    private static final Pattern NEGATIVE_INDEX_PATTERN = Pattern.compile("-[0-9]+(?:\\.[0-9]+)?");
    private static final String LONG_OPTION_MARKER = OPTION_MARKER.repeat(2);


    private record AttachedOption(String marker, String value) {}

    private FlagTokenizer() {} // this class only reads arguments

    /**
     * Returns the preamble and the flag values in {@code arguments}, which is tokenized
     * first so that a quoted value may hold spaces or open with the flag marker.
     *
     * @throws ParseException if a quote is left open, or a token belongs to no known flag.
     */
    public static FlagArgumentMap tokenize(String arguments, CliFlag... knownFlags) throws ParseException {
        requireNonNull(arguments);

        return tokenizeTokens(CommandTokenizer.tokenize(arguments), knownFlags);
    }

    /**
     * Reads arguments whose preamble is an index. An unquoted negative number could be an
     * index or an option, so reports both reasons it is rejected before reading the flags.
     *
     * @throws ParseException if the index looks like an invalid option or tokenization fails.
     */
    public static FlagArgumentMap tokenizeWithIndex(String arguments, CliFlag... knownFlags) throws ParseException {
        return tokenizeWithIndexTokens(CommandTokenizer.tokenize(arguments), knownFlags);
    }

    /** Reads already split tokens while retaining the shared invalid-index diagnostics. */
    static FlagArgumentMap tokenizeWithIndexTokens(List<Token> tokens, CliFlag... knownFlags) throws ParseException {
        if (!tokens.isEmpty()) {
            Token first = tokens.getFirst();
            if (!first.isQuoted() && NEGATIVE_INDEX_PATTERN.matcher(first.value()).matches()) {
                throw new ParseException(ParserUtil.MESSAGE_INVALID_INDEX + "\n"
                        + String.format(Messages.MESSAGE_UNKNOWN_FLAG, Messages.shorten(first.value())));
            }
        }
        return tokenizeTokens(tokens, knownFlags);
    }

    /** Reads already split tokens, sharing flag handling between index and name preambles. */
    static FlagArgumentMap tokenizeTokens(List<Token> tokens, CliFlag... knownFlags) throws ParseException {
        // a caller that names the same marker twice means one flag, not a broken collector
        Map<String, CliFlag> flagsByMarker = Stream.of(knownFlags)
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
     * Reads each flag from {@code firstFlag} onwards, together with its value when it takes one.
     */
    private static void readFlags(List<Token> tokens, int firstFlag, Map<String, CliFlag> flagsByMarker,
            FlagArgumentMap parsed) throws ParseException {
        int index = firstFlag;
        while (index < tokens.size()) {
            index = readFlag(tokens, index, flagsByMarker, parsed);
        }
    }

    /** Reads the flag at {@code index} and returns the index after its optional value. */
    private static int readFlag(List<Token> tokens, int index, Map<String, CliFlag> flagsByMarker,
            FlagArgumentMap parsed) throws ParseException {
        Token token = tokens.get(index);
        Optional<AttachedOption> attached = getAttachedOption(token);
        if (attached.isPresent()) {
            readAttachedFlag(token, attached.get(), flagsByMarker, parsed);
            return index + 1;
        }
        CliFlag flag = getFlag(token, flagsByMarker, parsed);
        if (!flag.isValueRequired()) {
            return readPresenceFlag(tokens, index, flag, parsed);
        }
        if (index + 1 >= tokens.size() || isOption(tokens.get(index + 1))) {
            throw new ParseException(String.format(
                    Messages.MESSAGE_FLAG_WITHOUT_VALUE, flag.getLabel(token.value())));
        }
        parsed.put(flag, token.value(), tokens.get(index + 1).value());
        return index + 2;
    }

    /**
     * Records the presence-only flag at {@code index} and returns the index after it. A value
     * written after such a flag belongs to no option at all, and naming the flag that takes
     * none points at the token to drop rather than at a place to move it to.
     */
    private static int readPresenceFlag(List<Token> tokens, int index, CliFlag flag,
            FlagArgumentMap parsed) throws ParseException {
        Token token = tokens.get(index);
        if (index + 1 < tokens.size() && !isOption(tokens.get(index + 1))) {
            throw new ParseException(String.format(Messages.MESSAGE_FLAG_TAKES_NO_VALUE, token.value()));
        }
        parsed.put(flag, token.value(), "");
        return index + 1;
    }

    /**
     * Reads an attached option while preserving the distinction between absent and quoted empty values.
     */
    private static void readAttachedFlag(Token token, AttachedOption attached, Map<String, CliFlag> flagsByMarker,
            FlagArgumentMap parsed) throws ParseException {
        CliFlag flag = getFlag(attached.marker(), flagsByMarker);
        if (!flag.isValueRequired()) {
            throw new ParseException(String.format(Messages.MESSAGE_FLAG_TAKES_NO_VALUE, attached.marker()));
        }
        if (attached.value().isEmpty() && !token.isQuoted()) {
            throw new ParseException(String.format(
                    Messages.MESSAGE_FLAG_WITHOUT_VALUE, flag.getLabel(attached.marker())));
        }
        parsed.put(flag, attached.marker(), attached.value());
    }

    /**
     * Returns the known flag named by {@code token}. A token naming none is reported as a value
     * belonging to no option, and the preamble read into {@code parsed} says whether the front
     * of the command is still free for it.
     */
    private static CliFlag getFlag(Token token, Map<String, CliFlag> flagsByMarker,
            FlagArgumentMap parsed) throws ParseException {
        if (!isOption(token)) {
            throw new ValueAfterFlagsException(Messages.getErrorMessageForValueAfterFlags(
                    token.value(), !parsed.getPreamble().isEmpty()));
        }

        return getFlag(token.value(), flagsByMarker);
    }

    /** Returns the known flag, suggesting a near spelling only if this command supports it. */
    private static CliFlag getFlag(String marker, Map<String, CliFlag> flagsByMarker) throws ParseException {
        CliFlag flag = flagsByMarker.get(marker);
        if (flag == null) {
            throw new UnknownFlagException(marker, getSuggestion(marker, flagsByMarker));
        }
        return flag;
    }

    /**
     * Returns the option of this command that {@code marker} is nearest to, or null when none of
     * its near spellings is one, so that a guess at another command's option is never offered.
     */
    private static String getSuggestion(String marker, Map<String, CliFlag> flagsByMarker) {
        return getNearSpellings(marker).stream()
                .filter(flagsByMarker::containsKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the spellings {@code marker} may stand for, nearest first. One dash before a word
     * is what an autocorrected {@code --} leaves behind, so the long option of that word counts
     * as a near spelling of it.
     */
    private static List<String> getNearSpellings(String marker) {
        String lowercase = marker.toLowerCase(Locale.ROOT);
        if (!isWordAfterOneDash(marker)) {
            return List.of(lowercase);
        }
        String longForm = LONG_OPTION_MARKER + marker.substring(OPTION_MARKER.length());
        return List.of(lowercase, longForm, longForm.toLowerCase(Locale.ROOT));
    }

    /** Returns true if {@code marker} is a single dash before more than one character. */
    private static boolean isWordAfterOneDash(String marker) {
        return marker.startsWith(OPTION_MARKER)
                && !marker.startsWith(LONG_OPTION_MARKER)
                && marker.length() > OPTION_MARKER.length() + 1;
    }

    /**
     * Returns true if {@code token} marks a field rather than being a value. A quoted token is
     * always a value, which is how a value opening with the flag marker is given.
     */
    private static boolean isFlag(Token token) {
        return !token.isQuoted()
                && token.value().startsWith(OPTION_MARKER)
                && token.value().length() > OPTION_MARKER.length();
    }

    /** Returns true if {@code token} looks like either supported option form. */
    private static boolean isOption(Token token) {
        return isFlag(token) || isAttachedOption(token);
    }

    /** Returns true if {@code token} has an unquoted option marker before an equals sign. */
    private static boolean isAttachedOption(Token token) {
        int separator = token.value().indexOf(VALUE_SEPARATOR);
        return !token.beginsQuoted()
                && token.value().startsWith(OPTION_MARKER)
                && separator > OPTION_MARKER.length();
    }

    /** Returns the option marker and value joined in {@code token}, if any. */
    private static Optional<AttachedOption> getAttachedOption(Token token) {
        if (!isAttachedOption(token)) {
            return Optional.empty();
        }

        int separator = token.value().indexOf(VALUE_SEPARATOR);
        String marker = token.value().substring(0, separator);
        return Optional.of(new AttachedOption(marker, token.value().substring(separator + 1)));
    }
}
