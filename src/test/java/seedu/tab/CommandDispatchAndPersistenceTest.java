package seedu.tab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.logic.Logic;
import seedu.tab.logic.LogicManager;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.ExitCommand;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.commands.HelpCommand;
import seedu.tab.logic.commands.ListCommand;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.StorageManager;

/**
 * Integration tests for command dispatch and saving changes between sessions.
 */
public class CommandDispatchAndPersistenceTest {

    @TempDir
    public Path testFolder;

    private Model model;
    private StorageManager storage;
    private Logic logic;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        JsonStudentBookStorage studentBookStorage =
                new JsonStudentBookStorage(testFolder.resolve("studentBook.json"));
        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(testFolder.resolve("userPrefs.json"));
        storage = new StorageManager(studentBookStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_coreCommands_dispatchesAndUpdatesState() throws Exception {
        // Add student
        CommandResult addResult = logic.execute(
                AddCommand.COMMAND_WORD + " \"Amy Bee\" -p 85355255 -e amy@gmail.com");
        assertTrue(addResult.getFeedbackToUser().contains("Amy Bee"));
        assertEquals(1, model.getFilteredStudentList().size());

        // List students
        CommandResult listResult = logic.execute(ListCommand.COMMAND_WORD);
        assertEquals(ListCommand.formatSuccessMessage(1), listResult.getFeedbackToUser());

        // Find student
        CommandResult findResult = logic.execute(FindCommand.COMMAND_WORD + " -n Amy");
        assertEquals(1, model.getFilteredStudentList().size());
        assertEquals(Messages.getStudentsFoundOverview(1),
                findResult.getFeedbackToUser());

        // Edit student
        CommandResult editResult = logic.execute(
                EditCommand.COMMAND_WORD + " 1 -p 91234567");
        assertTrue(editResult.getFeedbackToUser().contains("91234567"));

        // Delete student
        CommandResult deleteResult = logic.execute(DeleteCommand.COMMAND_WORD + " 1");
        assertTrue(deleteResult.getFeedbackToUser().contains("Deleted student: Amy Bee"));
        assertEquals(0, model.getFilteredStudentList().size());

        // Clear
        CommandResult clearResult = logic.execute(ClearCommand.COMMAND_WORD);
        assertEquals(ClearCommand.MESSAGE_EMPTY, clearResult.getFeedbackToUser());

        logic.execute("add Amy Bee -p 85355255");
        String prompt = "Clear all 1 student? This cannot be undone. [y/N]";
        assertEquals(prompt, logic.execute("clear").getFeedbackToUser());
        assertEquals(ClearCommand.MESSAGE_CANCELLED, logic.execute("no").getFeedbackToUser());
        assertEquals(1, model.getFilteredStudentList().size());
        assertEquals(1, storage.readStudentBook().orElseThrow().getStudentList().size());
        assertEquals(prompt, logic.execute("clear").getFeedbackToUser());
        assertEquals(ClearCommand.MESSAGE_SUCCESS, logic.execute("yes").getFeedbackToUser());
        assertTrue(model.getStudentBook().getStudentList().isEmpty());
        assertTrue(storage.readStudentBook().orElseThrow().getStudentList().isEmpty());

        // Help
        CommandResult helpResult = logic.execute(HelpCommand.COMMAND_WORD);
        assertTrue(helpResult.isHelpRequested());

        // Exit
        CommandResult exitResult = logic.execute(ExitCommand.COMMAND_WORD);
        assertTrue(exitResult.isExitRequested());
    }

    @Test
    public void execute_addPastedNameHoldingInvisibleMarks_storedAlikeAndWarnsOfDuplicate() throws Exception {
        logic.execute(AddCommand.COMMAND_WORD + " John Doe -p 98765432");

        // the mark a chat app pasted before the name draws nothing, so both records
        // read John Doe and the warning can name the first one
        String pastedAdd = AddCommand.COMMAND_WORD + " \u200EJohn Doe -p 98765432";
        CommandResult result = logic.execute(pastedAdd);

        assertTrue(result.getFeedbackToUser().contains(
                "WARNING: Possible duplicate student: John Doe has the same name."));

        assertEquals(2, model.getFilteredStudentList().size());
        assertEquals("John Doe", model.getFilteredStudentList().get(0).getName().getFullName());
        assertEquals("John Doe", model.getFilteredStudentList().get(1).getName().getFullName());

        ReadOnlyStudentBook saved = storage.readStudentBook().orElseThrow();
        assertEquals(2, saved.getStudentList().size());
        assertEquals("John Doe", saved.getStudentList().get(1).getName().getFullName());
    }

    @Test
    public void execute_addHyphenatedName_warnsAndKeepsBothSpellings() throws Exception {
        logic.execute(AddCommand.COMMAND_WORD + " \"Mary Jane\" -p 98765432");

        CommandResult result = logic.execute(AddCommand.COMMAND_WORD + " Mary-Jane -p 87654321");

        assertTrue(result.getFeedbackToUser().contains(
                "WARNING: Possible duplicate student: Mary Jane has the same name."));

        assertEquals("Mary Jane", model.getFilteredStudentList().get(0).getName().getFullName());
        assertEquals("Mary-Jane", model.getFilteredStudentList().get(1).getName().getFullName());

        ReadOnlyStudentBook saved = storage.readStudentBook().orElseThrow();
        assertEquals("Mary-Jane", saved.getStudentList().get(1).getName().getFullName());

        logic.execute(FindCommand.COMMAND_WORD + " -n \"mary jane\"");
        assertEquals(2, model.getFilteredStudentList().size());
    }

    @Test
    public void execute_exitCommand_returnsExitSignal() throws Exception {
        CommandResult result = logic.execute(ExitCommand.COMMAND_WORD);
        assertTrue(result.isExitRequested());
        assertEquals(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, result.getFeedbackToUser());
    }

    @Test
    public void execute_helpCommand_returnsShowHelpSignal() throws Exception {
        CommandResult result = logic.execute(HelpCommand.COMMAND_WORD);
        assertTrue(result.isHelpRequested());
        assertEquals(HelpCommand.SHOWING_HELP_MESSAGE, result.getFeedbackToUser());
    }

    @Test
    public void execute_addThenDelete_persistsBothChanges() throws Exception {
        logic.execute(AddCommand.COMMAND_WORD + " \"Bob Choo\" -p 88889999 -e bob@example.com");

        Path dataFile = storage.getStudentBookFilePath();
        assertTrue(dataFile.toFile().exists());

        ReadOnlyStudentBook readBook = storage.readStudentBook().get();
        assertEquals(1, readBook.getStudentList().size());
        assertEquals("Bob Choo", readBook.getStudentList().get(0).getName().getFullName());

        logic.execute(DeleteCommand.COMMAND_WORD + " 1");

        ReadOnlyStudentBook updatedBook = storage.readStudentBook().get();
        assertEquals(0, updatedBook.getStudentList().size());
        assertFalse(updatedBook.getStudentList().stream()
                .anyMatch(s -> s.getName().getFullName().equals("Bob Choo")));
    }
}
