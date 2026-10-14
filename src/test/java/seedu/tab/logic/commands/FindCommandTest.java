package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.Messages.MESSAGE_STUDENTS_FOUND_OVERVIEW;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.testutil.TypicalStudents.CARL;
import static seedu.tab.testutil.TypicalStudents.DANIEL;
import static seedu.tab.testutil.TypicalStudents.ELLE;
import static seedu.tab.testutil.TypicalStudents.FIONA;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;

/**
 * Contains integration tests (interaction with the Model) for {@code FindCommand}.
 */
public class FindCommandTest {
    private Model model = new ModelManager(getTypicalStudentBook(), new UserPrefs());
    private Model expectedModel = new ModelManager(getTypicalStudentBook(), new UserPrefs());

    @Test
    public void equals() {
        DetailsContainsKeywordsPredicate firstPredicate =
                new DetailsContainsKeywordsPredicate(List.of("first"));
        DetailsContainsKeywordsPredicate secondPredicate =
                new DetailsContainsKeywordsPredicate(List.of("second"));

        FindCommand findFirstCommand = new FindCommand(firstPredicate);
        FindCommand findSecondCommand = new FindCommand(secondPredicate);

        // same object -> returns true
        assertTrue(findFirstCommand.equals(findFirstCommand));

        // same values -> returns true
        FindCommand findFirstCommandCopy = new FindCommand(firstPredicate);
        assertTrue(findFirstCommand.equals(findFirstCommandCopy));

        // different types -> returns false
        assertFalse(findFirstCommand.equals(1));

        // null -> returns false
        assertFalse(findFirstCommand.equals(null));

        // different student -> returns false
        assertFalse(findFirstCommand.equals(findSecondCommand));
    }

    @Test
    public void execute_zeroKeywords_noStudentFound() {
        String expectedMessage = Messages.getStudentsFoundOverview(0);
        DetailsContainsKeywordsPredicate predicate = preparePredicate(" ");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredStudentList());
    }

    @Test
    public void execute_multipleKeywords_multipleStudentsFound() {
        String expectedMessage = Messages.getStudentsFoundOverview(3);
        DetailsContainsKeywordsPredicate predicate = preparePredicate("Kurz Elle Kunz");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL, ELLE, FIONA), model.getFilteredStudentList());
    }

    @Test
    public void execute_phoneFragment_studentFound() {
        String expectedMessage = Messages.getStudentsFoundOverview(1);
        DetailsContainsKeywordsPredicate predicate = preparePredicate("52533");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(DANIEL), model.getFilteredStudentList());
    }

    @Test
    public void execute_repeatedSearches_updatesCount() {
        // First search matches 1 student
        DetailsContainsKeywordsPredicate firstPredicate = preparePredicate("52533");
        FindCommand firstCommand = new FindCommand(firstPredicate);
        expectedModel.updateFilteredStudentList(firstPredicate);
        assertCommandSuccess(firstCommand, model, Messages.getStudentsFoundOverview(1), expectedModel);
        assertEquals(List.of(DANIEL), model.getFilteredStudentList());

        // Second search with different keywords matches 3 students
        DetailsContainsKeywordsPredicate secondPredicate = preparePredicate("Kurz Elle Kunz");
        FindCommand secondCommand = new FindCommand(secondPredicate);
        expectedModel.updateFilteredStudentList(secondPredicate);
        assertCommandSuccess(secondCommand, model, Messages.getStudentsFoundOverview(3), expectedModel);
        assertEquals(List.of(CARL, ELLE, FIONA), model.getFilteredStudentList());

        // Third search with no matches reports 0 students
        DetailsContainsKeywordsPredicate thirdPredicate = preparePredicate("nonexistent");
        FindCommand thirdCommand = new FindCommand(thirdPredicate);
        expectedModel.updateFilteredStudentList(thirdPredicate);
        assertCommandSuccess(thirdCommand, model, Messages.getStudentsFoundOverview(0), expectedModel);
        assertEquals(List.of(), model.getFilteredStudentList());
    }

    @Test
    public void getStudentsFoundOverview() {
        assertEquals("Found 0 students.", Messages.getStudentsFoundOverview(0));
        assertEquals("Found 1 student.", Messages.getStudentsFoundOverview(1));
        assertEquals("Found 3 students.", Messages.getStudentsFoundOverview(3));
        assertEquals(String.format(MESSAGE_STUDENTS_FOUND_OVERVIEW, 0, "s"),
                Messages.getStudentsFoundOverview(0));
    }
    @Test
    public void execute_nusStudentIdFragmentIgnoringCase_studentFound() {
        String expectedMessage = String.format(MESSAGE_STUDENTS_LISTED_OVERVIEW, 1);
        DetailsContainsKeywordsPredicate predicate = preparePredicate("a2345");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL), model.getFilteredStudentList());
    }

    @Test
    public void toStringMethod() {
        DetailsContainsKeywordsPredicate predicate = new DetailsContainsKeywordsPredicate(List.of("keyword"));
        FindCommand findCommand = new FindCommand(predicate);
        String expected = FindCommand.class.getCanonicalName() + "{predicate=" + predicate + "}";
        assertEquals(expected, findCommand.toString());
    }

    /**
     * Parses {@code userInput} into a {@code DetailsContainsKeywordsPredicate}.
     */
    private DetailsContainsKeywordsPredicate preparePredicate(String userInput) {
        return new DetailsContainsKeywordsPredicate(List.of(userInput.split("\\s+")));
    }
}
