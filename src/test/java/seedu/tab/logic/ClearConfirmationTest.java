package seedu.tab.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.ReadOnlyStudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.StorageManager;
import seedu.tab.testutil.StudentBuilder;

public class ClearConfirmationTest {

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
        for (String reply : List.of("no", "NO", "No", " \tno\u00A0\u3000")) {
            assertEquals(String.format(ClearCommand.MESSAGE_CONFIRMATION, 2),
                    logic.execute("clear").getFeedbackToUser());
            assertUnchanged();
            assertEquals(ClearCommand.MESSAGE_CANCELLED, logic.execute(reply).getFeedbackToUser());
            assertUnchanged();
        }
        assertThrows(ParseException.class, Messages.MESSAGE_UNKNOWN_COMMAND, () -> logic.execute("yes"));
        assertUnchanged();
    }

    /** Verifies that no data or filter changed and no storage write occurred. */
    private void assertUnchanged() throws IOException {
        assertEquals(expectedModel, model);
        assertTrue(model.getFilteredStudentList().isEmpty());
        assertEquals(savedJson, Files.readString(storage.getStudentBookFilePath()));
        assertEquals(0, saveCount);
    }
}
