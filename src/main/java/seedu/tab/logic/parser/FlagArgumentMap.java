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
 * Stores the value given for each flag of a command, together with the name given before the
 * first flag. A flag may be repeated, and the order its values were given in is kept.
 */
public class FlagArgumentMap {

    private final Map<Flag, List<String>> values = new HashMap<>();
    private String name = "";

    /**
     * Associates {@code value} with {@code flag}, after any value already given for it.
     */
    public void put(Flag flag, String value) {
        requireNonNull(flag);
        requireNonNull(value);
        List<String> flagValues = getAllValues(flag);
        flagValues.add(value);
        values.put(flag, flagValues);
    }

    public void setName(String name) {
        this.name = requireNonNull(name);
    }

    /**
     * Returns the name given before the first flag, which is empty when none was given.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the last value given for {@code flag}.
     */
    public Optional<String> getValue(Flag flag) {
        List<String> flagValues = getAllValues(flag);
        return flagValues.isEmpty() ? Optional.empty() : Optional.of(flagValues.getLast());
    }

    /**
     * Returns every value given for {@code flag}, or an empty list when it was not given.
     * The returned list is the caller's own.
     */
    public List<String> getAllValues(Flag flag) {
        if (!values.containsKey(flag)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values.get(flag));
    }

    /**
     * Throws a {@code ParseException} if any flag in {@code flags} was given more than once.
     */
    public void verifyNoDuplicateFlagsFor(Flag... flags) throws ParseException {
        Flag[] duplicated = Stream.of(flags).distinct()
                .filter(flag -> getAllValues(flag).size() > 1)
                .toArray(Flag[]::new);

        if (duplicated.length > 0) {
            throw new ParseException(Messages.getErrorMessageForDuplicateFlags(duplicated));
        }
    }
}
