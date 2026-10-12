package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.StorageManager;
import seedu.tab.testutil.StudentBuilder;

public class ClearConfirmationTest {

    private static final int LONG_REPLY_LENGTH = 100_000;
    private static final Duration LONG_REPLY_TIMEOUT = Duration.ofSeconds(5);

    @TempDir
    public Path testFolder;

    private Model model;
    private Logic logic;
    private JsonStudentBookStorage storage;
    private Model expectedModel;
    private String savedJson;
    private int saveCount;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        model.addStudent(new StudentBuilder().withFlag(true).withTags("Lab 3", "T1").build());
        model.addStudent(new StudentBuilder().withName("Other Student").withoutEmail().build());
        model.updateFilteredStudentList(student -> false);
        expectedModel = new ModelManager(model.getStudentBook(), new UserPrefs());
        expectedModel.updateFilteredStudentList(student -> false);
        storage = new JsonStudentBookStorage(testFolder.resolve("tab.json")) {
            @Override
            public void saveStudentBook(ReadOnlyStudentBook studentBook) throws IOException {
                saveCount++;
                super.saveStudentBook(studentBook);
            }
        };
        storage.saveStudentBook(model.getStudentBook());
        saveCount = 0;
        savedJson = Files.readString(storage.getStudentBookFilePath());
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
    }

    @Test
    public void execute_clearAndCancel_preservesDisplayAndJsonBytes() throws Exception {
        for (String reply : List.of("", " ", "\t\n", "\u00A0\u3000", "n", "N", " \tn\u00A0\u3000",
                "no", "nO", "No", "NO", " \tno\u00A0\u3000")) {
            assertEquals("Clear all 2 students? This cannot be undone. [y/N]",
                    logic.execute("clear").getFeedbackToUser());
            assertTrue(logic.isAwaitingConfirmation());
            assertUnchanged();
            assertEquals(ClearCommand.MESSAGE_CANCELLED, logic.execute(reply).getFeedbackToUser());
            assertFalse(logic.isAwaitingConfirmation());
            assertUnchanged();
        }
        assertThrows(ParseException.class, Messages.MESSAGE_UNKNOWN_COMMAND, () -> logic.execute("yes"));
        assertUnchanged();
    }

    @Test
    public void execute_invalidReplies_keepsConfirmationPendingWithoutWriting() throws Exception {
        logic.execute("clear");
        for (String reply : List.of("yellow", "YESPLEASE", "yep", "yeah", "yess", "yes no", "\"yes\"", "\"y\"",
                "yes!", "nope", "never", "nothing", "nonsense", "no!", "n o", "n\u200Bo", "y\u200Bes",
                "maybe", "clear", "clear oops", "exit", "help", "delete 1", "add New Student -p 81112222")) {
            String expected = ClearCommand.MESSAGE_INVALID_REPLY + "\n"
                    + "Clear all 2 students? This cannot be undone. [y/N]";
            assertThrows(ParseException.class, expected, () -> logic.execute(reply));
            assertTrue(logic.isAwaitingConfirmation());
            assertUnchanged();
        }
        assertEquals(ClearCommand.MESSAGE_CANCELLED, logic.execute("no").getFeedbackToUser());
        assertUnchanged();
        assertTrue(logic.execute("help").isShowHelp());
    }

    @Test
    public void execute_longInvalidReply_rejectsPromptlyWithoutWriting() throws Exception {
        logic.execute("clear");
        String expected = ClearCommand.MESSAGE_INVALID_REPLY + "\n"
                + "Clear all 2 students? This cannot be undone. [y/N]";
        String reply = "x" + " ".repeat(LONG_REPLY_LENGTH) + "x";
        assertTimeoutPreemptively(LONG_REPLY_TIMEOUT, () ->
                assertThrows(ParseException.class, expected, () -> logic.execute(reply)));
        assertTrue(logic.isAwaitingConfirmation());
        assertUnchanged();
    }

    @Test
    public void execute_longWhitespaceReply_cancelsPromptlyWithoutWriting() throws Exception {
        logic.execute("clear");
        String reply = "\u00A0\u3000\u0085".repeat(LONG_REPLY_LENGTH);
        assertTimeoutPreemptively(LONG_REPLY_TIMEOUT, () ->
                assertEquals(ClearCommand.MESSAGE_CANCELLED, logic.execute(reply).getFeedbackToUser()));
        assertFalse(logic.isAwaitingConfirmation());
        assertUnchanged();
    }

    @Test
    public void execute_longSurroundingWhitespace_confirmsAndPersistsOnce() throws Exception {
        logic.execute("clear");
        String whitespace = " \u00A0\u0085".repeat(LONG_REPLY_LENGTH);
        assertTimeoutPreemptively(LONG_REPLY_TIMEOUT, () ->
                assertEquals(ClearCommand.MESSAGE_SUCCESS,
                        logic.execute(whitespace + "YeS" + whitespace).getFeedbackToUser()));
        assertFalse(logic.isAwaitingConfirmation());
        assertEquals(new StudentBook(), model.getStudentBook());
        assertEquals(model.getStudentBook(), storage.readStudentBook().orElseThrow());
        assertEquals(1, saveCount);
    }

    @Test
    public void execute_yesReplyVariants_clearsHiddenRecordsAndPersistsOnce() throws Exception {
        for (String reply : List.of("y", "Y", " \tY\u00A0\u3000", "yes", "yeS", "yEs", "yES",
                "Yes", "YeS", "YEs", "YES", " \tyes\u00A0\u3000")) {
            setUp();
            logic.execute("clear");
            assertUnchanged();
            assertEquals(ClearCommand.MESSAGE_SUCCESS, logic.execute(reply).getFeedbackToUser());
            assertFalse(logic.isAwaitingConfirmation());
            assertEquals(new StudentBook(), model.getStudentBook());
            assertTrue(model.getFilteredStudentList().isEmpty());
            assertEquals(model.getStudentBook(), storage.readStudentBook().orElseThrow());
            assertEquals(1, saveCount);
            assertThrows(ParseException.class, Messages.MESSAGE_UNKNOWN_COMMAND, () -> logic.execute("yes"));
            assertEquals(1, saveCount);
        }
    }

    @Test
    public void execute_confirmationWithoutRequest_reportsCommandErrorWithoutMutation() throws Exception {
        // Simulates a confirmation indicator that disagrees with the stored request.
        logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json")))) {
            @Override
            public boolean isAwaitingConfirmation() {
                return true;
            }
        };
        for (String reply : List.of("yes", "", "no", "maybe")) {
            assertThrows(CommandException.class, ClearCommand.MESSAGE_NO_PENDING_CONFIRMATION, () ->
                    logic.execute(reply));
            assertUnchanged();
        }
    }

    @Test
    public void execute_emptyBook_doesNotPromptOrWrite() throws Exception {
        model.setStudentBook(new StudentBook());
        storage.saveStudentBook(model.getStudentBook());
        saveCount = 0;
        String emptyJson = Files.readString(storage.getStudentBookFilePath());
        assertEquals(ClearCommand.MESSAGE_EMPTY, logic.execute("clear").getFeedbackToUser());
        assertEquals(emptyJson, Files.readString(storage.getStudentBookFilePath()));
        assertEquals(0, saveCount);
        assertThrows(ParseException.class, Messages.MESSAGE_UNKNOWN_COMMAND, () -> logic.execute("yes"));
        Files.delete(storage.getStudentBookFilePath());
        assertEquals(ClearCommand.MESSAGE_EMPTY, logic.execute("clear").getFeedbackToUser());
        assertFalse(Files.exists(storage.getStudentBookFilePath()));
        assertEquals(0, saveCount);
    }

    /** Verifies that no data or filter changed and no storage write occurred. */
    private void assertUnchanged() throws IOException {
        assertEquals(expectedModel, model);
        assertTrue(model.getFilteredStudentList().isEmpty());
        assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
        assertEquals(0, saveCount);
    }
}
