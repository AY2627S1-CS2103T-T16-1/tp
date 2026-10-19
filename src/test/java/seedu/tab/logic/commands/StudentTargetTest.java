package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static seedu.tab.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;

import org.junit.jupiter.api.Test;

import seedu.tab.model.student.NusStudentId;

/**
 * Contains unit tests for {@code StudentTarget}.
 */
public class StudentTargetTest {

    private static final StudentTarget FIRST_BY_INDEX = StudentTarget.byIndex(INDEX_FIRST_STUDENT);
    private static final StudentTarget FIRST_BY_ID =
            StudentTarget.byNusStudentId(new NusStudentId("A12345678C"));

    @Test
    public void equals() {
        // same object -> returns true
        assertTrue(FIRST_BY_INDEX.equals(FIRST_BY_INDEX));

        // same index -> returns true
        assertTrue(FIRST_BY_INDEX.equals(StudentTarget.byIndex(INDEX_FIRST_STUDENT)));

        // same NUS Student ID, either spelling -> returns true, because NusStudentId
        // normalizes its value
        assertTrue(FIRST_BY_ID.equals(
                StudentTarget.byNusStudentId(new NusStudentId("a12345678c"))));

        // different index -> returns false
        assertFalse(FIRST_BY_INDEX.equals(StudentTarget.byIndex(INDEX_SECOND_STUDENT)));

        // a different way of naming a student -> returns false
        assertFalse(FIRST_BY_INDEX.equals(FIRST_BY_ID));

        // null -> returns false
        assertFalse(FIRST_BY_INDEX.equals(null));

        // different type -> returns false
        assertFalse(FIRST_BY_INDEX.equals(1));
    }

    @Test
    public void toStringMethod() {
        // the string form holds only the field the target holds
        assertEquals(StudentTarget.class.getCanonicalName() + "{index=" + INDEX_FIRST_STUDENT + "}",
                FIRST_BY_INDEX.toString());
        assertEquals(StudentTarget.class.getCanonicalName()
                        + "{nusStudentId=" + new NusStudentId("A12345678C") + "}",
                FIRST_BY_ID.toString());
    }

    @Test
    public void describe_namesHowTheTargetWasGiven() {
        assertEquals("shown at 1", FIRST_BY_INDEX.describe());
        assertEquals("identified by NUS Student ID", FIRST_BY_ID.describe());
    }
}
