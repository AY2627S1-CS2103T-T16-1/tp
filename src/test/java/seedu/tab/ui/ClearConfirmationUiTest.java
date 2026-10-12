package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import seedu.tab.logic.LogicManager;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.StorageManager;
import seedu.tab.testutil.StudentBuilder;

public class ClearConfirmationUiTest {

    @TempDir
    public Path testFolder;

    private Model model = new ModelManager();
    private JsonStudentBookStorage storage;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    @Test
    public void commandLine_enterAction_promptsCancelsAndConfirms() throws Exception {
        FxToolkit.onFxThread(() -> {
            MainWindow window = createWindow();
            Stage stage = window.getPrimaryStage();
            try {
                TextField field = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea feedback = (TextArea) stage.getScene().lookup("#resultDisplay");
                field.requestFocus();
                enter(field, "clear");
                assertEquals("Clear all 1 student? This cannot be undone. [y/N]", feedback.getText());
                assertEquals(field, stage.getScene().getFocusOwner());
                assertEquals(1, model.getStudentBook().getStudentList().size());
                enter(field, "");
                assertEquals(ClearCommand.MESSAGE_CANCELLED, feedback.getText());
                assertEquals(1, storage.readStudentBook().orElseThrow().getStudentList().size());
                enter(field, "clear");
                enter(field, "Y");
                assertEquals(ClearCommand.MESSAGE_SUCCESS, feedback.getText());
                assertTrue(model.getFilteredStudentList().isEmpty());
                assertTrue(storage.readStudentBook().orElseThrow().getStudentList().isEmpty());
                assertEquals(field, stage.getScene().getFocusOwner());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    /** Builds a window with real logic and temporary storage for one student. */
    private MainWindow createWindow() throws IOException {
        model.addStudent(new StudentBuilder().build());
        Path dataFile = testFolder.resolve("tab.json");
        storage = new JsonStudentBookStorage(dataFile);
        storage.saveStudentBook(model.getStudentBook());
        LogicManager logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json"))));
        Stage stage = new Stage();
        MainWindow window = new MainWindow(stage, logic, dataFile);
        window.fillInnerParts();
        stage.show();
        return window;
    }

    /** Runs the same action the command field fires when Enter is pressed. */
    private static void enter(TextField field, String text) {
        field.setText(text);
        field.fireEvent(new ActionEvent());
    }
}
