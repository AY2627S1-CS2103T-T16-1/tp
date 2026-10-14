package seedu.tab.logic.commands;

import static seedu.tab.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code AddCommand}.
 */
public class AddCommandIntegrationTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalStudentBook(), new UserPrefs());
    }

    @Test
    public void execute_newStudent_success() {
        Student validStudent = new StudentBuilder().build();

        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(validStudent);

        assertCommandSuccess(new AddCommand(validStudent), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(validStudent)),
                expectedModel);
    }

    @Test
    public void execute_duplicateStudent_throwsCommandException() {
        Student studentInList = model.getStudentBook().getStudentList().get(0);
        assertCommandFailure(new AddCommand(studentInList), model,
                AddCommand.MESSAGE_DUPLICATE_STUDENT);
    }

    @Test
    public void execute_sameNameDifferentNusStudentId_throwsCommandExceptionWithoutMutation() {
        Student studentInList = model.getStudentBook().getStudentList().get(0);
        Student duplicateName = new StudentBuilder(studentInList).withNusStudentId("A76543210Y").build();

        assertCommandFailure(new AddCommand(duplicateName), model,
                AddCommand.MESSAGE_DUPLICATE_STUDENT);
    }

    @Test
    public void execute_sameNusStudentIdDifferentNames_bothStudentsAccepted() {
        Student first = new StudentBuilder().withName("First Student").withNusStudentId("A01234567X").build();
        Student second = new StudentBuilder().withName("Second Student").withNusStudentId("A01234567X").build();
        model.addStudent(first);
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(second);

        assertCommandSuccess(new AddCommand(second), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(second)), expectedModel);
    }

    @Test
    public void execute_studentWithNusStudentId_successMessageIncludesNormalizedId() {
        Student student = new StudentBuilder().withName("Nus Student").withNusStudentId("a01234567x").build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(student);

        assertCommandSuccess(new AddCommand(student), model,
                "New student added: Nus Student; NUS Student ID: A01234567X; Phone: 85355255; "
                        + "Email: amy@gmail.com; Tags: ", expectedModel);
    }

}
