package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.logic.commands.CommandTestUtil.showStudentAtIndex;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static seedu.tab.testutil.TypicalStudents.ALICE;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;

/**
 * Contains integration tests (interaction with the Model) and unit tests for ListCommand.
 */
public class ListCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalStudentBook(), new UserPrefs());
        expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
    }

    @Test
    public void execute_listIsNotFiltered_showsSameList() {
        String expectedMessage = ListCommand.formatSuccessMessage(
                model.getFilteredStudentList().size());
        assertCommandSuccess(new ListCommand(), model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_listIsFiltered_showsEverything() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);
        String expectedMessage = ListCommand.formatSuccessMessage(
                expectedModel.getFilteredStudentList().size());
        assertCommandSuccess(new ListCommand(), model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_singleStudent_showsSingleStudentMessage() {
        StudentBook studentBook = new StudentBook();
        studentBook.addStudent(ALICE);
        model = new ModelManager(studentBook, new UserPrefs());
        expectedModel = new ModelManager(studentBook, new UserPrefs());
        assertCommandSuccess(new ListCommand(), model, ListCommand.formatSuccessMessage(1), expectedModel);
    }

    @Test
    public void execute_emptyStudentBook_showsEmptyListMessage() {
        model = new ModelManager();
        expectedModel = new ModelManager();
        assertCommandSuccess(new ListCommand(), model, ListCommand.formatSuccessMessage(0), expectedModel);
    }

    @Test
    public void formatSuccessMessage() {
        assertEquals("Listed 0 students.", ListCommand.formatSuccessMessage(0));
        assertEquals("Listed 1 student.", ListCommand.formatSuccessMessage(1));
        assertEquals("Listed 24 students.", ListCommand.formatSuccessMessage(24));
    }
}
