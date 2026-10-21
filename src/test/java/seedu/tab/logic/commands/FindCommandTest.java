package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.testutil.TypicalStudents.BENSON;
import static seedu.tab.testutil.TypicalStudents.CARL;
import static seedu.tab.testutil.TypicalStudents.DANIEL;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate.Field;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code FindCommand}.
 */
public class FindCommandTest {
    private Model model = new ModelManager(getTypicalStudentBook(), new UserPrefs());
    private Model expectedModel = new ModelManager(getTypicalStudentBook(), new UserPrefs());

    @Test
    public void equals() {
        DetailsContainsKeywordsPredicate firstPredicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("first")));
        DetailsContainsKeywordsPredicate secondPredicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("second")));

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
        DetailsContainsKeywordsPredicate predicate = preparePredicate(Field.NAME, " ");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(), model.getFilteredStudentList());
    }

    @Test
    public void execute_multipleKeywords_requiresEveryKeyword() {
        String expectedMessage = Messages.getStudentsFoundOverview(2);
        DetailsContainsKeywordsPredicate predicate = new DetailsContainsKeywordsPredicate(Map.of(
                Field.NAME, List.of("Meier"), Field.TAG, List.of("friends")));
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(BENSON, DANIEL), model.getFilteredStudentList());
    }

    @Test
    public void execute_phoneFragment_studentFound() {
        String expectedMessage = Messages.getStudentsFoundOverview(1);
        DetailsContainsKeywordsPredicate predicate = preparePredicate(Field.PHONE, "52533");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(DANIEL), model.getFilteredStudentList());
    }

    @Test
    public void execute_repeatedSearches_updatesCount() {
        // First search matches 1 student
        DetailsContainsKeywordsPredicate firstPredicate = preparePredicate(Field.PHONE, "52533");
        FindCommand firstCommand = new FindCommand(firstPredicate);
        expectedModel.updateFilteredStudentList(firstPredicate);
        assertCommandSuccess(firstCommand, model, Messages.getStudentsFoundOverview(1), expectedModel);
        assertEquals(List.of(DANIEL), model.getFilteredStudentList());

        // Second search with different keywords matches 2 students
        DetailsContainsKeywordsPredicate secondPredicate = new DetailsContainsKeywordsPredicate(Map.of(
                Field.NAME, List.of("Meier"), Field.TAG, List.of("friends")));
        FindCommand secondCommand = new FindCommand(secondPredicate);
        expectedModel.updateFilteredStudentList(secondPredicate);
        assertCommandSuccess(secondCommand, model, Messages.getStudentsFoundOverview(2), expectedModel);
        assertEquals(List.of(BENSON, DANIEL), model.getFilteredStudentList());

        // Third search with no matches reports 0 students
        DetailsContainsKeywordsPredicate thirdPredicate = preparePredicate(Field.NAME, "nonexistent");
        FindCommand thirdCommand = new FindCommand(thirdPredicate);
        expectedModel.updateFilteredStudentList(thirdPredicate);
        assertCommandSuccess(thirdCommand, model, Messages.getStudentsFoundOverview(0), expectedModel);
        assertEquals(List.of(), model.getFilteredStudentList());
    }

    @Test
    public void execute_nusStudentIdFragmentIgnoringCase_studentFound() {
        String expectedMessage = Messages.getStudentsFoundOverview(1);
        DetailsContainsKeywordsPredicate predicate = preparePredicate(Field.NUS_STUDENT_ID, "a2345");
        FindCommand command = new FindCommand(predicate);
        expectedModel.updateFilteredStudentList(predicate);
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
        assertEquals(List.of(CARL), model.getFilteredStudentList());
    }

    @Test
    public void execute_quotedPhrase_matchesWholePhraseAndExcludesSeparateFragments() {
        Student phraseStudent = new StudentBuilder().withName("Alice Tan")
                .withPhone("91111111").withTags("Lab 3").build();
        Student fragmentStudent = new StudentBuilder().withName("Bob Lee")
                .withPhone("93333333").withTags("Lab").build();
        StudentBook studentBook = new StudentBook();
        studentBook.addStudent(phraseStudent);
        studentBook.addStudent(fragmentStudent);

        Model localModel = new ModelManager(studentBook, new UserPrefs());
        Model localExpectedModel = new ModelManager(studentBook, new UserPrefs());

        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.TAG, List.of("Lab 3")));
        FindCommand command = new FindCommand(predicate);
        localExpectedModel.updateFilteredStudentList(predicate);

        assertCommandSuccess(command, localModel,
                Messages.getStudentsFoundOverview(1), localExpectedModel);
        assertEquals(List.of(phraseStudent), localModel.getFilteredStudentList());
    }

    @Test
    public void execute_followUpLabelFragment_findsOnlyStudentWithThatName() {
        Student surnameStudent = new StudentBuilder().withName("Low Jia Hui").withPhone("91112222").build();
        Student firstFlaggedStudent = new StudentBuilder().withName("Alex Yeoh").withPhone("87438807")
                .withFlag(true).build();
        Student secondFlaggedStudent = new StudentBuilder().withName("Bernice Yu").withPhone("99272758")
                .withFlag(true).build();
        StudentBook studentBook = new StudentBook();
        studentBook.addStudent(surnameStudent);
        studentBook.addStudent(firstFlaggedStudent);
        studentBook.addStudent(secondFlaggedStudent);

        Model localModel = new ModelManager(studentBook, new UserPrefs());
        Model localExpectedModel = new ModelManager(studentBook, new UserPrefs());

        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("low")));
        FindCommand command = new FindCommand(predicate);
        localExpectedModel.updateFilteredStudentList(predicate);

        assertCommandSuccess(command, localModel,
                Messages.getStudentsFoundOverview(1), localExpectedModel);
        assertEquals(List.of(surnameStudent), localModel.getFilteredStudentList());
    }

    @Test
    public void execute_mixedPhraseAndKeyword_requiresBoth() {
        Student phraseStudent = new StudentBuilder().withName("Alice Tan")
                .withPhone("91111111").withTags("Lab 3").build();
        Student keywordStudent = new StudentBuilder().withName("Bob Lee")
                .withPhone("93333333").withTags("Tutorial").build();
        Student nonMatchingStudent = new StudentBuilder().withName("Charlie Lim")
                .withPhone("94444444").withTags("Lecture").build();
        StudentBook studentBook = new StudentBook();
        studentBook.addStudent(phraseStudent);
        studentBook.addStudent(keywordStudent);
        studentBook.addStudent(nonMatchingStudent);

        Model localModel = new ModelManager(studentBook, new UserPrefs());
        Model localExpectedModel = new ModelManager(studentBook, new UserPrefs());

        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.TAG, List.of("Lab 3"), Field.NAME, List.of("Alice")));
        FindCommand command = new FindCommand(predicate);
        localExpectedModel.updateFilteredStudentList(predicate);

        assertCommandSuccess(command, localModel,
                Messages.getStudentsFoundOverview(1), localExpectedModel);
        assertEquals(List.of(phraseStudent), localModel.getFilteredStudentList());
    }

    @Test
    public void toStringMethod() {
        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("keyword")));
        FindCommand findCommand = new FindCommand(predicate);
        String expected = FindCommand.class.getCanonicalName() + "{predicate=" + predicate + "}";
        assertEquals(expected, findCommand.toString());
    }

    /**
     * Parses {@code userInput} into a {@code DetailsContainsKeywordsPredicate}.
     */
    private DetailsContainsKeywordsPredicate preparePredicate(Field field, String userInput) {
        return new DetailsContainsKeywordsPredicate(Map.of(field, List.of(userInput.split("\\s+"))));
    }
}
