package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    public void throwIfAny_keepsTheOrderProblemsWereRecordedIn() {
        ParseProblems problems = new ParseProblems();
        problems.add("b");
        problems.add("a");

        ParseException thrown = assertThrows(ParseException.class, problems::throwIfAny);
        assertEquals("b\na", thrown.getMessage());
    }
}
