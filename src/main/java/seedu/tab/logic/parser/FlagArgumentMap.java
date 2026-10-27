package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import seedu.tab.logic.Messages;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Stores the value given for each flag of a command, together with the preamble: the value
 * given before the first flag, which each command names for itself. A flag may be repeated,
 * and the order its values were given in is kept.
 */
public class FlagArgumentMap {

    private final Map<CliFlag, List<FlagValue>> values = new HashMap<>();
    private String preamble = "";

    private record FlagValue(String marker, String value) {}

    /**
     * Associates {@code value} with the spelling of {@code flag} that introduced it, after
     * any value already given for that flag.
     */
    public void put(CliFlag flag, String marker, String value) {
        requireNonNull(flag);
        requireNonNull(marker);
        requireNonNull(value);
        values.computeIfAbsent(flag, key -> new ArrayList<>()).add(new FlagValue(marker, value));
    }

    public void setPreamble(String preamble) {
        this.preamble = requireNonNull(preamble);
    }

    /**
     * Returns the value given before the first flag, which is empty when none was given.
     */
    public String getPreamble() {
        return preamble;
    }

    /**
     * Returns the last value given for {@code flag}.
     */
    public Optional<String> getValue(CliFlag flag) {
        List<String> flagValues = getAllValues(flag);
        return flagValues.isEmpty() ? Optional.empty() : Optional.of(flagValues.getLast());
    }

    /**
     * Returns every value given for {@code flag}, or an empty list when it was not given.
     * The returned list is the caller's own.
     */
    public List<String> getAllValues(CliFlag flag) {
        if (!values.containsKey(flag)) {
            return new ArrayList<>();
        }
        List<String> flagValues = new ArrayList<>();
        values.get(flag).forEach(flagValue -> flagValues.add(flagValue.value()));
        return flagValues;
    }

    /**
     * Throws a {@code ParseException} if any flag in {@code flags} was given more than once.
     */
    public void verifyNoDuplicateFlagsFor(CliFlag... flags) throws ParseException {
        String[] duplicatedFlags = Stream.of(flags).distinct()
                .filter(flag -> getAllValues(flag).size() > 1)
                .map(flag -> values.get(flag).getFirst().marker())
                .toArray(String[]::new);

        if (duplicatedFlags.length > 0) {
            throw new ParseException(Messages.getErrorMessageForDuplicateFlags(duplicatedFlags));
        }
    }
}
