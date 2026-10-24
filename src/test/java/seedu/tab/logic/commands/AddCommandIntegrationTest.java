package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.logic.parser.StudentBookParser;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.StudentBook;
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
                new CommandResult(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(validStudent)),
                        validStudent),
                expectedModel);
    }

    @Test
    public void execute_newStudentWithoutEmailOrTags_successWithCleanFeedback() {
        Student validStudent = new StudentBuilder().withName("Bob Choo").withPhone("81112222")
                .withoutEmail().withTags().build();

        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(validStudent);

        assertCommandSuccess(new AddCommand(validStudent), model,
                new CommandResult("New student added: Bob Choo; Phone: 81112222", validStudent),
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
                new CommandResult("New student added: Bob Choo; Phone: 81112222; Email: bob@example.com; "
                + "Tags: [lab3][urgent]; Needs follow-up", validStudent),
                expectedModel);
    }

    @Test
    public void execute_matchingSearch_preservesIndexesForTagAndDelete() throws Exception {
        setUpTutorialSearch();
        var predicate = model.getFilteredStudentPredicate();
        String feedback = executeCommand("add Eve Ng -p 91110005 -t T1");

        assertEquals("New student added: Eve Ng; Phone: 91110005; Tags: [T1]", feedback);
        assertSame(predicate, model.getFilteredStudentPredicate());
        assertEquals(List.of("Alice Tan", "Carl Ong", "Eve Ng"), displayedNames());
        assertEquals("Added tag [Lab 3]: Carl Ong; Phone: 91110003; Tags: [Lab 3][T1]",
                executeCommand("tag 2 -a \"Lab 3\""));
        assertEquals("Deleted student: Carl Ong; Phone: 91110003; Tags: [Lab 3][T1]",
                executeCommand("delete 2"));
        assertEquals(List.of("Alice Tan", "Eve Ng"), displayedNames());
        executeCommand("list");
        assertEquals(List.of("Alice Tan", "Bob Lim", "Eve Ng"), displayedNames());
    }

    @Test
    public void execute_nonMatchingSearch_clearsSearchWithNotice() throws Exception {
        setUpTutorialSearch();

        assertEquals("New student added: Eve Ng; Phone: 91110005; Tags: [T2]\n"
                + "Search cleared to show the new student.", executeCommand("add Eve Ng -p 91110005 -t T2"));
        assertEquals(List.of("Alice Tan", "Bob Lim", "Carl Ong", "Eve Ng"), displayedNames());
        assertEquals("Deleted student: Bob Lim; Phone: 91110002; Tags: [T2]", executeCommand("delete 2"));
    }

    @Test
    public void execute_emptySearch_showsMatchingStudentAndClearsForNonMatch() throws Exception {
        setUpTutorialSearch();
        executeCommand("find -t T3");
        var predicate = model.getFilteredStudentPredicate();
        assertEquals("New student added: Eve Ng; Phone: 91110005; Tags: [t3]",
                executeCommand("add Eve Ng -p 91110005 -t t3"));
        assertSame(predicate, model.getFilteredStudentPredicate());
        assertEquals(List.of("Eve Ng"), displayedNames());

        executeCommand("find -t T4");
        assertEquals("New student added: Dan Koh; Phone: 91110004; Tags: [T2]\n"
                + "Search cleared to show the new student.", executeCommand("add Dan Koh -p 91110004 -t T2"));
        assertEquals(List.of("Alice Tan", "Bob Lim", "Carl Ong", "Eve Ng", "Dan Koh"), displayedNames());
    }

    @Test
    public void execute_duplicateOutsideSearch_preservesSearchAndBook() throws Exception {
        setUpTutorialSearch();
        executeCommand("list");
        executeCommand("edit 2 --id A01234567X");
        executeCommand("find -t T1");
        var predicate = model.getFilteredStudentPredicate();
        Student duplicate = new StudentBuilder().withName("Eve Ng").withNusStudentId("A01234567X").build();

        assertCommandFailure(new AddCommand(duplicate), model,
                String.format(Messages.MESSAGE_DUPLICATE_STUDENT, "A01234567X", "Bob Lim"));
        assertSame(predicate, model.getFilteredStudentPredicate());
        assertEquals(List.of("Alice Tan", "Carl Ong"), displayedNames());
    }

    private void setUpTutorialSearch() throws Exception {
        model = new ModelManager(new StudentBook(), new UserPrefs());
        executeCommand("add Alice Tan -p 91110001 -t T1");
        executeCommand("add Bob Lim -p 91110002 -t T2");
        executeCommand("add Carl Ong -p 91110003 -t T1");
        executeCommand("find -t T1");
    }

    private String executeCommand(String command) throws Exception {
        return new StudentBookParser().parseCommand(command).execute(model).getFeedbackToUser();
    }

    private List<String> displayedNames() {
        return model.getFilteredStudentList().stream().map(student -> student.getName().fullName).toList();
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
                new CommandResult(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(sameName))
                        + "\n\nWARNING: Possible duplicate student: Alice Pauline has the same name (aLiCe pAuLiNe)."
                        + "\nReview the records and delete duplicates only if they are the same person.", sameName),
                expectedModel);
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
                new CommandResult(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(second))
                        + "\n\nWARNING: Possible duplicate student: Tan Wei Ming has the same name (Tan Wei Ming)."
                        + "\nReview the records and delete duplicates only if they are the same person.", second),
                expectedModel);
    }

    @Test
    public void execute_phoneWithDifferentSpacing_addsStudentAndWarns() {
        Student studentInList = model.getStudentBook().getStudentList().get(0);
        Student matchingPhone = new StudentBuilder().withName("Different Name")
                .withPhone("9435 1253").withEmail("unique@example.com").build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(matchingPhone);

        assertCommandSuccess(new AddCommand(matchingPhone), model,
                new CommandResult(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(matchingPhone))
                        + "\n\nWARNING: Possible duplicate student: " + studentInList.getName()
                        + " has the same phone (9435 1253)."
                        + "\nReview the records and delete duplicates only if they are the same person.",
                        matchingPhone),
                expectedModel);
    }

    @Test
    public void execute_emailWithDifferentCase_addsStudentAndWarns() throws Exception {
        executeCommand("find -n Nobody");
        Student studentInList = model.getStudentBook().getStudentList().get(0);
        Student matchingEmail = new StudentBuilder().withName("Different Name")
                .withPhone("81112222").withEmail("ALICE@EXAMPLE.COM").build();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.addStudent(matchingEmail);

        assertCommandSuccess(new AddCommand(matchingEmail), model,
                new CommandResult(String.format(AddCommand.MESSAGE_SUCCESS, Messages.format(matchingEmail))
                        + "\n\nWARNING: Possible duplicate student: " + studentInList.getName()
                        + " has the same email (ALICE@EXAMPLE.COM)."
                        + "\nReview the records and delete duplicates only if they are the same person."
                        + "\nSearch cleared to show the new student.", matchingEmail),
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
                new CommandResult("New student added: Nus Student; NUS Student ID: A01234567X; Phone: 85355255; "
                        + "Email: amy@gmail.com", student), expectedModel);
    }

}
