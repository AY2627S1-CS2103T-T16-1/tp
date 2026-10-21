package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.Messages.MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX;
import static seedu.tab.logic.commands.CommandTestUtil.EMAIL_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.NAME_AMY;
import static seedu.tab.logic.commands.CommandTestUtil.PHONE_AMY;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalStudents.AMY;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.ExitCommand;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.commands.HelpCommand;
import seedu.tab.logic.commands.ListCommand;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.FindCommandParser;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Student;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.StorageManager;
import seedu.tab.testutil.StudentBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;

    @BeforeEach
    public void setUp() {
        JsonStudentBookStorage studentBookStorage =
                new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        StorageManager storage = new StorageManager(studentBookStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, Messages.getErrorMessageForUnknownCommand(invalidCommand));
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.formatSuccessMessage(0), model);
    }

    @Test
    public void execute_findFormattedPhoneDigits_preservesRecordsAndStorage() throws Exception {
        logic.execute("add Alex Yeoh -p \"+65 9123 4567\"");
        logic.execute("add Bernice Yu -p 9876-5432");
        List<Student> students = List.copyOf(model.getStudentBook().getStudentList());
        JsonStudentBookStorage storage = new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json"));
        String savedJson = Files.readString(storage.getStudentBookFilePath());
        model = new ModelManager(storage.readStudentBook().orElseThrow(), new UserPrefs());
        setUp();

        for (String keyword : List.of("91234567", "91234", "6591234567", "\"9123 4567\"", "  91234567  ")) {
            assertEquals(new CommandResult(Messages.getStudentsFoundOverview(1)), logic.execute("find -p " + keyword));
            assertEquals(List.of(students.get(0)), model.getFilteredStudentList());
        }
        assertEquals(new CommandResult(Messages.getStudentsFoundOverview(1)), logic.execute("find -p 98765432"));
        assertEquals(List.of(students.get(1)), model.getFilteredStudentList());
        assertEquals(new CommandResult(Messages.getStudentsFoundOverview(0)),
                logic.execute("find -p 91234567 -n Bernice"));
        assertTrue(model.getFilteredStudentList().isEmpty());
        assertEquals(new CommandResult(Messages.getStudentsFoundOverview(0)), logic.execute("find -p 91234568"));
        assertTrue(model.getFilteredStudentList().isEmpty());
        assertEquals(students, model.getStudentBook().getStudentList());
        assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
        assertEquals(students, storage.readStudentBook().orElseThrow().getStudentList());
    }

    @Test
    public void execute_findFieldCombinations_preservesRecordsAndStorage() throws Exception {
        addSearchStudents();
        List<Student> students = List.copyOf(model.getStudentBook().getStudentList());
        JsonStudentBookStorage storage = new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json"));
        String savedJson = Files.readString(storage.getStudentBookFilePath());
        model = new ModelManager(storage.readStudentBook().orElseThrow(), new UserPrefs());
        setUp();
        for (var entry : Map.of(
                "find -n Jason -t lab -t tutorial", List.of(students.get(0), students.get(1)),
                "find --tag friend", List.of(students.get(0)),
                "find -t \"lab tutorial\"", List.of(students.get(0)),
                "find --id a0123 -p 9123-4567 -e NUS.EDU", List.of(students.get(0)),
                "find -n Jason -t lab -t tutorial -n Zhou", List.of(students.get(1)),
                "find -n Friend -t friend", List.<Student>of()).entrySet()) {
            assertEquals(new CommandResult(Messages.getStudentsFoundOverview(entry.getValue().size())),
                    logic.execute(entry.getKey()));
            assertEquals(entry.getValue(), model.getFilteredStudentList(), entry.getKey());
        }
        assertEquals(students, model.getStudentBook().getStudentList());
        assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
        assertEquals(students, storage.readStudentBook().orElseThrow().getStudentList());
    }

    @Test
    public void execute_invalidFindFields_preservesFilterRecordsAndStorage() throws Exception {
        addSearchStudents();
        logic.execute("find -t friend");
        List<Student> filtered = List.copyOf(model.getFilteredStudentList());
        List<Student> students = List.copyOf(model.getStudentBook().getStudentList());
        Path dataPath = temporaryFolder.resolve("studentBook.json");
        String savedJson = Files.readString(dataPath);
        for (var entry : Map.of(
                "find Jason -t lab", FindCommandParser.MESSAGE_UNQUALIFIED_TERM,
                "find Jason", FindCommandParser.MESSAGE_UNQUALIFIED_TERM,
                "find -t \"\"", FindCommandParser.MESSAGE_EMPTY_KEYWORD,
                "find --unknown friend", String.format(Messages.MESSAGE_UNKNOWN_FLAG, "--unknown"),
                "find -t lab tutorial", String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "tutorial"),
                "find -t", String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, "-t TAG")).entrySet()) {
            assertThrows(ParseException.class, entry.getValue(), () -> logic.execute(entry.getKey()));
            assertEquals(filtered, model.getFilteredStudentList());
            assertEquals(students, model.getStudentBook().getStudentList());
            assertEquals(savedJson, Files.readString(dataPath));
        }
    }

    @Test
    public void execute_findNames_distinguishesPhrasesFromRepeatedTermsAndKeepsApostrophes() throws Exception {
        logic.execute("add Jason Tan -p 91111111");
        logic.execute("add Jason Wei Tan -p 92222222");
        logic.execute("add O'Brien -p 93333333");
        List<Student> students = List.copyOf(model.getStudentBook().getStudentList());
        assertEquals(new CommandResult(Messages.getStudentsFoundOverview(2)), logic.execute("find -n Jason -n Tan"));
        assertEquals(students.subList(0, 2), model.getFilteredStudentList());
        assertEquals(new CommandResult(Messages.getStudentsFoundOverview(1)), logic.execute("find -n \"Jason Tan\""));
        assertEquals(List.of(students.get(0)), model.getFilteredStudentList());
        assertEquals(new CommandResult(Messages.getStudentsFoundOverview(1)), logic.execute("find -n O'Brien"));
        assertEquals(List.of(students.get(2)), model.getFilteredStudentList());
        assertEquals(new CommandResult(Messages.getStudentsFoundOverview(0)), logic.execute("find -n 'Jason'"));
        assertTrue(model.getFilteredStudentList().isEmpty());
    }

    private void addSearchStudents() throws Exception {
        logic.execute("add Jason Lim -p \"+65 9123 4567\" --id A01234567X -e jason@nus.edu"
                + " -t \"lab tutorial\" -t friend");
        logic.execute("add Jason Zhou -p 93333333 -t \"lab 5\" -t \"tutorial 3\"");
        logic.execute("add Jason Tan -p 94444444 -t lab");
        logic.execute("add Friend Lee -p 95555555 -e friend@example.com");
    }

    @Test
    public void execute_studentCountAgreesWithList_afterAddDeleteClear() throws Exception {
        // Initial empty state
        CommandResult result = logic.execute(ListCommand.COMMAND_WORD);
        assertEquals(ListCommand.formatSuccessMessage(0), result.getFeedbackToUser());
        assertEquals(0, model.getFilteredStudentList().size());

        // Add first student
        logic.execute(AddCommand.COMMAND_WORD + NAME_AMY + PHONE_AMY + EMAIL_AMY);
        result = logic.execute(ListCommand.COMMAND_WORD);
        assertEquals(ListCommand.formatSuccessMessage(1), result.getFeedbackToUser());
        assertEquals(1, model.getFilteredStudentList().size());

        // Add second student
        logic.execute(AddCommand.COMMAND_WORD + " \"Bob Choo\" -p 88888888 -e bob@example.com");
        result = logic.execute(ListCommand.COMMAND_WORD);
        assertEquals(ListCommand.formatSuccessMessage(2), result.getFeedbackToUser());
        assertEquals(2, model.getFilteredStudentList().size());

        // Find matches
        result = logic.execute(FindCommand.COMMAND_WORD + " -n Amy");
        assertEquals(Messages.getStudentsFoundOverview(1), result.getFeedbackToUser());
        assertEquals(1, model.getFilteredStudentList().size());

        result = logic.execute(FindCommand.COMMAND_WORD + " -n Nonexistent");
        assertEquals(Messages.getStudentsFoundOverview(0), result.getFeedbackToUser());
        assertEquals(0, model.getFilteredStudentList().size());

        // Delete first student
        logic.execute(ListCommand.COMMAND_WORD);
        logic.execute(DeleteCommand.COMMAND_WORD + " 1");
        result = logic.execute(ListCommand.COMMAND_WORD);
        assertEquals(ListCommand.formatSuccessMessage(1), result.getFeedbackToUser());
        assertEquals(1, model.getFilteredStudentList().size());

        // Clear all students
        logic.execute(ClearCommand.COMMAND_WORD);
        logic.execute("yes");
        result = logic.execute(ListCommand.COMMAND_WORD);
        assertEquals(ListCommand.formatSuccessMessage(0), result.getFeedbackToUser());
        assertEquals(0, model.getFilteredStudentList().size());
    }

    @Test
    public void execute_exitCommand_success() throws Exception {
        String exitCommand = ExitCommand.COMMAND_WORD;
        CommandResult result = logic.execute(exitCommand);
        assertEquals(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, result.getFeedbackToUser());
        assertTrue(result.isExit());
        assertFalse(result.isShowHelp());
        assertEquals(model, new ModelManager());
    }

    @Test
    public void execute_clearEmailAloneOrWithOtherEdits_persistsAndReloads() throws Exception {
        Student student = new StudentBuilder(AMY).withFlag(true).build();
        model.addStudent(student);
        Student cleared = new StudentBuilder(student).withoutEmail().build();
        JsonStudentBookStorage storage = new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json"));

        for (String command : new String[] {"edit 1 -e \"\"", "edit 1 -e \"\"",
            "edit 1 -e \"\" -p 81112222 -t \"\" --no-follow-up"}) {
            if (command.contains("-p")) {
                cleared = new StudentBuilder(cleared).withPhone("81112222").withTags().withFlag(false).build();
            }
            CommandResult result = logic.execute(command);
            assertEquals(String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(cleared)),
                    result.getFeedbackToUser());
            assertEquals(cleared, model.getFilteredStudentList().get(0));
            assertEquals(model.getStudentBook(), storage.readStudentBook().orElseThrow());
            assertFalse(Files.readString(storage.getStudentBookFilePath()).contains("\"email\""));
        }

        String savedJson = Files.readString(storage.getStudentBookFilePath());
        assertParseException("edit 1 -e", String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, "-e EMAIL"));
        assertParseException("edit 1 -e bob!yahoo",
                Messages.getErrorMessageForInvalidValue("Email", "bob!yahoo",
                        Email.getFailureReason("bob!yahoo")));
        assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
    }

    @Test
    public void execute_unknownOrIncompleteLongOption_preservesModelAndStorage() throws Exception {
        Student student = new StudentBuilder(AMY).build();
        model.addStudent(student);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json"));
        storage.saveStudentBook(model.getStudentBook());
        String savedJson = Files.readString(storage.getStudentBookFilePath());
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());

        assertCommandFailure("add Bob --phone", ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, "--phone PHONE"), expectedModel);
        assertCommandFailure("add Bob --unknown value", ParseException.class,
                String.format(Messages.MESSAGE_UNKNOWN_FLAG, "--unknown"), expectedModel);
        assertCommandFailure("edit 1 --phone", ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, "--phone PHONE"), expectedModel);
        assertCommandFailure("edit 1 --unknown value", ParseException.class,
                String.format(Messages.MESSAGE_UNKNOWN_FLAG, "--unknown"), expectedModel);

        assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
    }

    @Test
    public void execute_explicitFollowUpEdits_idempotentAndPersistAcrossReloads() throws Exception {
        Student original = new StudentBuilder(AMY).withNusStudentId("A01234567X").withFlag(false).build();
        model.addStudent(original);
        Student flagged = new StudentBuilder(original).withFlag(true).build();
        for (String command : List.of("edit 1 --follow-up", "edit 1 --follow-up", "edit 1 -f")) {
            assertFollowUpEditPersists(command, flagged);
        }
        flagged = new StudentBuilder(flagged).withPhone("81112222").build();
        assertFollowUpEditPersists("edit 1 -p 81112222", flagged);
        Student cleared = new StudentBuilder(flagged).withFlag(false).build();
        for (String command : List.of("edit 1 -nf", "edit 1 -nf", "edit 1 --no-follow-up")) {
            assertFollowUpEditPersists(command, cleared);
        }
        cleared = new StudentBuilder(cleared).withPhone("82223333").build();
        assertFollowUpEditPersists("edit 1 -p 82223333", cleared);
        assertFollowUpEditPersists("edit 1 --no-follow-up -p 82223333", cleared);
        assertFollowUpEditPersists("edit 1 --follow-up", new StudentBuilder(cleared).withFlag(true).build());
        assertFollowUpEditPersists("edit 1 --no-follow-up", cleared);
    }

    @Test
    public void execute_conflictingFollowUpEdits_preservesFilteredListAndStorage() throws Exception {
        JsonStudentBookStorage storage = new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json"));
        for (boolean isFlagged : new boolean[] {true, false}) {
            model = new ModelManager();
            setUp();
            model.addStudent(new StudentBuilder(AMY).withFlag(isFlagged).build());
            storage.saveStudentBook(model.getStudentBook());
            String savedJson = Files.readString(storage.getStudentBookFilePath());
            model.updateFilteredStudentList(student -> false);
            Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
            expectedModel.updateFilteredStudentList(student -> false);
            for (String options : List.of("--follow-up --no-follow-up", "--no-follow-up --follow-up",
                    "-f --no-follow-up", "--no-follow-up -f -p 81112222", "--follow-up -nf", "-nf --follow-up",
                    "-f -nf", "-nf -f -p 81112222")) {
                assertCommandFailure("edit 1 " + options, ParseException.class,
                        EditCommand.MESSAGE_CONFLICTING_FOLLOW_UP, expectedModel);
                assertTrue(model.getFilteredStudentList().isEmpty());
                assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
            }
        }
    }

    /**
     * Checks feedback, all student fields, saved data, and a fresh model loaded from that data.
     */
    private void assertFollowUpEditPersists(String command, Student expected) throws Exception {
        CommandResult result = logic.execute(command);
        assertEquals(String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(expected)),
                result.getFeedbackToUser());
        assertEquals(List.of(expected), model.getFilteredStudentList());
        JsonStudentBookStorage storage = new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json"));
        ReadOnlyStudentBook saved = storage.readStudentBook().orElseThrow();
        assertEquals(model.getStudentBook(), saved);
        model = new ModelManager(saved, new UserPrefs());
        setUp();
        assertEquals(List.of(expected), model.getFilteredStudentList());
    }

    @Test
    public void execute_deleteCommand_successAndPersistsToStorage() throws Exception {
        Student student = new StudentBuilder(AMY).build();
        model.addStudent(student);

        String deleteCommand = DeleteCommand.COMMAND_WORD + " 1";
        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS,
                Messages.format(student));
        ModelManager expectedModel = new ModelManager();

        assertCommandSuccess(deleteCommand, expectedMessage, expectedModel);

        ReadOnlyStudentBook savedStudentBook =
                new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json")).readStudentBook().get();
        assertEquals(0, savedStudentBook.getStudentList().size());
    }

    @Test
    public void execute_unexpectedArguments_preservesFilteredListAndStorage() throws Exception {
        model.addStudent(new StudentBuilder(AMY).build());
        model.updateFilteredStudentList(student -> false);
        JsonStudentBookStorage storage = new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json"));
        storage.saveStudentBook(model.getStudentBook());
        String savedJson = Files.readString(storage.getStudentBookFilePath());
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.updateFilteredStudentList(student -> false);

        for (String command : List.of("clear", "list", "help", "exit")) {
            for (String input : List.of("oops", "\"oops\"", "\"\"")) {
                assertCommandFailure(command + " " + input, ParseException.class,
                        String.format(Messages.MESSAGE_UNEXPECTED_ARGUMENTS, command, input), expectedModel);
                assertTrue(model.getFilteredStudentList().isEmpty());
                assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
            }
        }
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage()));
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, String.format(
                LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT, DUMMY_AD_EXCEPTION.getMessage()));
    }

    @Test
    public void execute_mutatingCommandsWhenStorageFails_rollsBackAndCanRetry() throws Exception {
        for (String command : List.of(
                "add \"New Student\" -p 81112222",
                "edit 1 -p 81112222",
                "delete 1",
                "tag 1 -a T2")) {
            model = new ModelManager();
            Student amy = new StudentBuilder(AMY).build();
            model.addStudent(amy);
            model.addStudent(new StudentBuilder().withName("Other Student").build());
            model.updateFilteredStudentList(amy::equals);
            Predicate<? super Student> expectedFilter = model.getFilteredStudentPredicate();
            Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
            expectedModel.updateFilteredStudentList(amy::equals);
            logic = createLogicWithFailingStorage(DUMMY_IO_EXCEPTION);
            String expectedMessage = String.format(
                    LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage());

            for (int attempt = 0; attempt < 2; attempt++) {
                assertCommandFailure(command, CommandException.class, expectedMessage, expectedModel);
                assertSame(expectedFilter, model.getFilteredStudentPredicate());
            }
        }
    }

    @Test
    public void execute_confirmedClearWhenStorageFails_rollsBackAndCanRetry() throws Exception {
        Student amy = new StudentBuilder(AMY).build();
        model.addStudent(amy);
        model.addStudent(new StudentBuilder().withName("Other Student").build());
        model.updateFilteredStudentList(amy::equals);
        Predicate<? super Student> expectedFilter = model.getFilteredStudentPredicate();
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.updateFilteredStudentList(amy::equals);
        logic = createLogicWithFailingStorage(DUMMY_IO_EXCEPTION);
        String expectedMessage = String.format(
                LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage());

        for (int attempt = 0; attempt < 2; attempt++) {
            assertEquals("Clear all 2 students? This cannot be undone. [y/N]",
                    logic.execute("clear").getFeedbackToUser());
            assertTrue(logic.isAwaitingConfirmation());
            assertCommandFailure("yes", CommandException.class, expectedMessage, expectedModel);
            assertFalse(logic.isAwaitingConfirmation());
            assertSame(expectedFilter, model.getFilteredStudentPredicate());
        }
    }

    @Test
    public void execute_retryAfterStorageRecovers_appliesChangeOnce() throws Exception {
        Student otherStudent = new StudentBuilder().withName("Other Student").build();
        model.addStudent(AMY);
        model.addStudent(otherStudent);
        Path studentBookPath = temporaryFolder.resolve("studentBook.json");
        AtomicInteger saveAttempts = new AtomicInteger();
        JsonStudentBookStorage studentBookStorage = new JsonStudentBookStorage(studentBookPath) {
            @Override
            public void saveStudentBook(ReadOnlyStudentBook studentBook) throws IOException {
                if (saveAttempts.incrementAndGet() == 1) {
                    throw DUMMY_IO_EXCEPTION;
                }
                super.saveStudentBook(studentBook);
            }
        };
        logic = new LogicManager(model, new StorageManager(studentBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());

        assertCommandFailure("delete 1", CommandException.class,
                String.format(LogicManager.FILE_OPS_ERROR_FORMAT, DUMMY_IO_EXCEPTION.getMessage()), expectedModel);
        CommandResult result = logic.execute("delete 1");

        assertEquals(String.format(DeleteCommand.MESSAGE_DELETE_STUDENT_SUCCESS, Messages.format(AMY)),
                result.getFeedbackToUser());
        assertEquals(List.of(otherStudent), model.getStudentBook().getStudentList());
        assertEquals(List.of(otherStudent), studentBookStorage.readStudentBook().orElseThrow().getStudentList());
        assertEquals(2, saveAttempts.get());
    }

    @Test
    public void execute_unwritableStorage_readOnlyCommandsSucceed() throws Exception {
        model.addStudent(AMY);
        setUpUnwritableStorage();

        assertReadOnlyCommandsSucceed();
        assertEquals(List.of(AMY), model.getStudentBook().getStudentList());
    }

    @Test
    public void execute_afterFailedSave_readOnlyCommandsSucceed() throws Exception {
        Student existingStudent = new StudentBuilder().withName("Existing Student").build();
        model.addStudent(existingStudent);
        setUpUnwritableStorage();
        String addCommand = AddCommand.COMMAND_WORD + NAME_AMY + PHONE_AMY + EMAIL_AMY;
        assertThrows(CommandException.class, String.format(LogicManager.FILE_OPS_PERMISSION_ERROR_FORMAT,
                DUMMY_AD_EXCEPTION.getMessage()), () -> logic.execute(addCommand));
        ReadOnlyStudentBook expectedBook = new StudentBook(model.getStudentBook());

        assertReadOnlyCommandsSucceed();
        assertEquals(expectedBook, model.getStudentBook());
        assertFalse(Files.exists(temporaryFolder.resolve("studentBook.json")));
    }

    @Test
    public void execute_unwritableStorage_unchangedEditSucceeds() throws Exception {
        model.addStudent(AMY);
        setUpUnwritableStorage();

        CommandResult result = logic.execute("edit 1 -p " + AMY.getPhone().value);

        assertEquals(String.format(EditCommand.MESSAGE_EDIT_STUDENT_SUCCESS, Messages.format(AMY)),
                result.getFeedbackToUser());
        assertEquals(List.of(AMY), model.getStudentBook().getStudentList());
    }

    private void setUpUnwritableStorage() {
        JsonStudentBookStorage studentBookStorage =
                new JsonStudentBookStorage(temporaryFolder.resolve("studentBook.json")) {
                    @Override
                    public void saveStudentBook(ReadOnlyStudentBook studentBook) throws IOException {
                        throw DUMMY_AD_EXCEPTION;
                    }
                };
        logic = new LogicManager(model, new StorageManager(studentBookStorage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"))));
    }

    private void assertReadOnlyCommandsSucceed() throws Exception {
        assertEquals(new CommandResult(Messages.getStudentsFoundOverview(0)), logic.execute("find -n Nobody"));
        assertTrue(model.getFilteredStudentList().isEmpty());
        assertEquals(new CommandResult(ListCommand.formatSuccessMessage(1)), logic.execute("list"));
        assertEquals(model.getStudentBook().getStudentList(), model.getFilteredStudentList());
        assertEquals(new CommandResult(HelpCommand.SHOWING_HELP_MESSAGE, true, false), logic.execute("help"));
        assertEquals(new CommandResult(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true), logic.execute("exit"));
    }

    @Test
    public void getFilteredStudentList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredStudentList().remove(0));
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        logic = createLogicWithFailingStorage(e);

        // Triggers the saveStudentBook method by executing an add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_AMY + PHONE_AMY
                + EMAIL_AMY;
        ModelManager expectedModel = new ModelManager();
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, expectedModel);
    }

    /** Returns a LogicManager whose student-book storage always throws {@code exception}. */
    private Logic createLogicWithFailingStorage(IOException exception) {
        Path studentBookPath = temporaryFolder.resolve("ExceptionStudentBook.json");
        JsonStudentBookStorage studentBookStorage = new JsonStudentBookStorage(studentBookPath) {
            @Override
            public void saveStudentBook(ReadOnlyStudentBook studentBook) throws IOException {
                throw exception;
            }
        };
        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        return new LogicManager(model, new StorageManager(studentBookStorage, userPrefsStorage));
    }
}
