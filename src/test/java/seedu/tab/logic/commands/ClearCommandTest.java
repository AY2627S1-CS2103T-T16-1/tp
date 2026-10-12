package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import org.junit.jupiter.api.Test;

import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;

public class ClearCommandTest {

    @Test
    public void execute_emptyStudentBook_success() {
        Model model = new ModelManager();
        Model expectedModel = new ModelManager();

        assertCommandSuccess(new ClearCommand(), model, ClearCommand.MESSAGE_EMPTY, expectedModel);
    }

    @Test
    public void execute_nonEmptyStudentBook_requestsConfirmation() {
        Model model = new ModelManager(getTypicalStudentBook(), new UserPrefs());
        Model expectedModel = new ModelManager(getTypicalStudentBook(), new UserPrefs());
        model.updateFilteredStudentList(student -> false);
        expectedModel.updateFilteredStudentList(student -> false);
        String prompt = String.format(ClearCommand.MESSAGE_CONFIRMATION,
                model.getStudentBook().getStudentList().size());
        assertCommandSuccess(new ClearCommand(), model, prompt, expectedModel);
    }

    @Test
    public void executeConfirmed_nonEmptyStudentBook_clearsAllRecords() {
        Model model = new ModelManager(getTypicalStudentBook(), new UserPrefs());
        model.updateFilteredStudentList(student -> false);
        assertEquals(new CommandResult(ClearCommand.MESSAGE_SUCCESS), new ClearCommand().executeConfirmed(model));
        assertEquals(new StudentBook(), model.getStudentBook());
    }

}
