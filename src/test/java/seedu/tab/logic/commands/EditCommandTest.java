package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.commands.CommandTestUtil.DESC_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.DESC_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.logic.commands.CommandTestUtil.showStudentAtIndex;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NO_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static seedu.tab.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate.Field;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.EditStudentDescriptorBuilder;
import seedu.tab.testutil.StudentBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for EditCommand.
 */
public class EditCommandTest {

    private Model model = new ModelManager(getTypicalStudentBook(), new UserPrefs());

    @Test
    public void messageUsage_includesNusStudentIdAlias() {
        assertTrue(EditCommand.MESSAGE_USAGE.contains(FLAG_NUS_STUDENT_ID.getMarkerSummary()));
    }

    @Test
    public void messageUsage_includesFollowUpAliases() {
        assertTrue(EditCommand.MESSAGE_USAGE.contains(FLAG_FOLLOW_UP.getMarkerSummary()));
        assertTrue(EditCommand.MESSAGE_USAGE.contains(FLAG_NO_FOLLOW_UP.getMarkerSummary()));
    }

    @Test
    public void messageConflictingFollowUp_namesEachFlagWithItsAliases() {
        assertTrue(EditCommand.MESSAGE_CONFLICTING_FOLLOW_UP.contains(FLAG_FOLLOW_UP.getMarkerWithAliases()));
        assertTrue(EditCommand.MESSAGE_CONFLICTING_FOLLOW_UP.contains(FLAG_NO_FOLLOW_UP.getMarkerWithAliases()));
    }

    @Test
    public void execute_allFieldsSpecifiedUnfilteredList_success() {
        Student editedStudent = new StudentBuilder().build();
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder(editedStudent).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT, descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        expectedModel.setStudent(model.getFilteredStudentList().get(0), editedStudent);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_someFieldsSpecifiedUnfilteredList_success() {
        Index indexLastStudent = Index.fromOneBased(model.getFilteredStudentList().size());
        Student lastStudent = model.getFilteredStudentList().get(indexLastStudent.getZeroBased());

        StudentBuilder studentInList = new StudentBuilder(lastStudent);
        Student editedStudent = studentInList.withName(VALID_NAME_BOB).withPhone(VALID_PHONE_BOB)
                .withTags(VALID_TAG_HUSBAND).build();

        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB)
                .withPhone(VALID_PHONE_BOB).withTags(VALID_TAG_HUSBAND).build();
        EditCommand editCommand = new EditCommand(indexLastStudent, descriptor);

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        expectedModel.setStudent(lastStudent, editedStudent);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_studentWithoutEmail_keepsItAbsentWhenAnotherFieldIsEdited() {
        Student first = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student withoutEmail = new StudentBuilder(first).withoutEmail().build();
        model.setStudent(first, withoutEmail);

        Student editedStudent = new StudentBuilder(withoutEmail).withPhone(VALID_PHONE_BOB).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_BOB).build());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        expectedModel.setStudent(withoutEmail, editedStudent);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
        assertEquals(Optional.empty(), editedStudent.getEmail());
    }

    @Test
    public void execute_studentWithoutEmail_takesOneGivenLater() {
        Student first = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student withoutEmail = new StudentBuilder(first).withoutEmail().build();
        model.setStudent(first, withoutEmail);

        Student editedStudent = new StudentBuilder(withoutEmail).withEmail(VALID_EMAIL_BOB).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withEmail(VALID_EMAIL_BOB).build());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        expectedModel.setStudent(withoutEmail, editedStudent);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
        assertEquals(Optional.of(new Email(VALID_EMAIL_BOB)), editedStudent.getEmail());
    }

    @Test
    public void execute_clearEmail_preservesOtherDetails() {
        Student first = model.getFilteredStudentList().get(0);
        Student edited = new StudentBuilder(first).withoutEmail().build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.setStudent(first, edited);
        EditCommand command = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withoutEmail().build());

        assertCommandSuccess(command, model,
                String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(edited)), expectedModel);
        assertCommandSuccess(command, model,
                String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(edited)), expectedModel);
    }

    @Test
    public void execute_clearTags_feedbackOmitsTags() {
        Student first = model.getFilteredStudentList().get(0);
        Student edited = new StudentBuilder(first).withoutEmail().withTags().build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.setStudent(first, edited);
        EditCommand command = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withoutEmail().withTags().build());

        String expectedMessage = "Edited student: Alice Pauline; NUS Student ID: A12345678C; Phone: 94351253";
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_multipleTags_feedbackSortsTagsAlphabetically() {
        Student first = model.getFilteredStudentList().get(0);
        Student edited = new StudentBuilder(first).withoutEmail().withTags("urgent", "lab3").build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.setStudent(first, edited);
        EditCommand command = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withoutEmail().withTags("urgent", "lab3").build());

        String expectedMessage = "Edited student: Alice Pauline; NUS Student ID: A12345678C; Phone: 94351253; "
                + "Tags: [lab3][urgent]";
        assertCommandSuccess(command, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_setNusStudentId_addsItToStudent() {
        Student original = model.getFilteredStudentList().get(0);
        Student edited = new StudentBuilder(original).withNusStudentId("A01234567X").build();
        EditStudentDescriptor descriptor = new EditStudentDescriptor();
        descriptor.setNusStudentId(new NusStudentId("A01234567X"));
        EditCommand command = new EditCommand(INDEX_FIRST_STUDENT, descriptor);
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.setStudent(original, edited);

        assertCommandSuccess(command, model,
                String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(edited)), expectedModel);
    }

    @Test
    public void execute_unrelatedEdit_preservesNusStudentId() {
        Student original = model.getFilteredStudentList().get(0);
        Student withNusStudentId = new StudentBuilder(original).withNusStudentId("A01234567X").build();
        model.setStudent(original, withNusStudentId);
        Student edited = new StudentBuilder(withNusStudentId).withPhone(VALID_PHONE_BOB).build();
        EditCommand command = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_BOB).build());
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.setStudent(withNusStudentId, edited);

        assertCommandSuccess(command, model,
                String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(edited)), expectedModel);
    }

    @Test
    public void execute_clearNusStudentId_removesItAndPreservesOtherDetails() {
        Student original = model.getFilteredStudentList().get(0);
        Student withNusStudentId = new StudentBuilder(original).withNusStudentId("A01234567X").build();
        model.setStudent(original, withNusStudentId);
        Student edited = new StudentBuilder(withNusStudentId).withoutNusStudentId().build();
        EditStudentDescriptor descriptor = new EditStudentDescriptor();
        descriptor.clearNusStudentId();
        EditCommand command = new EditCommand(INDEX_FIRST_STUDENT, descriptor);
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.setStudent(withNusStudentId, edited);

        assertCommandSuccess(command, model,
                String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(edited)), expectedModel);
    }

    @Test
    public void execute_setFollowUpFlag_changesFalseToTrue() {
        Student original = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student editedStudent = new StudentBuilder(original).withFlag(true).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withFlag(true).build());

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        expectedModel.setStudent(original, editedStudent);
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
        // Repeating the request keeps the flag set.
        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_clearFollowUpFlag_changesTrueToFalse() {
        Student original = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student flaggedStudent = new StudentBuilder(original).withFlag(true).build();
        model.setStudent(original, flaggedStudent);
        Student editedStudent = new StudentBuilder(flaggedStudent).withFlag(false).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withFlag(false).build());

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        expectedModel.setStudent(flaggedStudent, editedStudent);
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
        // Repeating the request keeps the flag cleared.
        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_unrelatedEdit_preservesFollowUpFlag() {
        Student original = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student flaggedStudent = new StudentBuilder(original).withFlag(true).build();
        model.setStudent(original, flaggedStudent);
        Student editedStudent = new StudentBuilder(flaggedStudent).withPhone(VALID_PHONE_BOB).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_BOB).build());

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        expectedModel.setStudent(flaggedStudent, editedStudent);
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
        assertTrue(editedStudent.isFlagged());
    }

    @Test
    public void execute_setFollowUpFlagInFilteredList_changesDisplayedStudent() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);
        Student original = model.getFilteredStudentList().get(0);
        Student editedStudent = new StudentBuilder(original).withFlag(true).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withFlag(true).build());

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        showStudentAtIndex(expectedModel, INDEX_FIRST_STUDENT);
        expectedModel.setStudent(original, editedStudent);
        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_repeatedEditAfterFilter_targetsSameStudent() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);
        Student firstShown = model.getFilteredStudentList().get(0);

        EditCommand setFlag = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withFlag(true).build());
        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        showStudentAtIndex(expectedModel, INDEX_FIRST_STUDENT);
        Student flagged = new StudentBuilder(firstShown).withFlag(true).build();
        expectedModel.setStudent(firstShown, flagged);
        assertCommandSuccess(setFlag, model, String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(flagged)), expectedModel);

        assertEquals(List.of(flagged), model.getFilteredStudentList());

        EditCommand clearFlag = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withFlag(false).build());
        Model clearedModel = new ModelManager(new StudentBook(expectedModel.getStudentBook()), new UserPrefs());
        showStudentAtIndex(clearedModel, INDEX_FIRST_STUDENT);
        Student cleared = new StudentBuilder(flagged).withFlag(false).build();
        clearedModel.setStudent(flagged, cleared);
        assertCommandSuccess(clearFlag, model, String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(cleared)), clearedModel);

        assertEquals(cleared, model.getFilteredStudentList().get(0));
    }

    @Test
    public void execute_editRemovingMatchAfterFilter_shiftsRemainingIndices() {
        model.updateFilteredStudentList(
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("Meier"))));
        Student firstMatch = model.getFilteredStudentList().get(0);
        Student secondMatch = model.getFilteredStudentList().get(1);

        EditCommand renameFirst = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB).build());
        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        expectedModel.updateFilteredStudentList(
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("Meier"))));
        Student renamed = new StudentBuilder(firstMatch).withName(VALID_NAME_BOB).build();
        expectedModel.setStudent(firstMatch, renamed);
        assertCommandSuccess(renameFirst, model, String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(renamed)), expectedModel);

        assertEquals(List.of(secondMatch), model.getFilteredStudentList());

        EditCommand editShifted = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_BOB).build());
        Model shiftedModel = new ModelManager(new StudentBook(expectedModel.getStudentBook()), new UserPrefs());
        shiftedModel.updateFilteredStudentList(
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("Meier"))));
        Student editedShifted = new StudentBuilder(secondMatch).withPhone(VALID_PHONE_BOB).build();
        shiftedModel.setStudent(secondMatch, editedShifted);
        assertCommandSuccess(editShifted, model, String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedShifted)), shiftedModel);
    }

    @Test
    public void execute_noFieldSpecifiedUnfilteredList_success() {
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT, new EditStudentDescriptor());
        Student editedStudent = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_filteredList_success() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);

        Student studentInFilteredList = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student editedStudent = new StudentBuilder(studentInFilteredList).withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB).build());

        String expectedMessage = String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS,
                Messages.format(editedStudent));

        Model expectedModel = new ModelManager(new StudentBook(model.getStudentBook()), new UserPrefs());
        showStudentAtIndex(expectedModel, INDEX_FIRST_STUDENT);
        expectedModel.setStudent(model.getFilteredStudentList().get(0), editedStudent);

        assertCommandSuccess(editCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_duplicateStudentUnfilteredList_failure() {
        Student firstStudent = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder(firstStudent).build();
        EditCommand editCommand = new EditCommand(INDEX_SECOND_STUDENT, descriptor);

        assertCommandFailure(editCommand, model, String.format(Messages.MESSAGE_DUPLICATE_STUDENT,
                firstStudent.getNusStudentId().orElseThrow(), firstStudent.getName()));
    }

    @Test
    public void execute_duplicateStudentFilteredList_failure() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);

        // edit student in filtered list into a duplicate in student book
        Student studentInList = model.getStudentBook().getStudentList().get(2);
        EditCommand editCommand = new EditCommand(INDEX_FIRST_STUDENT,
                new EditStudentDescriptorBuilder(studentInList).build());

        assertCommandFailure(editCommand, model, String.format(Messages.MESSAGE_DUPLICATE_STUDENT,
                studentInList.getNusStudentId().orElseThrow(), studentInList.getName()));
    }

    @Test
    public void execute_editToSameNameWithDifferentNusStudentId_success() {
        Index thirdStudentIndex = Index.fromOneBased(3);
        Student original = model.getFilteredStudentList().get(thirdStudentIndex.getZeroBased());
        Student firstStudent = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        Student edited = new StudentBuilder(original).withName(firstStudent.getName().fullName).build();
        EditCommand command = new EditCommand(thirdStudentIndex,
                new EditStudentDescriptorBuilder().withName(firstStudent.getName().fullName).build());
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.setStudent(original, edited);

        assertCommandSuccess(command, model,
                String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(edited))
                        + "\n\nWARNING: Possible duplicate student: " + firstStudent.getName() + " has the same name ("
                        + edited.getName() + ")."
                        + "\nReview the records and delete duplicates only if they are the same person.",
                expectedModel);
    }

    @Test
    public void execute_sameNameWithoutNusStudentIds_editOneSuccess() {
        Student first = new StudentBuilder().withName("Tan Wei Ming").withPhone("91234567")
                .withoutNusStudentId().build();
        Student second = new StudentBuilder(first).withPhone("92345678")
                .withEmail("second@example.com").build();
        StudentBook sameNameStudentBook = new StudentBook();
        sameNameStudentBook.addStudent(first);
        sameNameStudentBook.addStudent(second);
        model = new ModelManager(sameNameStudentBook, new UserPrefs());
        Student edited = new StudentBuilder(second).withPhone("93456789").build();
        EditCommand command = new EditCommand(INDEX_SECOND_STUDENT,
                new EditStudentDescriptorBuilder().withPhone("93456789").build());
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.setStudent(second, edited);

        assertCommandSuccess(command, model,
                String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(edited)), expectedModel);
    }

    @Test
    public void execute_identicalStudentsWithoutNusStudentIds_editsSelectedStudent() {
        Student first = new StudentBuilder().withName("Tan Wei Ming").withPhone("91234567")
                .withoutNusStudentId().build();
        Student second = new StudentBuilder(first).build();
        StudentBook studentBook = new StudentBook();
        studentBook.addStudent(first);
        studentBook.addStudent(second);
        model = new ModelManager(studentBook, new UserPrefs());
        Student editedSecond = new StudentBuilder(second).withPhone("92345678").build();
        EditCommand command = new EditCommand(INDEX_SECOND_STUDENT,
                new EditStudentDescriptorBuilder().withPhone("92345678").build());
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.setStudent(expectedModel.getFilteredStudentList().get(1), editedSecond);

        assertCommandSuccess(command, model,
                String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(editedSecond)),
                expectedModel);
        assertSame(first, model.getFilteredStudentList().get(0));
    }

    @Test
    public void execute_editToNusStudentIdUsedByAnotherStudent_failure() {
        Student firstStudent = model.getFilteredStudentList().get(INDEX_FIRST_STUDENT.getZeroBased());
        String duplicateId = firstStudent.getNusStudentId().orElseThrow().value.toLowerCase();
        EditCommand command = new EditCommand(INDEX_SECOND_STUDENT,
                new EditStudentDescriptorBuilder().withNusStudentId(duplicateId).build());

        assertCommandFailure(command, model, String.format(Messages.MESSAGE_DUPLICATE_STUDENT,
                firstStudent.getNusStudentId().orElseThrow(), firstStudent.getName()));
    }

    @Test
    public void execute_invalidStudentIndexUnfilteredList_failure() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredStudentList().size() + 1);
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB).build();
        EditCommand editCommand = new EditCommand(outOfBoundIndex, descriptor);

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_invalidStudentIndexWithFollowUp_failureWithoutMutation() {
        Index outOfBoundIndex = Index.fromOneBased(model.getFilteredStudentList().size() + 1);
        EditCommand editCommand = new EditCommand(outOfBoundIndex,
                new EditStudentDescriptorBuilder().withFlag(true).build());

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    /**
     * Edit filtered list where index is larger than size of filtered list,
     * but smaller than size of student book
     */
    @Test
    public void execute_invalidStudentIndexFilteredList_failure() {
        showStudentAtIndex(model, INDEX_FIRST_STUDENT);
        Index outOfBoundIndex = INDEX_SECOND_STUDENT;
        // ensures that outOfBoundIndex is still in bounds of student book list
        assertTrue(outOfBoundIndex.getZeroBased() < model.getStudentBook().getStudentList().size());

        EditCommand editCommand = new EditCommand(outOfBoundIndex,
                new EditStudentDescriptorBuilder().withName(VALID_NAME_BOB).build());

        assertCommandFailure(editCommand, model, Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        final EditCommand standardCommand = new EditCommand(INDEX_FIRST_STUDENT, DESC_AMY);

        // same values -> returns true
        EditStudentDescriptor copyDescriptor = new EditStudentDescriptor(DESC_AMY);
        EditCommand commandWithSameValues = new EditCommand(INDEX_FIRST_STUDENT, copyDescriptor);
        assertTrue(standardCommand.equals(commandWithSameValues));

        // same object -> returns true
        assertTrue(standardCommand.equals(standardCommand));

        // null -> returns false
        assertFalse(standardCommand.equals(null));

        // different types -> returns false
        assertFalse(standardCommand.equals(new ClearCommand()));

        // different index -> returns false
        assertFalse(standardCommand.equals(new EditCommand(INDEX_SECOND_STUDENT, DESC_AMY)));

        // different descriptor -> returns false
        assertFalse(standardCommand.equals(new EditCommand(INDEX_FIRST_STUDENT, DESC_BOB)));
    }

    @Test
    public void toStringMethod() {
        Index index = Index.fromOneBased(1);
        EditStudentDescriptor editStudentDescriptor = new EditStudentDescriptor();
        EditCommand editCommand = new EditCommand(index, editStudentDescriptor);
        String expected = EditCommand.class.getCanonicalName() + "{index=" + index + ", editStudentDescriptor="
                + editStudentDescriptor + "}";
        assertEquals(expected, editCommand.toString());
    }

}
