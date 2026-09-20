package seedu.tab.logic.parser;

import java.util.ArrayList;
import java.util.List;

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
