package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.logic.commands.ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import org.junit.jupiter.api.Test;

import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.UserPrefs;

public class ExitCommandTest {
    private Model model = new ModelManager();
    private Model expectedModel = new ModelManager();

    @Test
    public void execute_exit_success() {
        CommandResult expectedCommandResult = new CommandResult(MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true);
        assertCommandSuccess(new ExitCommand(), model, expectedCommandResult, expectedModel);
    }

    @Test
    public void execute_exit_commandResultProperties() {
        CommandResult result = new ExitCommand().execute(model);
        assertEquals(MESSAGE_EXIT_ACKNOWLEDGEMENT, result.getFeedbackToUser());
        assertTrue(result.isExit());
        assertFalse(result.isShowHelp());
    }

    @Test
    public void execute_exit_doesNotMutateModel() {
        Model initialModel = new ModelManager(getTypicalStudentBook(), new UserPrefs());
        Model unmutatedModel = new ModelManager(getTypicalStudentBook(), new UserPrefs());
        new ExitCommand().execute(initialModel);
        assertEquals(unmutatedModel, initialModel);
    }
}
