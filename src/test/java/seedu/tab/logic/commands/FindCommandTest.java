package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.Messages.MESSAGE_STUDENTS_LISTED_OVERVIEW;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.testutil.TypicalStudents.CARL;
import static seedu.tab.testutil.TypicalStudents.DANIEL;
import static seedu.tab.testutil.TypicalStudents.ELLE;
import static seedu.tab.testutil.TypicalStudents.FIONA;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.util.List;

import org.junit.jupiter.api.Test;

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
        String expectedMessage = String.format(MESSAGE_STUDENTS_LISTED_OVERVIEW, 0);
        DetailsContainsKeywordsPredicate predicate = preparePredicate(" ");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredStudentList());
    }

    @Test
    public void execute_multipleKeywords_multipleStudentsFound() {
        String expectedMessage = String.format(MESSAGE_STUDENTS_LISTED_OVERVIEW, 3);
        DetailsContainsKeywordsPredicate predicate = preparePredicate("Kurz Elle Kunz");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL, ELLE, FIONA), model.getFilteredStudentList());
    }

    @Test
    public void execute_phoneFragment_studentFound() {
        String expectedMessage = String.format(MESSAGE_STUDENTS_LISTED_OVERVIEW, 1);
        DetailsContainsKeywordsPredicate predicate = preparePredicate("52533");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(DANIEL), model.getFilteredStudentList());
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
