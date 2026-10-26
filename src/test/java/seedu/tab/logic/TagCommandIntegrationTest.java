package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.TagCommand;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.ParserUtil;
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
    public void execute_identicalFlaggedRecords_keepsFollowUpHintOnSelectedRecord() throws Exception {
        Student first = new StudentBuilder(student).withoutNusStudentId().build();
        Student second = new StudentBuilder(first).build();
        StudentBook book = new StudentBook();
        book.addStudent(first);
        book.addStudent(second);
        model.setStudentBook(book);
        model.updateFilteredStudentList(shown -> true);
        storage.saveStudentBook(model.getStudentBook());
        saveCount = 0;

        assertFailureWithoutMutation("tag 2 -r \"Needs follow-up\"", CommandException.class,
                "This student does not have tag [Needs follow-up]. No changes made.\n"
                        + String.format(TagCommand.MESSAGE_FOLLOW_UP_FLAG_HINT, 2));
    }

    @Test
    public void execute_studentIdOutsideFilter_addsAndRemovesTagsWithoutChangingOtherFields() throws Exception {
        model.updateFilteredStudentList(shown -> shown.getName().equals(otherStudent.getName()));
        Student expected = new StudentBuilder(student).withTags("T1", "Lab 3", "组 1").build();

        assertEquals("Added tag [组 1]: Amy Bee; NUS Student ID: A01234567X; Phone: 85355255; "
                + "Email: amy@gmail.com; Tags: [Lab 3][T1][组 1]; Needs follow-up",
                logic.execute("tag a01234567x --add=\"组 1\"").getFeedbackToUser());
        assertEquals(List.of(otherStudent), model.getFilteredStudentList());
        assertEquals(List.of(otherStudent, expected), storage.readStudentBook().orElseThrow().getStudentList());
        assertEquals("Removed tag [组 1]: Amy Bee; NUS Student ID: A01234567X; Phone: 85355255; "
                + "Email: amy@gmail.com; Tags: [Lab 3][T1]; Needs follow-up",
                logic.execute("tag --id a01234567x -r \"组 1\"").getFeedbackToUser());
        assertEquals(List.of(otherStudent), model.getFilteredStudentList());
        assertEquals(List.of(otherStudent, student), storage.readStudentBook().orElseThrow().getStudentList());
        assertEquals(2, saveCount);
    }

    @Test
    public void execute_digitLeadingId_usesExplicitTargetAndPreservesIndexMeaning() throws Exception {
        Student numericIdStudent = new StudentBuilder(student).withNusStudentId("1234").build();
        model.setStudent(student, numericIdStudent);
        storage.saveStudentBook(model.getStudentBook());
        saveCount = 0;
        model.updateFilteredStudentList(shown -> false);

        assertFailureWithoutMutation("tag 1234 -a T2", CommandException.class,
                Messages.getErrorMessageForInvalidIndex("1234", 0));
        logic.execute("tag -i 1234 -a T2");
        Student expected = new StudentBuilder(numericIdStudent).withTags("T1", "Lab 3", "T2").build();
        assertEquals(List.of(otherStudent, expected), storage.readStudentBook().orElseThrow().getStudentList());
        assertTrue(model.getFilteredStudentList().isEmpty());
        logic.execute("tag --id=1234 -r T2");
        assertEquals(List.of(otherStudent, numericIdStudent), storage.readStudentBook().orElseThrow().getStudentList());
    }

    @Test
    public void execute_studentIdFailures_preservesModelDisplayAndSavedBytes() throws Exception {
        assertFailureWithoutMutation("tag A0999999Z -a T2", CommandException.class,
                String.format(Messages.MESSAGE_NO_STUDENT_WITH_NUS_ID, "A0999999Z"));
        assertFailureWithoutMutation("tag --id A0999999Z -r T1", CommandException.class,
                String.format(Messages.MESSAGE_NO_STUDENT_WITH_NUS_ID, "A0999999Z"));
        assertFailureWithoutMutation("tag 1 --id A01234567X -a T2", ParseException.class,
                TagCommand.MESSAGE_MULTIPLE_TARGETS);
        assertFailureWithoutMutation("tag A01234567X -a t1", CommandException.class,
                "This student already has tag [T1]. No changes made.");
        model.updateFilteredStudentList(shown -> false);
        assertFailureWithoutMutation("tag A01234567X -r \"Needs follow-up\"", CommandException.class,
                "This student does not have tag [Needs follow-up]. No changes made.\n"
                        + String.format(TagCommand.MESSAGE_HIDDEN_FOLLOW_UP_FLAG_HINT, "A01234567X"));
    }

    @Test
    public void execute_hiddenNumericIdFollowUpHint_clearsFlagWithSuggestedCommand() throws Exception {
        Student numericIdStudent = new StudentBuilder(student).withNusStudentId("001234").build();
        model.setStudent(student, numericIdStudent);
        model.updateFilteredStudentList(shown -> false);
        storage.saveStudentBook(model.getStudentBook());
        saveCount = 0;
        assertFailureWithoutMutation("tag --id 001234 -r \"Needs follow-up\"", CommandException.class,
                "This student does not have tag [Needs follow-up]. No changes made.\n"
                        + "This student is flagged for follow-up, which is not a tag. To clear the flag, use "
                        + "edit --target-id 001234 --no-follow-up.");
        logic.execute("edit --target-id 001234 --no-follow-up");
        Student expected = new StudentBuilder(numericIdStudent).withFlag(false).build();
        assertEquals(List.of(otherStudent, expected), storage.readStudentBook().orElseThrow().getStudentList());
        assertTrue(model.getFilteredStudentList().isEmpty());
        assertEquals(1, saveCount);
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
    public void execute_minimalStudent_preservesAbsentFields() throws Exception {
        model.updateFilteredStudentList(shown -> shown.getName().equals(otherStudent.getName()));
        logic.execute("tag 1 -a \"T2\"");
        Student expected = new StudentBuilder(otherStudent).withTags("T2").build();
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
                "tag 1 -t T2", "There is no -t option.\n" + TagCommand.MESSAGE_ACTION_GUIDANCE,
                "tag 1 --tag T2", "There is no --tag option.\n" + TagCommand.MESSAGE_ACTION_GUIDANCE,
                "tag 0 -a T2", "Index must be a positive integer.",
                "tag 2147483648 -a T2", String.format(ParserUtil.MESSAGE_INDEX_TOO_LARGE, "2147483648"),
                "tag 1 -a \"\"", "Tag \"\" is not valid: it holds no letter or number",
                "tag 1 -a \"---\"", "Tag \"---\" is not valid: it holds no letter or number",
                "tag 1 -a T2 --add T3", "Options given more than once: -a. Give each listed option only once.",
                "tag 1 -a \"Needs follow-up\"", ParserUtil.MESSAGE_FOLLOW_UP_IS_NOT_A_TAG,
                "tag 1 -a \"  nEEds   follow-up \"", ParserUtil.MESSAGE_FOLLOW_UP_IS_NOT_A_TAG);
        for (Map.Entry<String, String> error : errors.entrySet()) {
            assertFailureWithoutMutation(error.getKey(), ParseException.class, error.getValue());
        }
        for (String index : List.of("2", "2147483647")) {
            assertFailureWithoutMutation("tag " + index + " -a T2", CommandException.class,
                    Messages.getErrorMessageForInvalidIndex(index, 1));
        }
        model.updateFilteredStudentList(shown -> false);
        assertFailureWithoutMutation("tag 1 -a T2", CommandException.class,
                Messages.getErrorMessageForInvalidIndex("1", 0));
    }

    @Test
    public void execute_negativeIndexOrMissingAction_preservesModelDisplayAndSavedBytes() throws Exception {
        String hint = "\n" + TagCommand.MESSAGE_ACTION_GUIDANCE;
        Map<String, String> errors = Map.of(
                "tag -1 -a T2", "Index must be a positive integer.\nThere is no -1 option." + hint,
                "tag -1 --add T2", "Index must be a positive integer.\nThere is no -1 option." + hint,
                "edit -1 -p 91234567", "Index must be a positive integer.\nThere is no -1 option.",
                "tag 1 lab4", "Unexpected value after INDEX: \"lab4\". Put field values after an option.\n"
                        + "Missing required field(s): -a TAG or -r TAG" + hint,
                "tag 1", "Missing required field(s): -a TAG or -r TAG" + hint,
                "edit 1 Alex -p 91234567",
                        "Unexpected value after INDEX: \"Alex\". Put field values after an option.");
        for (Map.Entry<String, String> error : errors.entrySet()) {
            assertFailureWithoutMutation(error.getKey(), ParseException.class, error.getValue());
        }
    }

    @Test
    public void execute_invalidEditIndex_reportsReasonWithoutMutation() throws Exception {
        for (String target : List.of("0", "1.5", "+1", "\"-1\"")) {
            assertFailureWithoutMutation("edit " + target + " -p 91234567", ParseException.class,
                    ParserUtil.MESSAGE_INVALID_INDEX);
        }
        assertFailureWithoutMutation("edit 0", ParseException.class,
                ParserUtil.MESSAGE_INVALID_INDEX + "\n" + EditCommand.MESSAGE_NOT_EDITED);
    }

    @Test
    public void execute_humanEditedLegacyTags_mergesVariantsBeforeTagging() throws Exception {
        // Loading merges legacy variants before the command handles the student.
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
        Student expected = new StudentBuilder(legacy).withTags("Lab 3", "Café", "T2").build();
        assertEquals(List.of(otherStudent, expected), storage.readStudentBook().orElseThrow().getStudentList());
    }

    @Test
    public void execute_removeTag_preservesEveryFieldAndPersistsAfterReload() throws Exception {
        model.updateFilteredStudentList(shown -> shown.getName().equals(student.getName()));
        Student expected = new StudentBuilder(student).withTags("T1").build();
        assertEquals("Removed tag [Lab 3]: Amy Bee; NUS Student ID: A01234567X; Phone: 85355255; "
                + "Email: amy@gmail.com; Tags: [T1]; Needs follow-up",
                logic.execute("tag 1 --remove \"  lAB\u00a0  3  \"").getFeedbackToUser());
        assertEquals(List.of(expected), model.getFilteredStudentList());
        assertEquals(List.of(otherStudent, expected), model.getStudentBook().getStudentList());
        Model restartedModel = new ModelManager(storage.readStudentBook().orElseThrow(), new UserPrefs());
        assertEquals(List.of(otherStudent, expected), restartedModel.getStudentBook().getStudentList());
        assertEquals(1, saveCount);
        assertEquals(new StudentBuilder().withTags("T1", "Lab 3").withFlag(true)
                .withNusStudentId("A01234567X").build(), student);
    }

    @Test
    public void execute_removeLastTag_preservesAbsentFieldsAndFollowUpStatus() throws Exception {
        Student minimal = new StudentBuilder(otherStudent).withoutNusStudentId()
                .withTags("Needs follow-up").build();
        model.setStudent(otherStudent, minimal);
        model.updateFilteredStudentList(shown -> shown.getName().equals(minimal.getName()));
        logic.execute("tag 1 -r \"needs follow-up\"");
        Student expected = new StudentBuilder(minimal).withTags().build();
        assertEquals(List.of(expected), model.getFilteredStudentList());
        assertEquals(List.of(expected, student), storage.readStudentBook().orElseThrow().getStudentList());
    }

    @Test
    public void execute_removeAbsentVariants_preservesModelDisplayAndSavedBytes() throws Exception {
        Map<String, String> variants = Map.of("Lab 4", "Lab 4", "lab 4", "lab 4",
                "  LAB\u00a0  4  ", "LAB 4");
        for (Map.Entry<String, String> variant : variants.entrySet()) {
            assertFailureWithoutMutation("tag 1 -r \"" + variant.getKey() + "\"", CommandException.class,
                    "This student does not have tag [" + variant.getValue() + "]. No changes made.");
        }
        model.updateFilteredStudentList(shown -> shown.getName().equals(otherStudent.getName()));
        assertFailureWithoutMutation("tag 1 --remove T1", CommandException.class,
                "This student does not have tag [T1]. No changes made.");
    }

    @Test
    public void execute_removeInvalidInput_preservesModelDisplayAndSavedBytes() throws Exception {
        String hint = "\n" + TagCommand.MESSAGE_ACTION_GUIDANCE;
        Map<String, String> errors = Map.of(
                "tag 0 -r T1", "Index must be a positive integer.",
                "tag -1 -r T1", "Index must be a positive integer.\nThere is no -1 option." + hint,
                "tag 2147483648 --remove T1", String.format(ParserUtil.MESSAGE_INDEX_TOO_LARGE, "2147483648"),
                "tag 1 -r \"\"", "Tag \"\" is not valid: it holds no letter or number",
                "tag 1 -r \"---\"", "Tag \"---\" is not valid: it holds no letter or number",
                "tag 1 -r", "-r TAG needs a value after it. "
                        + "A value opening with a hyphen goes in double quotes." + hint,
                "tag 1 -r T1 --remove T2", "Options given more than once: -r. Give each listed option only once.",
                "tag 1 -a T2 --remove T1", "Options -a/--add and -r/--remove cannot be used together.");
        for (Map.Entry<String, String> error : errors.entrySet()) {
            assertFailureWithoutMutation(error.getKey(), ParseException.class, error.getValue());
        }
        for (String index : List.of("2", "2147483647")) {
            assertFailureWithoutMutation("tag " + index + " -r T1", CommandException.class,
                    Messages.getErrorMessageForInvalidIndex(index, 1));
        }
        model.updateFilteredStudentList(shown -> false);
        assertFailureWithoutMutation("tag 1 -r T1", CommandException.class,
                Messages.getErrorMessageForInvalidIndex("1", 0));
    }

    @Test
    public void execute_removeHumanEditedVariants_removesMatchingGroupAndPreservesUnrelatedTags() throws Exception {
        model.updateFilteredStudentList(shown -> shown.getName().equals(student.getName()));
        Student legacy = new StudentBuilder(student).withTags("Lab 3", "lab 3", "Café").build();
        model.setStudent(student, legacy);
        storage.saveStudentBook(model.getStudentBook());
        String json = Files.readString(storage.getStudentBookFilePath()).replace("Lab 3", "  Lab   3  ");
        Files.writeString(storage.getStudentBookFilePath(), json);
        model.setStudentBook(storage.readStudentBook().orElseThrow());
        saveCount = 0;
        assertEquals("Removed tag [Lab 3]: Amy Bee; NUS Student ID: A01234567X; Phone: 85355255; "
                + "Email: amy@gmail.com; Tags: [Café]; Needs follow-up",
                logic.execute("tag 1 -r \"LAB 3\"").getFeedbackToUser());
        Student expected = new StudentBuilder(legacy).withTags("Café").build();
        assertEquals(List.of(expected), model.getFilteredStudentList());
        assertEquals(List.of(otherStudent, expected), storage.readStudentBook().orElseThrow().getStudentList());
        saveCount = 0;
        assertFailureWithoutMutation("tag 1 --remove \"  lab  3 \"", CommandException.class,
                "This student does not have tag [lab 3]. No changes made.");
        logic.execute("tag 1 --remove \" CAFE\u0301 \"");
        Student withoutTags = new StudentBuilder(legacy).withTags().build();
        assertEquals(List.of(otherStudent, withoutTags), storage.readStudentBook().orElseThrow().getStudentList());
    }

    @Test
    public void execute_removeFollowUpLabelFromFlaggedStudent_pointsToTheCommandThatClearsIt()
            throws Exception {
        // the card draws the label beside the name, so reaching for tag to take it off is a
        // fair guess, and saying only that the student has no such tag leaves it drawn there
        String hint = "\n" + String.format(TagCommand.MESSAGE_FOLLOW_UP_FLAG_HINT, 1);
        assertTrue(hint.endsWith("edit 1 --no-follow-up."), "the hint should name the command: " + hint);
        Map<String, String> spellings = Map.of("Needs follow-up", "Needs follow-up",
                "needs follow-up", "needs follow-up", "  NEEDS   follow-up ", "NEEDS follow-up");
        for (Map.Entry<String, String> spelling : spellings.entrySet()) {
            assertFailureWithoutMutation("tag 1 -r \"" + spelling.getKey() + "\"", CommandException.class,
                    "This student does not have tag [" + spelling.getValue() + "]. No changes made." + hint);
        }
    }

    @Test
    public void execute_removeFollowUpLabelFromUnflaggedStudent_saysOnlyThatTheTagIsAbsent()
            throws Exception {
        model.updateFilteredStudentList(shown -> shown.getName().equals(otherStudent.getName()));
        assertFailureWithoutMutation("tag 1 -r \"Needs follow-up\"", CommandException.class,
                "This student does not have tag [Needs follow-up]. No changes made.");
    }

    @Test
    public void execute_removeGeneralFollowUpTag_doesNotClearFollowUpFlag() throws Exception {
        Student tagged = new StudentBuilder(student).withTags("T1", "Needs follow-up").build();
        model.setStudent(student, tagged);
        model.updateFilteredStudentList(shown -> shown.getName().equals(student.getName()));
        logic.execute("tag 1 -r \"needs follow-up\"");
        Student expected = new StudentBuilder(student).withTags("T1").build();
        assertEquals(List.of(expected), model.getFilteredStudentList());
        assertEquals(List.of(otherStudent, expected), storage.readStudentBook().orElseThrow().getStudentList());
    }

    @Test
    public void execute_removeQuotedSymbolsAndLongTag_persistsEachRemoval() throws Exception {
        List<String> tags = List.of("组 1", "CS2103T (AY26/27)", "Lab \"A\"\\B", "-r", "x".repeat(1000));
        Student tagged = new StudentBuilder(student).withTags(tags.toArray(String[]::new)).build();
        model.setStudent(student, tagged);
        model.updateFilteredStudentList(shown -> shown.getName().equals(student.getName()));
        for (int i = 0; i < tags.size(); i++) {
            String quoted = tags.get(i).replace("\\", "\\\\").replace("\"", "\\\"");
            logic.execute("tag 1 -r \"" + quoted + "\"");
            Student expected = new StudentBuilder(student)
                    .withTags(tags.subList(i + 1, tags.size()).toArray(String[]::new)).build();
            assertEquals(List.of(expected), model.getFilteredStudentList());
            assertEquals(List.of(otherStudent, expected), storage.readStudentBook().orElseThrow().getStudentList());
        }
        assertEquals(tags.size(), saveCount);
        saveCount = 0;
        assertFailureWithoutMutation("tag 1 -r \"" + tags.getLast() + "\"", CommandException.class,
                "This student does not have tag [" + Messages.shorten(tags.getLast()) + "]. No changes made.");
    }

    @Test
    public void execute_removeTagUsedByFilter_keepsFilterActive() throws Exception {
        model.updateFilteredStudentList(shown -> shown.getTags().stream()
                .anyMatch(tag -> tag.tagName.equals("Lab 3")));
        logic.execute("tag 1 -r \"Lab 3\"");
        assertEquals(List.of(), model.getFilteredStudentList());
        Student expected = new StudentBuilder(student).withTags("T1").build();
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
