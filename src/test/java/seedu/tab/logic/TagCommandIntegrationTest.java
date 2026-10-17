package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static seedu.tab.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.logic.commands.TagCommand;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.Student;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.StorageManager;
import seedu.tab.testutil.StudentBuilder;

public class TagCommandIntegrationTest {

    @TempDir
    public Path testFolder;

    private Model model;
    private Logic logic;
    private JsonStudentBookStorage storage;
    private Student student;
    private Student otherStudent;
    private int saveCount;

    @BeforeEach
    public void setUp() throws Exception {
        student = new StudentBuilder().withTags("T1", "Lab 3").withFlag(true)
                .withNusStudentId("A01234567X").build();
        otherStudent = new StudentBuilder().withName("Other Student").withoutEmail().build();
        model = new ModelManager();
        model.addStudent(otherStudent);
        model.addStudent(student);
        model.updateFilteredStudentList(student::equals);
        storage = new JsonStudentBookStorage(testFolder.resolve("tab.json")) {
            @Override
            public void saveStudentBook(ReadOnlyStudentBook studentBook) throws IOException {
                saveCount++;
                super.saveStudentBook(studentBook);
            }
        };
        storage.saveStudentBook(model.getStudentBook());
        saveCount = 0;
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
    }

    @Test
    public void execute_addTag_preservesEveryFieldAndPersistsAfterReload() throws Exception {
        // The first displayed student is the second record in storage.
        model.updateFilteredStudentList(shown -> shown.getName().equals(student.getName()));
        Student expected = new StudentBuilder(student).withTags("T1", "Lab 3", "New group").build();
        assertEquals("Added tag [New group]: Amy Bee; NUS Student ID: A01234567X; Phone: 85355255; "
                + "Email: amy@gmail.com; Tags: [Lab 3][New group][T1]; Needs follow-up",
                logic.execute("tag 1 --add \"  New\u00a0  group  \"").getFeedbackToUser());
        assertEquals(List.of(expected), model.getFilteredStudentList());
        assertEquals(List.of(otherStudent, expected), model.getStudentBook().getStudentList());
        Model restartedModel = new ModelManager(storage.readStudentBook().orElseThrow(), new UserPrefs());
        assertEquals(List.of(otherStudent, expected), restartedModel.getStudentBook().getStudentList());
        assertEquals(1, saveCount);
        assertEquals(new StudentBuilder().withTags("T1", "Lab 3").withFlag(true)
                .withNusStudentId("A01234567X").build(), student);
    }

    @Test
    public void execute_minimalStudent_preservesAbsentFieldsAndDoesNotSetFollowUp() throws Exception {
        model.updateFilteredStudentList(shown -> shown.getName().equals(otherStudent.getName()));
        logic.execute("tag 1 -a \"Needs follow-up\"");
        Student expected = new StudentBuilder(otherStudent).withTags("Needs follow-up").build();
        assertEquals(List.of(expected, student), storage.readStudentBook().orElseThrow().getStudentList());
        assertEquals(List.of(expected), model.getFilteredStudentList());
    }

    @Test
    public void execute_duplicateVariants_preservesModelDisplayAndSavedBytes() throws Exception {
        Map<String, String> storedSpellings = Map.of("Lab 3", "Lab 3", "lab 3", "Lab 3", "LAB 3", "Lab 3",
                "  lAb\t 3\u00a0", "Lab 3", "T1", "T1", "t1", "T1");
        for (Map.Entry<String, String> spelling : storedSpellings.entrySet()) {
            assertFailureWithoutMutation("tag 1 -a \"" + spelling.getKey() + "\"", CommandException.class,
                    "This student already has tag [" + spelling.getValue() + "]. No changes made.");
        }
    }

    @Test
    public void execute_invalidInput_preservesModelDisplayAndSavedBytes() throws Exception {
        Map<String, String> errors = Map.of(
                "tag 1 -t T2", "There is no -t option.\n" + TagCommand.MESSAGE_ADD_GUIDANCE,
                "tag 1 --tag T2", "There is no --tag option.\n" + TagCommand.MESSAGE_ADD_GUIDANCE,
                "tag 0 -a T2", "Index must be a positive integer.",
                "tag 2147483648 -a T2", "Index must be a positive integer.",
                "tag 1 -a \"\"", "Tag \"\" is not valid: it holds no letter or number",
                "tag 1 -a \"---\"", "Tag \"---\" is not valid: it holds no letter or number",
                "tag 1 -a T2 --add T3", "Multiple values specified for the following single-valued field(s): -a");
        for (Map.Entry<String, String> error : errors.entrySet()) {
            assertFailureWithoutMutation(error.getKey(), ParseException.class, error.getValue());
        }
        for (String index : List.of("2", "2147483647")) {
            assertFailureWithoutMutation("tag " + index + " -a T2", CommandException.class,
                    Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
        }
        model.updateFilteredStudentList(shown -> false);
        assertFailureWithoutMutation("tag 1 -a T2", CommandException.class,
                Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_negativeIndexOrMissingAction_preservesModelDisplayAndSavedBytes() throws Exception {
        String hint = "\n" + TagCommand.MESSAGE_ADD_GUIDANCE;
        Map<String, String> errors = Map.of(
                "tag -1 -a T2", "Index must be a positive integer.\nThere is no -1 option." + hint,
                "tag -1 --add T2", "Index must be a positive integer.\nThere is no -1 option." + hint,
                "edit -1 -p 91234567", "Index must be a positive integer.\nThere is no -1 option.",
                "tag 1 lab4", "Unexpected value after INDEX: \"lab4\". Put field values after an option.\n"
                        + "Missing required field(s): -a TAG" + hint,
                "tag 1", "Missing required field(s): -a TAG" + hint,
                "edit 1 Alex -p 91234567",
                        "Unexpected value after INDEX: \"Alex\". Put field values after an option.");
        for (Map.Entry<String, String> error : errors.entrySet()) {
            assertFailureWithoutMutation(error.getKey(), ParseException.class, error.getValue());
        }
    }

    @Test
    public void execute_humanEditedLegacyTags_normalizesWithoutDiscardingExistingVariants() throws Exception {
        // Legacy/edit records may contain two case variants. Tagging must retain both.
        model.updateFilteredStudentList(shown -> shown.getName().equals(student.getName()));
        Student legacy = new StudentBuilder(student).withTags("Lab 3", "lab 3", "Café").build();
        model.setStudent(student, legacy);
        storage.saveStudentBook(model.getStudentBook());
        String json = Files.readString(storage.getStudentBookFilePath()).replace("Lab 3", "  Lab   3  ");
        Files.writeString(storage.getStudentBookFilePath(), json);
        model.setStudentBook(storage.readStudentBook().orElseThrow());
        saveCount = 0;
        assertFailureWithoutMutation("tag 1 -a \" CAFE\u0301 \"", CommandException.class,
                "This student already has tag [Café]. No changes made.");
        assertFailureWithoutMutation("tag 1 -a \"LAB 3\"", CommandException.class,
                "This student already has tag [Lab 3]. No changes made.");
        logic.execute("tag 1 -a T2");
        Student expected = new StudentBuilder(legacy).withTags("Lab 3", "lab 3", "Café", "T2").build();
        assertEquals(List.of(otherStudent, expected), storage.readStudentBook().orElseThrow().getStudentList());
    }

    /** Verifies the exact failure, unchanged records and display, and absence of a storage write. */
    private <T extends Exception> void assertFailureWithoutMutation(String input, Class<T> type, String message)
            throws IOException {
        StudentBook expectedBook = new StudentBook(model.getStudentBook());
        List<Student> expectedDisplay = List.copyOf(model.getFilteredStudentList());
        String savedJson = Files.readString(storage.getStudentBookFilePath());
        assertThrows(type, message, () -> logic.execute(input));
        assertEquals(expectedBook, model.getStudentBook());
        assertEquals(expectedDisplay, model.getFilteredStudentList());
        for (int i = 0; i < expectedDisplay.size(); i++) {
            assertSame(expectedDisplay.get(i), model.getFilteredStudentList().get(i));
        }
        assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
        assertEquals(0, saveCount);
    }
}
