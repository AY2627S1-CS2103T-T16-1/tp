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

public class KeyboardAndOfflineRequirementTest {

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
    public void keyboardOnly_allCoreTasksExecutableViaCommandLine_success() throws Exception {
        // Add student using keyboard command
        CommandResult addResult = logic.execute(
                AddCommand.COMMAND_WORD + " \"Amy Bee\" -p 85355255 -e amy@gmail.com");
        assertTrue(addResult.getFeedbackToUser().contains("Amy Bee"));
        assertEquals(1, model.getFilteredStudentList().size());

        // List students using keyboard command
        CommandResult listResult = logic.execute(ListCommand.COMMAND_WORD);
        assertEquals(ListCommand.MESSAGE_SUCCESS, listResult.getFeedbackToUser());

        // Find student using keyboard command
        CommandResult findResult = logic.execute(FindCommand.COMMAND_WORD + " Amy");
        assertEquals(1, model.getFilteredStudentList().size());
        assertEquals(String.format(Messages.MESSAGE_STUDENTS_LISTED_OVERVIEW, 1),
                findResult.getFeedbackToUser());

        // Edit student using keyboard command
        CommandResult editResult = logic.execute(
                EditCommand.COMMAND_WORD + " 1 p/91234567");
        assertTrue(editResult.getFeedbackToUser().contains("91234567"));

        // Delete student using keyboard command
        CommandResult deleteResult = logic.execute(DeleteCommand.COMMAND_WORD + " 1");
        assertTrue(deleteResult.getFeedbackToUser().contains("Deleted student: Amy Bee"));
        assertEquals(0, model.getFilteredStudentList().size());

        // Clear using keyboard command
        CommandResult clearResult = logic.execute(ClearCommand.COMMAND_WORD);
        assertEquals(ClearCommand.MESSAGE_SUCCESS, clearResult.getFeedbackToUser());

        // Help using keyboard command
        CommandResult helpResult = logic.execute(HelpCommand.COMMAND_WORD);
        assertTrue(helpResult.isShowHelp());

        // Exit using keyboard command
        CommandResult exitResult = logic.execute(ExitCommand.COMMAND_WORD);
        assertTrue(exitResult.isExit());
    }

    @Test
    public void keyboardOnly_exitCommandSignalsExitWithoutMouse_success() throws Exception {
        CommandResult result = logic.execute(ExitCommand.COMMAND_WORD);
        assertTrue(result.isExit());
        assertEquals(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, result.getFeedbackToUser());
    }

    @Test
    public void keyboardOnly_helpCommandSignalsShowHelpWithoutMouse_success() throws Exception {
        CommandResult result = logic.execute(HelpCommand.COMMAND_WORD);
        assertTrue(result.isShowHelp());
        assertEquals(HelpCommand.SHOWING_HELP_MESSAGE, result.getFeedbackToUser());
    }

    @Test
    public void offlineUsage_operationsExecuteLocallyWithoutNetwork_success() throws Exception {
        // Execute add locally
        logic.execute(AddCommand.COMMAND_WORD + " \"Bob Choo\" -p 88889999 -e bob@example.com");

        // Verify data was written to local file
        Path localDataFile = storage.getStudentBookFilePath();
        assertTrue(localDataFile.toFile().exists());

        // Read back from local storage
        ReadOnlyStudentBook readBook = storage.readStudentBook().get();
        assertEquals(1, readBook.getStudentList().size());
        assertEquals("Bob Choo", readBook.getStudentList().get(0).getName().fullName);

        // Execute delete locally
        logic.execute(DeleteCommand.COMMAND_WORD + " 1");

        // Verify local file reflects deletion immediately
        ReadOnlyStudentBook updatedBook = storage.readStudentBook().get();
        assertEquals(0, updatedBook.getStudentList().size());
        assertFalse(updatedBook.getStudentList().stream()
                .anyMatch(s -> s.getName().fullName.equals("Bob Choo")));
    }
}
