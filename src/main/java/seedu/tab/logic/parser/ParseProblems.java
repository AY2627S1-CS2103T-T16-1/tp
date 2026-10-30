package seedu.tab.logic.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Collects everything wrong with one command, so that a user who mistyped two fields is told
 * about both at once rather than discovering the second after correcting the first.
 *
 * <p>A parser records each problem as it goes, then calls {@link #throwIfAny()} once. A step that
 * fails yields {@code null}, which is safe because nothing reads the results until every step
 * has run and {@link #throwIfAny()} has decided whether to stop.
 */
public class ParseProblems {

    private final List<String> problems = new ArrayList<>();

    /**
     * Records {@code problem} as one of the things wrong with this command.
     */
    public void add(String problem) {
        problems.add(problem);
    }

    /**
     * Runs {@code step}, returning its result. If the step rejects its input, records why and
     * returns null.
     */
    public <T> T collect(Step<T> step) {
        try {
            return step.run();
        } catch (ParseException e) {
            problems.add(e.getMessage());
            return null;
        }
    }

    /**
     * Parses {@code value} if it was given, recording why if it is rejected, and returns what
     * the field holds. A field the command left out yields nothing without a complaint, because
     * an optional field is entitled to be absent and a required one is reported separately. A
     * field whose value is rejected yields nothing as well, its reason having been recorded.
     */
    public <T> Optional<T> collectIfPresent(Optional<String> value, ValueParser<T> parser) {
        return value.map(given -> collect(() -> parser.parse(given)));
    }

    /**
     * Parses every value supplied for one repeatable field, recording why for each one that is
     * rejected, and returns the ones that were accepted in input order.
     */
    public <T> Set<T> collectEach(Collection<String> values, ValueParser<T> parser) {
        Set<T> parsed = new LinkedHashSet<>();
        for (String value : values) {
            T parsedValue = collect(() -> parser.parse(value));
            if (parsedValue != null) {
                parsed.add(parsedValue);
            }
        }
        return parsed;
    }

    /**
     * Throws a single exception listing every problem recorded, one to a line, or returns
     * quietly when there were none.
     *
     * @throws ParseException if anything was recorded.
     */
    public void throwIfAny() throws ParseException {
        if (!problems.isEmpty()) {
            throw new ParseException(String.join("\n", problems));
        }
    }

    /**
     * One step of parsing that may reject its input.
     *
     * @param <T> what the step produces when the input is accepted.
     */
    @FunctionalInterface
    public interface Step<T> {
        /**
         * Returns the parsed result, or throws when the input is rejected.
         */
        T run() throws ParseException;
    }

    /**
     * Turns the text a user supplied for one field into a value, or rejects it.
     *
     * @param <T> the field this parses into.
     */
    @FunctionalInterface
    public interface ValueParser<T> {
        /**
         * Parses the field value, or throws when it is rejected.
         */
        T parse(String value) throws ParseException;
    }
}
