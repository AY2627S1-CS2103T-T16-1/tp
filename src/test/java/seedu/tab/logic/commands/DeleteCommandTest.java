package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.logic.commands.CommandTestUtil.showStudentAtIndex;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static seedu.tab.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;
import static seedu.tab.testutil.TypicalStudents.ALICE;
import static seedu.tab.testutil.TypicalStudents.CARL;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for
 * {@code DeleteCommand}.
 */
public class DeleteCommandTest {

    private Model model = new ModelManager(getTypicalStudentBook(), new UserPrefs());

    @Test
    public void execute_validIndexUnfilteredList_success() {
        Student studentToDelete = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_STUDENT);

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS,
                Messages.format(studentToDelete));

        ModelManager expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.deleteStudent(studentToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_studentWithoutEmailOrTags_feedbackIdentifiesStudentWithoutEmptyFields() {
        Student studentToDelete = new StudentBuilder().withName("Clean Student").withPhone("91234567")
                .withoutEmail().withTags().build();
        model.addStudent(studentToDelete);
        Index targetIndex = Index.fromOneBased(model.getFilteredStudentList().size());
        DeleteCommand deleteCommand = new DeleteCommand(targetIndex);

        ModelManager expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.deleteStudent(studentToDelete);

        String expectedMessage = "Deleted student: Clean Student; Phone: 91234567";
        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_studentWithMultipleTagsAndFollowUp_feedbackIdentifiesSortedTagsAndFollowUp() {
        Student studentToDelete = new StudentBuilder().withName("Flagged Student").withPhone("91234567")
                .withoutEmail().withTags("urgent", "lab3").withFlag(true).build();
        model.addStudent(studentToDelete);
        Index targetIndex = Index.fromOneBased(model.getFilteredStudentList().size());
        DeleteCommand deleteCommand = new DeleteCommand(targetIndex);

        ModelManager expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.deleteStudent(studentToDelete);

        String expectedMessage = "Deleted student: Flagged Student; Phone: 91234567; "
                + "Tags: [lab3][urgent]; Needs follow-up";
        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredStudentList().size() + 1);
        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validIndexFilteredList_success() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);

        Student studentToDelete = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_STUDENT);

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS,
                Messages.format(studentToDelete));

        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.deleteStudent(studentToDelete);
        showNoStudent(expectedModel);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidIndexFilteredList_throwsCommandException() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);

        Index outOfBoundIndex = INDEX_SECOND_STUDENT;
        // ensures that outOfBoundIndex is still in bounds of student book list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getStudentBook().getStudentList().size());

        DeleteCommand deleteCommand = new DeleteCommand(outOfBoundIndex);

        assertCommandFailure(deleteCommand, model, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validNusStudentIdUnfilteredList_success() {
        // CARL is not the first student, so a lookup that stopped at the first student holding
        // any ID at all would leave the wrong student in the book
        Student studentToDelete = CARL;
        DeleteCommand deleteCommand = deleteById("A23456789D");

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS,
                Messages.format(studentToDelete));

        ModelManager expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.deleteStudent(studentToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_nusStudentIdIgnoresCase_success() {
        // NusStudentId normalizes its value, so a lowercase ID names the same student
        Student studentToDelete = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        DeleteCommand deleteCommand = deleteById("a12345678c");

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS,
                Messages.format(studentToDelete));

        ModelManager expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.deleteStudent(studentToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_nusStudentIdOfStudentHiddenByFilter_success() {
        // an NUS Student ID names its holder wherever they are in the book, so a search
        // that hides the student cannot put them out of reach of delete
        showStudentAtIndex(model, INDEX_SECOND_STUDENT);

        DeleteCommand deleteCommand = deleteById("A12345678C");

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS,
                Messages.format(ALICE));

        ModelManager expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        showStudentAtIndex(expectedModel, INDEX_SECOND_STUDENT);
        expectedModel.deleteStudent(ALICE);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_studentHoldingNoNusStudentId_isNotNamedById() {
        // a student with no NUS Student ID is passed over when scanning for one
        Student studentWithoutId = new StudentBuilder().withName("No Ida").withPhone("91234567")
                .withoutNusStudentId().build();
        StudentBook studentBook = new StudentBook();
        studentBook.addStudent(studentWithoutId);
        model = new ModelManager(studentBook, new UserPrefs());
        DeleteCommand deleteCommand = deleteById("A12345678C");

        assertCommandFailure(deleteCommand, model,
                String.format(Messages.MESSAGE_NO_STUDENT_WITH_NUS_ID, "A12345678C"));
    }

    @Test
    public void execute_unknownNusStudentId_throwsCommandException() {
        DeleteCommand deleteCommand = deleteById("A99999999Z");

        assertCommandFailure(deleteCommand, model,
                String.format(Messages.MESSAGE_NO_STUDENT_WITH_NUS_ID, "A99999999Z"));
    }

    @Test
    public void equals() {
        DeleteCommand deleteFirstCommand = new DeleteCommand(INDEX_FIRST_STUDENT);
        DeleteCommand deleteSecondCommand = new DeleteCommand(INDEX_SECOND_STUDENT);
        DeleteCommand deleteAliceByIdCommand = deleteById("A12345678C");

        // same object -> returns true
        assertTrue(deleteFirstCommand.equals(deleteFirstCommand));

        // same values -> returns true
        DeleteCommand deleteFirstCommandCopy = new DeleteCommand(INDEX_FIRST_STUDENT);
        assertTrue(deleteFirstCommand.equals(deleteFirstCommandCopy));

        // an index and the target that holds it name the same student -> returns true
        assertTrue(deleteFirstCommand.equals(
                new DeleteCommand(StudentTarget.byIndex(INDEX_FIRST_STUDENT))));

        // same NUS Student ID -> returns true
        assertTrue(deleteAliceByIdCommand.equals(deleteById("A12345678C")));

        // different types -> returns false
        assertFalse(deleteFirstCommand.equals(1));

        // null -> returns false
        assertFalse(deleteFirstCommand.equals(null));

        // different student -> returns false
        assertFalse(deleteFirstCommand.equals(deleteSecondCommand));

        // same position but a different way of naming a student -> returns false
        assertFalse(deleteFirstCommand.equals(deleteAliceByIdCommand));
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_STUDENT);
        assertThrows(NullPointerException.class, () -> deleteCommand.execute(null));
    }

    @Test
    public void execute_validIndexLastStudentUnfilteredList_success() {
        Index lastIndex = Index.fromOneBased(model.getFilteredStudentList().size());
        Student studentToDelete = model.getFilteredStudentList().get(lastIndex.getZeroBased());
        DeleteCommand deleteCommand = new DeleteCommand(lastIndex);

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS,
                Messages.format(studentToDelete));

        ModelManager expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.deleteStudent(studentToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_studentActuallyRemovedFromStudentBook_success() throws Exception {
        Student studentToDelete = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        int initialSize = model.getFilteredStudentList().size();
        DeleteCommand deleteCommand = new DeleteCommand(INDEX_FIRST_STUDENT);

        deleteCommand.execute(model);

        assertEquals(initialSize - 1, model.getFilteredStudentList().size());
        assertFalse(model.getFilteredStudentList().contains(studentToDelete));
        assertFalse(model.getStudentBook().getStudentList().contains(studentToDelete));
    }

    @Test
    public void execute_identicalStudentsWithoutNusStudentIds_deletesSelectedStudent() {
        Student first = new StudentBuilder().withName("Tan Wei Ming").withPhone("91234567")
                .withoutNusStudentId().build();
        Student middle = new StudentBuilder().withName("Alice Tan").withPhone("92345678")
                .withoutNusStudentId().build();
        Student second = new StudentBuilder(first).build();
        StudentBook studentBook = new StudentBook();
        studentBook.addStudent(first);
        studentBook.addStudent(middle);
        studentBook.addStudent(second);
        model = new ModelManager(studentBook, new UserPrefs());
        DeleteCommand command = new DeleteCommand(Index.fromOneBased(3));
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.deleteStudent(expectedModel.getFilteredStudentList().get(2));

        assertCommandSuccess(command, model,
                String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS, Messages.format(second)), expectedModel);
        assertSame(first, model.getStudentBook().getStudentList().get(0));
        assertSame(middle, model.getStudentBook().getStudentList().get(1));
    }

    @Test
    public void toStringMethod() {
        Index targetIndex = Index.fromOneBased(1);
        DeleteCommand deleteCommand = new DeleteCommand(targetIndex);
        String expected = DeleteCommand.class.getCanonicalName()
                + "{target=" + StudentTarget.byIndex(targetIndex) + "}";
        assertEquals(expected, deleteCommand.toString());
    }

    /** Returns the delete command that names the student holding {@code nusStudentId}. */
    private static DeleteCommand deleteById(String nusStudentId) {
        return new DeleteCommand(StudentTarget.byNusStudentId(new NusStudentId(nusStudentId)));
    }

    /**
     * Updates {@code model}'s filtered list to show no one.
     */
    private void showNoStudent(Model model) {
        model.updateFilteredStudentList(p -> false);

        assertTrue(model.getFilteredStudentList().isEmpty());
    }
}
