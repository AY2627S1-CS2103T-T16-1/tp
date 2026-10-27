package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.tab.testutil.StudentBuilder;

public class CommandResultTest {
    @Test
    public void equals() {
        CommandResult commandResult = new CommandResult("feedback");

        // same values -> returns true
        assertTrue(commandResult.equals(new CommandResult("feedback")));
        assertTrue(commandResult.equals(new CommandResult("feedback", false, false)));

        // same object -> returns true
        assertTrue(commandResult.equals(commandResult));

        // null -> returns false
        assertFalse(commandResult.equals(null));

        // different types -> returns false
        assertFalse(commandResult.equals(0.5f));

        // different feedbackToUser value -> returns false
        assertFalse(commandResult.equals(new CommandResult("different")));

        // different showHelp value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", true, false)));

        // different exit value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", false, true)));
    }

    @Test
    public void hashCode_equivalentResults_returnsSameHashCode() {
        CommandResult commandResult = new CommandResult("feedback");

        assertEquals(commandResult.hashCode(), new CommandResult("feedback").hashCode());
        assertEquals(commandResult.hashCode(), new CommandResult("feedback", false, false).hashCode());
        assertEquals(new CommandResult("feedback", true, false).hashCode(),
                new CommandResult("feedback", true, false).hashCode());
        assertEquals(new CommandResult("feedback", false, true).hashCode(),
                new CommandResult("feedback", false, true).hashCode());
    }

    @Test
    public void studentToReveal_presentOnlyForTargetedResults() {
        var student = new StudentBuilder().build();
        CommandResult result = new CommandResult("feedback", student);
        CommandResult copy = new CommandResult("feedback", new StudentBuilder().build());

        assertEquals(student, result.getStudentToReveal().orElseThrow());
        assertEquals(result, copy);
        assertEquals(result.hashCode(), copy.hashCode());
        assertFalse(result.equals(new CommandResult("feedback")));
        assertFalse(result.equals(new CommandResult("feedback", new StudentBuilder().withName("Other").build())));
        assertTrue(new CommandResult("feedback").getStudentToReveal().isEmpty());
    }

    @Test
    public void toStringMethod() {
        CommandResult commandResult = new CommandResult("feedback");
        String expected = CommandResult.class.getCanonicalName() + "{feedbackToUser="
                + commandResult.getFeedbackToUser() + ", isHelpRequested=" + commandResult.isHelpRequested()
                + ", isExitRequested=" + commandResult.isExitRequested() + ", studentToReveal=null}";
        assertEquals(expected, commandResult.toString());
    }
}
