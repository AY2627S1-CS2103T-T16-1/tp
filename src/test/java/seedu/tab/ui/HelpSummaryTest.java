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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
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

    private static Node node(MainWindow window, String id) {
        return window.getRoot().getScene().lookup("#" + id);
    }

    private static void enter(MainWindow window, String text) {
        TextField field = fieldOf(window);
        field.setText(text);
        field.fireEvent(new ActionEvent());
    }

    private static TextField fieldOf(MainWindow window) {
        return (TextField) node(window, "commandTextField");
    }

    private static TextArea resultOf(MainWindow window) {
        return (TextArea) node(window, "resultDisplay");
    }

    private static void press(MainWindow window, KeyCode code) {
        window.getRoot().getScene().getFocusOwner().fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }

    private static void type(MainWindow window, String character) {
        window.getRoot().getScene().getFocusOwner().fireEvent(new KeyEvent(
                KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED, false, false, false, false));
    }

    @Test
    public void help_quitAndNextCommand_restoresStudentPageAndPreviousResult() throws Exception {
        FxToolkit.onFxThread(() -> {
            Model model = new ModelManager(TypicalStudents.getTypicalStudentBook(), new UserPrefs());
            StudentBook before = new StudentBook(model.getStudentBook());
            MainWindow window = createWindow(model);
            try {
                enter(window, "list");
                String previousResult = resultOf(window).getText();
                int windows = Window.getWindows().size();
                enter(window, "help\t ");
                assertEquals(HelpCommand.SHOWING_HELP_MESSAGE, ((Label) node(window, "helpText")).getText());
                assertEquals(windows, Window.getWindows().size());
                assertEquals(node(window, "helpPage"), window.getRoot().getScene().getFocusOwner());
                assertFalse(node(window, "studentPage").isVisible());
                assertFalse(node(window, "studentPage").isManaged());
                assertTrue(node(window, "helpPagePlaceholder").isManaged());
                type(window, "q");
                assertTrue(node(window, "studentPage").isVisible());
                assertFalse(node(window, "helpPagePlaceholder").isManaged());
                assertEquals(previousResult, resultOf(window).getText());
                assertEquals("", fieldOf(window).getText(), "quit must not type q into the command box");
                assertEquals(fieldOf(window), window.getRoot().getScene().getFocusOwner());
                enter(window, "list");
                assertEquals(ListCommand.formatSuccessMessage(before.getStudentList().size()),
                        resultOf(window).getText());
                assertEquals(before, model.getStudentBook());
            } finally {
                window.getRoot().hide();
            }
            return null;
        });
    }

    @Test
    public void help_menuAndF1_preserveDraftAndFilteredStudents() throws Exception {
        FxToolkit.onFxThread(() -> {
            Model model = new ModelManager(TypicalStudents.getTypicalStudentBook(), new UserPrefs());
            model.updateFilteredStudentList(student -> false);
            MainWindow window = createWindow(model);
            try {
                fieldOf(window).setText("find Alex");
                fieldOf(window).requestFocus();
                press(window, KeyCode.F1);
                assertTrue(node(window, "helpPagePlaceholder").isVisible());
                press(window, KeyCode.ESCAPE);
                assertEquals("find Alex", fieldOf(window).getText());
                assertTrue(model.getFilteredStudentList().isEmpty());
                MenuBar menu = (MenuBar) node(window, "menuBar");
                menu.getMenus().get(1).getItems().get(0).fire();
                type(window, "G");
                window.handleHelp();
                assertEquals(0, ((ScrollPane) node(window, "helpViewport")).getVvalue());
                type(window, "q");
                assertEquals("find Alex", fieldOf(window).getText());
            } finally {
                window.getRoot().hide();
            }
            return null;
        });
    }

    @Test
    public void help_returnToDraft_nextKeyContinuesEditing() throws Exception {
        FxToolkit.onFxThread(() -> {
            MainWindow window = createWindow(new ModelManager());
            try {
                for (boolean useMenu : List.of(false, true)) {
                    for (KeyCode quitKey : List.of(KeyCode.Q, KeyCode.ESCAPE)) {
                        for (KeyCode editKey : List.of(KeyCode.UNDEFINED, KeyCode.BACK_SPACE, KeyCode.DELETE)) {
                            restoreDraft(window, useMenu, quitKey, "add Wei Ming");
                            assertEquals(fieldOf(window).getLength(), fieldOf(window).getCaretPosition());
                            assertEquals(0, fieldOf(window).getSelection().getLength());
                            if (editKey == KeyCode.UNDEFINED) {
                                type(window, " Tan");
                                assertEquals("add Wei Ming Tan", fieldOf(window).getText());
                            } else {
                                press(window, editKey);
                                String expected = editKey == KeyCode.BACK_SPACE ? "add Wei Min" : "add Wei Ming";
                                assertEquals(expected, fieldOf(window).getText());
                            }
                        }
                        restoreDraft(window, useMenu, quitKey, "");
                        assertEquals(0, fieldOf(window).getCaretPosition());
                        assertEquals(0, fieldOf(window).getSelection().getLength());
                        type(window, "list");
                        assertEquals("list", fieldOf(window).getText());
                    }
                }
            } finally {
                window.getRoot().hide();
            }
            return null;
        });
    }

    private static void restoreDraft(MainWindow window, boolean useMenu, KeyCode quitKey, String draft) {
        fieldOf(window).setText(draft);
        fieldOf(window).requestFocus();
        if (useMenu) {
            MenuBar menu = (MenuBar) node(window, "menuBar");
            menu.getMenus().get(1).getItems().get(0).fire();
        } else {
            press(window, KeyCode.F1);
        }
        assertTrue(node(window, "helpPagePlaceholder").isVisible());
        if (quitKey == KeyCode.Q) {
            type(window, "q");
        } else {
            press(window, quitKey);
        }
        assertFalse(node(window, "helpPagePlaceholder").isVisible());
        assertEquals(fieldOf(window), window.getRoot().getScene().getFocusOwner());
        assertEquals(draft, fieldOf(window).getText());
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
                    assertFalse(node(window, "helpPagePlaceholder").isVisible());
                    assertFalse(Files.exists(temporaryFolder.resolve("students.json")));
                }
            } finally {
                window.getRoot().hide();
            }
            return null;
        });
    }

    @Test
    public void help_pagerKeysAtMinimumSize_reachBothEndsWithoutScrollbars() throws Exception {
        MainWindow window = FxToolkit.onFxThread(() -> {
            MainWindow result = createWindow(new ModelManager());
            result.getRoot().setWidth(result.getRoot().getMinWidth());
            result.getRoot().setHeight(result.getRoot().getMinHeight());
            enter(result, "help");
            return result;
        });
        try {
            FxToolkit.awaitAssertion(() -> {
                Region page = (Region) node(window, "helpPage");
                assertTrue(page.getWidth() <= window.getRoot().getMinWidth(),
                        "the page has not reached the minimum window width");
                assertTrue(page.getHeight() > window.getRoot().getScene().getHeight() * 0.8,
                        "the page has not filled the resized scene");
            });
            FxToolkit.onFxThread(() -> {
                assertPagerNavigation(window);
                return null;
            });
        } finally {
            FxToolkit.onFxThread(() -> {
                window.getRoot().hide();
                return null;
            });
        }
    }

    private static void assertPagerNavigation(MainWindow window) {
        ScrollPane viewport = (ScrollPane) node(window, "helpViewport");
        assertEquals(ScrollPane.ScrollBarPolicy.NEVER, viewport.getVbarPolicy());
        assertEquals(ScrollPane.ScrollBarPolicy.NEVER, viewport.getHbarPolicy());
        Region page = (Region) node(window, "helpPage");
        assertTrue(page.getHeight() > window.getRoot().getScene().getHeight() * 0.8);
        assertEquals(0, viewport.getVvalue());
        type(window, " ");
        assertTrue(viewport.getVvalue() > 0);
        type(window, "b");
        assertEquals(0, viewport.getVvalue(), 0.0001);
        type(window, "d");
        assertTrue(viewport.getVvalue() > 0);
        type(window, "u");
        assertEquals(0, viewport.getVvalue(), 0.0001);
        type(window, "j");
        assertTrue(viewport.getVvalue() > 0);
        type(window, "k");
        assertEquals(0, viewport.getVvalue(), 0.0001);
        press(window, KeyCode.PAGE_DOWN);
        assertTrue(viewport.getVvalue() > 0);
        press(window, KeyCode.PAGE_UP);
        assertEquals(0, viewport.getVvalue(), 0.0001);
        for (int pageNumber = 0; pageNumber < 20; pageNumber++) {
            type(window, " ");
        }
        assertEquals(1, viewport.getVvalue());
        assertEquals("(END)", ((Label) node(window, "helpProgress")).getText());
        type(window, "g");
        assertEquals(0, viewport.getVvalue());
        type(window, "G");
        assertEquals(1, viewport.getVvalue());
    }
}
