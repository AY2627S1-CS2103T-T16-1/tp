package seedu.tab.logic.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Collects everything wrong with one command, so that a user who mistyped two fields is told
 * about both at once rather than discovering the second after correcting the first.
 *
 * A parser records each problem as it goes, then calls {@link #throwIfAny()} once. A step that
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
     * Parses {@code value} if it was given, recording why if it is rejected. A field the command
     * left out yields null without a complaint, because an optional field is entitled to be
     * absent and a required one is reported separately.
     */
    public <T> T collectIfPresent(Optional<String> value, ValueParser<T> parser) {
        if (value.isEmpty()) {
            return null;
        }
        return collect(() -> parser.parse(value.get()));
    }

    /**
     * Parses every value supplied for one repeatable field, recording why for each one that is
     * rejected, and returns the ones that were accepted.
     */
    public <T> Set<T> collectEach(Collection<String> values, ValueParser<T> parser) {
        Set<T> parsed = new HashSet<>();
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
        T run() throws ParseException;
    }

    /**
     * Turns the text a user supplied for one field into a value, or rejects it.
     *
     * @param <T> the field this parses into.
     */
    @FunctionalInterface
    public interface ValueParser<T> {
        T parse(String value) throws ParseException;
    }
}
