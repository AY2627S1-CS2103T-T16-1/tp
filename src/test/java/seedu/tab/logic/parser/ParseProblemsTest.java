package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.parser.exceptions.ParseException;

public class ParseProblemsTest {

    @Test
    public void throwIfAny_nothingRecorded_doesNotThrow() throws ParseException {
        new ParseProblems().throwIfAny();
    }

    @Test
    public void collect_stepSucceeds_returnsItsResultAndRecordsNothing() throws ParseException {
        ParseProblems problems = new ParseProblems();
        assertEquals("ok", problems.collect(() -> "ok"));
        problems.throwIfAny();
    }

    @Test
    public void collect_stepFails_returnsNullAndCarriesOn() {
        ParseProblems problems = new ParseProblems();

        assertNull(problems.collect(() -> {
            throw new ParseException("first");
        }));
        // a failed step must not stop the ones after it, or only the first fault is ever found
        assertEquals("second", problems.collect(() -> "second"));

        ParseException thrown = assertThrows(ParseException.class, problems::throwIfAny);
        assertEquals("first", thrown.getMessage());
    }

    @Test
    public void throwIfAny_severalProblems_listsThemOneToALine() {
        ParseProblems problems = new ParseProblems();
        problems.add("first");
        problems.collect(() -> {
            throw new ParseException("second");
        });
        problems.add("third");

        ParseException thrown = assertThrows(ParseException.class, problems::throwIfAny);
        assertEquals("first\nsecond\nthird", thrown.getMessage());
    }

    @Test
    public void collectIfPresent_valueAbsent_returnsNullAndRecordsNothing() throws ParseException {
        ParseProblems problems = new ParseProblems();

        assertNull(problems.collectIfPresent(Optional.empty(), value -> value));
        // an optional field is entitled to be absent, and a required one is reported elsewhere
        problems.throwIfAny();
    }

    @Test
    public void collectIfPresent_valueAccepted_returnsIt() throws ParseException {
        ParseProblems problems = new ParseProblems();

        assertEquals("AMY", problems.collectIfPresent(Optional.of("amy"), String::toUpperCase));
        problems.throwIfAny();
    }

    @Test
    public void collectIfPresent_valueRejected_returnsNullAndRecordsWhy() {
        ParseProblems problems = new ParseProblems();

        assertNull(problems.collectIfPresent(Optional.of("12"), value -> {
            throw new ParseException("too short");
        }));

        ParseException thrown = assertThrows(ParseException.class, problems::throwIfAny);
        assertEquals("too short", thrown.getMessage());
    }

    @Test
    public void collectEach_noValues_returnsAnEmptySet() throws ParseException {
        ParseProblems problems = new ParseProblems();

        assertEquals(Set.of(), problems.collectEach(List.of(), value -> value));
        problems.throwIfAny();
    }

    @Test
    public void collectEach_everyValueAccepted_returnsThemAll() throws ParseException {
        ParseProblems problems = new ParseProblems();

        assertEquals(Set.of("T1", "LAB 3"),
                problems.collectEach(List.of("t1", "Lab 3"), String::toUpperCase));
        problems.throwIfAny();
    }

    @Test
    public void collectEach_someValuesRejected_keepsTheRestAndRecordsEachReason() {
        ParseProblems problems = new ParseProblems();

        // every value is examined, so a user who mistyped two of them hears about both
        Set<String> accepted = problems.collectEach(List.of("---", "T1", "***"), value -> {
            if (value.equals("T1")) {
                return value;
            }
            throw new ParseException(value + " has no letter or number");
        });

        assertEquals(Set.of("T1"), accepted);
        ParseException thrown = assertThrows(ParseException.class, problems::throwIfAny);
        assertEquals("--- has no letter or number\n*** has no letter or number", thrown.getMessage());
    }

    @Test
    public void throwIfAny_keepsTheOrderProblemsWereRecordedIn() {
        ParseProblems problems = new ParseProblems();
        problems.add("b");
        problems.add("a");

        ParseException thrown = assertThrows(ParseException.class, problems::throwIfAny);
        assertEquals("b\na", thrown.getMessage());
    }
}
