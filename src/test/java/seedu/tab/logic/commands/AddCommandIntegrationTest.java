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
    public void execute_newStudentWithoutEmailOrTags_successWithCleanFeedback() {
        Student validStudent = new StudentBuilder().withName("Bob Choo").withPhone("81112222")
                .withoutEmail().withTags().build();

        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(validStudent);

        assertCommandSuccess(new AddCommand(validStudent), model,
                "New student added: Bob Choo; Phone: 81112222",
                expectedModel);
    }

    @Test
    public void execute_newStudentWithMultipleTagsAndFlag_successWithSortedTagsAndFollowUp() {
        Student validStudent = new StudentBuilder().withName("Bob Choo").withPhone("81112222")
                .withEmail("bob@example.com").withTags("urgent", "lab3")
                .withFlag(true).build();

        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(validStudent);

        assertCommandSuccess(new AddCommand(validStudent), model,
                "New student added: Bob Choo; Phone: 81112222; Email: bob@example.com; "
                + "Tags: [lab3][urgent]; Needs follow-up",
                expectedModel);
    }

    @Test
    public void execute_duplicateStudent_throwsCommandException() {
        Student studentInList = model.getStudentBook().getStudentList().get(0);
        assertCommandFailure(new AddCommand(studentInList), model,
                String.format(Messages.MESSAGE_DUPLICATE_STUDENT,
                        studentInList.getNusStudentId().orElseThrow(), studentInList.getName()));
    }

    @Test
    public void execute_sameNameDifferentNusStudentId_addsBothStudents() {
        Student studentInList = model.getStudentBook().getStudentList().get(0);
        Student sameName = new StudentBuilder().withName("  aLiCe   pAuLiNe  ").withPhone("81112222")
                .withEmail("unique@example.com").withNusStudentId("A76543210Y").build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(sameName);

        assertCommandSuccess(new AddCommand(sameName), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(sameName))
                        + "\n\nWARNING: Possible duplicate student: Alice Pauline has the same name (aLiCe pAuLiNe). "
                        + "Delete one if they are the same person.", expectedModel);
    }

    @Test
    public void execute_sameNameWithoutNusStudentIds_addsBothStudents() {
        Student first = new StudentBuilder().withName("Tan Wei Ming").withoutNusStudentId().build();
        Student second = new StudentBuilder(first).withPhone("91234567")
                .withEmail("second@example.com").build();
        model.addStudent(first);
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(second);

        assertCommandSuccess(new AddCommand(second), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(second))
                        + "\n\nWARNING: Possible duplicate student: Tan Wei Ming has the same name (Tan Wei Ming). "
                        + "Delete one if they are the same person.", expectedModel);
    }

    @Test
    public void execute_phoneWithDifferentSpacing_addsStudentAndWarns() {
        Student studentInList = model.getStudentBook().getStudentList().get(0);
        Student matchingPhone = new StudentBuilder().withName("Different Name")
                .withPhone("9435 1253").withEmail("unique@example.com").build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(matchingPhone);

        assertCommandSuccess(new AddCommand(matchingPhone), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(matchingPhone))
                        + "\n\nWARNING: Possible duplicate student: " + studentInList.getName()
                        + " has the same phone (9435 1253). "
                        + "Delete one if they are the same person.", expectedModel);
    }

    @Test
    public void execute_emailWithDifferentCase_addsStudentAndWarns() {
        Student studentInList = model.getStudentBook().getStudentList().get(0);
        Student matchingEmail = new StudentBuilder().withName("Different Name")
                .withPhone("81112222").withEmail("ALICE@EXAMPLE.COM").build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(matchingEmail);

        assertCommandSuccess(new AddCommand(matchingEmail), model,
                String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(matchingEmail))
                        + "\n\nWARNING: Possible duplicate student: " + studentInList.getName()
                        + " has the same email (ALICE@EXAMPLE.COM). Delete one if they are the same person.",
                expectedModel);
    }

    @Test
    public void execute_sameNusStudentIdDifferentNames_throwsCommandExceptionWithoutMutation() {
        Student first = new StudentBuilder().withName("First Student").withNusStudentId("A01234567X").build();
        Student second = new StudentBuilder().withName("Second Student").withNusStudentId("a01234567x").build();
        model.addStudent(first);

        assertCommandFailure(new AddCommand(second), model,
                String.format(Messages.MESSAGE_DUPLICATE_STUDENT,
                        second.getNusStudentId().orElseThrow(), first.getName()));
    }

    @Test
    public void execute_studentWithNusStudentId_successMessageIncludesNormalizedId() {
        Student student = new StudentBuilder().withName("Nus Student").withNusStudentId("a01234567x").build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(student);

        assertCommandSuccess(new AddCommand(student), model,
                "New student added: Nus Student; NUS Student ID: A01234567X; Phone: 85355255; "
                        + "Email: amy@gmail.com", expectedModel);
    }

}
