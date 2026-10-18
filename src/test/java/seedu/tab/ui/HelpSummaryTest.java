package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.tab.logic.LogicManager;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.HelpCommand;
import seedu.tab.logic.commands.ListCommand;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.StudentBook;
import seedu.tab.model.UserPrefs;
import seedu.tab.storage.JsonStudentBookStorage;
import seedu.tab.storage.JsonUserPrefsStorage;
import seedu.tab.storage.StorageManager;
import seedu.tab.testutil.TypicalStudents;

public class HelpSummaryTest {
    @TempDir
    public Path temporaryFolder;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    private MainWindow createWindow(Model model) {
        Path dataFile = temporaryFolder.resolve("students.json");
        StorageManager storage = new StorageManager(new JsonStudentBookStorage(dataFile),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")));
        MainWindow window = new MainWindow(new Stage(), new LogicManager(model, storage), dataFile);
        window.fillInnerParts();
        window.show();
        window.getRoot().getScene().getRoot().applyCss();
        window.getRoot().getScene().getRoot().layout();
        return window;
    }

    private static void enter(MainWindow window, String text) {
        TextField field = fieldOf(window);
        field.setText(text);
        field.fireEvent(new ActionEvent());
    }

    private static TextField fieldOf(MainWindow window) {
        return (TextField) window.getRoot().getScene().lookup("#commandTextField");
    }

    private static TextArea resultOf(MainWindow window) {
        return (TextArea) window.getRoot().getScene().lookup("#resultDisplay");
    }

    private static void press(MainWindow window, KeyCode code) {
        window.getRoot().getScene().getFocusOwner().fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }

    @Test
    public void help_keyboardNavigationAndNextCommand_staysInMainWindow() throws Exception {
        FxToolkit.onFxThread(() -> {
            Model model = new ModelManager(TypicalStudents.getTypicalStudentBook(), new UserPrefs());
            StudentBook before = new StudentBook(model.getStudentBook());
            MainWindow window = createWindow(model);
            try {
                int windows = Window.getWindows().size();
                enter(window, "help\t ");
                TextArea result = resultOf(window);
                assertEquals(HelpCommand.SHOWING_HELP_MESSAGE, result.getText());
                assertEquals(windows, Window.getWindows().size());
                assertEquals(fieldOf(window), window.getRoot().getScene().getFocusOwner());
                assertFalse(result.isEditable());
                press(window, KeyCode.F6);
                assertEquals(result, window.getRoot().getScene().getFocusOwner());
                result.end();
                assertEquals(result.getLength(), result.getCaretPosition());
                press(window, KeyCode.ESCAPE);
                assertEquals(fieldOf(window), window.getRoot().getScene().getFocusOwner());
                enter(window, "list");
                assertEquals(ListCommand.formatSuccessMessage(before.getStudentList().size()), result.getText());
                assertEquals(before, model.getStudentBook());
            } finally {
                window.getRoot().hide();
            }
            return null;
        });
    }

    @Test
    public void help_menuAndF1_preserveDraftAndResetReadingPosition() throws Exception {
        FxToolkit.onFxThread(() -> {
            MainWindow window = createWindow(new ModelManager());
            try {
                fieldOf(window).setText("find Alex");
                fieldOf(window).requestFocus();
                press(window, KeyCode.F1);
                assertEquals(HelpCommand.SHOWING_HELP_MESSAGE, resultOf(window).getText());
                resultOf(window).end();
                MenuBar menu = (MenuBar) window.getRoot().getScene().lookup("#menuBar");
                menu.getMenus().get(1).getItems().get(0).fire();
                assertEquals(0, resultOf(window).getCaretPosition());
                assertEquals("find Alex", fieldOf(window).getText());
                assertEquals(fieldOf(window), window.getRoot().getScene().getFocusOwner());
            } finally {
                window.getRoot().hide();
            }
            return null;
        });
    }

    @Test
    public void help_unexpectedArguments_reportsExactErrorWithoutSavingOrChangingData() throws Exception {
        FxToolkit.onFxThread(() -> {
            Model model = new ModelManager(TypicalStudents.getTypicalStudentBook(), new UserPrefs());
            StudentBook before = new StudentBook(model.getStudentBook());
            model.updateFilteredStudentList(student -> false);
            MainWindow window = createWindow(model);
            try {
                for (String argument : List.of("oops", "-p", "\"\"", "\"unfinished", "O’Neil")) {
                    enter(window, "help " + argument);
                    assertEquals(Messages.getErrorMessageForUnexpectedArguments("help", argument),
                            resultOf(window).getText());
                    assertEquals("help " + argument, fieldOf(window).getText());
                    assertEquals(before, model.getStudentBook());
                    assertTrue(model.getFilteredStudentList().isEmpty());
                    assertFalse(Files.exists(temporaryFolder.resolve("students.json")));
                }
            } finally {
                window.getRoot().hide();
            }
            return null;
        });
    }

    @Test
    public void help_keyboardEnd_reachesLastLineInMinimumWindow() throws Exception {
        MainWindow window = FxToolkit.onFxThread(() -> {
            MainWindow result = createWindow(new ModelManager());
            result.getRoot().setWidth(result.getRoot().getMinWidth());
            result.getRoot().setHeight(result.getRoot().getMinHeight());
            enter(result, "help");
            return result;
        });
        try {
            FxToolkit.afterPulse(() -> {
                press(window, KeyCode.F6);
                boolean isMac = System.getProperty("os.name").startsWith("Mac");
                resultOf(window).fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.END,
                        false, !isMac, false, isMac));
                return null;
            });
            FxToolkit.afterPulse(() -> {
                TextArea result = resultOf(window);
                assertEquals(result.getLength(), result.getCaretPosition());
                assertTrue(result.getScrollTop() > 0, "the last line must be scrolled into view");
                return null;
            });
        } finally {
            FxToolkit.onFxThread(() -> {
                window.getRoot().hide();
                return null;
            });
        }
    }
}
