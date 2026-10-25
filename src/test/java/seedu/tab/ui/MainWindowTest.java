package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import seedu.tab.commons.core.GuiSettings;
import seedu.tab.logic.Logic;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class MainWindowTest {

    /** A reply of the length a command reporting several bad fields gives. */
    private static final String FIVE_LINE_REPLY =
            "Siti Nur-Aisyah\nAlex Yeoh\nBernice Yu\nDavid Li\nRoy Balakrishnan";
    private static final long GEOMETRY_TIMEOUT_SECONDS = 10;
    private static final Path DATA_FILE = Paths.get("data", "tab.json");

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        return FxToolkit.onFxThread(work);
    }

    /** A {@code Logic} that holds one student and runs nothing. */
    private static class LogicStub implements Logic {
        private GuiSettings guiSettings = new GuiSettings();
        private final ObservableList<Student> students =
                FXCollections.observableArrayList(new StudentBuilder().build());

        @Override
        public CommandResult execute(String commandText) {
            return new CommandResult(
                    commandText.equals("long") ? FIVE_LINE_REPLY : commandText);
        }

        @Override
        public boolean isAwaitingConfirmation() {
            return false;
        }

        @Override
        public ObservableList<Student> getFilteredStudentList() {
            return students;
        }

        @Override
        public GuiSettings getGuiSettings() {
            return guiSettings;
        }

        @Override
        public void setGuiSettings(GuiSettings guiSettings) {
            // the window under test never saves its settings
        }
    }

    private static MainWindow filledWindow2() {
        MainWindow window = new MainWindow(new Stage(), new LogicStub(), DATA_FILE);
        window.fillInnerParts();
        return window;
    }

    private static MainWindow filledWindow() throws Exception {
        return onFxThread(() -> {
            MainWindow window = new MainWindow(new Stage(), new LogicStub(), DATA_FILE);
            window.fillInnerParts();
            return window;
        });
    }

    @Test
    public void show_offscreenOversizedPreferences_fitsPrimaryDisplay() throws Exception {
        MainWindow window = onFxThread(() -> {
            LogicStub logic = new LogicStub();
            logic.guiSettings = new GuiSettings(1.0E9, 1.0E9, 99999, 99999);
            MainWindow restored = new MainWindow(new Stage(), logic, DATA_FILE);
            restored.fillInnerParts();
            restored.show();
            return restored;
        });
        try {
            FxToolkit.awaitAssertion(() -> {
                Stage stage = window.getPrimaryStage();
                Rectangle2D display = Screen.getPrimary().getVisualBounds();
                Bounds content = stage.getScene().getRoot().localToScreen(
                        stage.getScene().getRoot().getBoundsInLocal());
                assertNotNull(content, "the restored scene must be visible on a screen");
                String geometry = "content=" + content + ", display=" + display
                        + ", stage=" + stage.getWidth() + "x" + stage.getHeight();
                // Window managers may include invisible frame shadows in the stage origin.
                assertTrue(content.getMinX() >= display.getMinX(), "left edge: " + geometry);
                assertTrue(content.getMinY() >= display.getMinY(), "top edge: " + geometry);
                assertTrue(content.getMaxX() <= display.getMaxX() + 1, "right edge: " + geometry);
                assertTrue(content.getMaxY() <= display.getMaxY() + 1, "bottom edge: " + geometry);
                assertTrue(stage.getWidth() <= display.getWidth() + 1, "window width: " + geometry);
                assertTrue(stage.getHeight() <= display.getHeight() + 1, "window height: " + geometry);
            }, GEOMETRY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            onFxThread(() -> {
                window.getPrimaryStage().hide();
                return null;
            });
        }
    }

    @Test
    public void resize_shortWindow_keepsStudentAndHelpFootersReachable() throws Exception {
        MainWindow window = onFxThread(MainWindowTest::compactWindow);
        try {
            FxToolkit.awaitAssertion(() -> assertCompactStudentView(window));
            onFxThread(() -> {
                window.handleHelp();
                Node help = window.getRoot().getScene().lookup("#helpPage");
                help.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.END,
                        false, false, false, false));
                return null;
            });
            FxToolkit.awaitAssertion(() -> assertCompactHelpView(window));
        } finally {
            onFxThread(() -> {
                window.getPrimaryStage().hide();
                return null;
            });
        }
    }

    private static MainWindow compactWindow() {
        LogicStub logic = new LogicStub();
        for (int i = 1; i < 10; i++) {
            logic.students.add(new StudentBuilder().withName("Student " + i).build());
        }
        MainWindow window = new MainWindow(new Stage(), logic, DATA_FILE);
        window.fillInnerParts();
        window.show();
        // Leave room for window decorations and desktop panels within 480 logical pixels.
        window.getPrimaryStage().setWidth(820);
        window.getPrimaryStage().setHeight(400);
        TextField field = (TextField) window.getRoot().getScene().lookup("#commandTextField");
        field.setText("Student added");
        field.fireEvent(new ActionEvent());
        return window;
    }

    private static void assertCompactStudentView(MainWindow window) {
        Stage stage = window.getPrimaryStage();
        assertTrue(stage.getHeight() <= 401, "the minimum height prevents resizing to fit");
        Region root = (Region) stage.getScene().getRoot();
        assertInside(root, root.lookup("#commandTextField"));
        assertInside(root, root.lookup("#resultDisplay"));
        assertInside(root, root.lookup("#saveLocationStatus"));
        assertInside(root, root.lookup("#total"));
        assertEquals("10 students", ((Label) root.lookup("#total")).getText());
        ListView<?> list = (ListView<?>) root.lookup("#studentListView");
        list.scrollTo(9);
        Optional<ListCell<?>> last = list.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof ListCell<?> cell && cell.getIndex() == 9)
                .<ListCell<?>>map(node -> (ListCell<?>) node).findFirst();
        assertTrue(last.isPresent(), "the last student is not visible after scrolling");
        assertInside(list, last.orElseThrow().getGraphic());
    }

    private static void assertCompactHelpView(MainWindow window) {
        Region root = (Region) window.getPrimaryStage().getScene().getRoot();
        Node help = root.lookup("#helpPage");
        assertInside(root, help);
        assertInside(root, root.lookup("#saveLocationStatus"));
        assertEquals("(END)", ((Label) help.lookup("#helpProgress")).getText());
        for (Node label : help.lookupAll(".label")) {
            if (!"helpText".equals(label.getId())) {
                assertInside(root, label);
            }
        }
    }

    private static void assertInside(Node container, Node child) {
        Bounds outer = container.localToScene(container.getLayoutBounds());
        Bounds inner = child.localToScene(child.getLayoutBounds());
        assertTrue(inner.getMinY() >= outer.getMinY() - 1, child + " extends above its container");
        assertTrue(inner.getMaxY() <= outer.getMaxY() + 1, child + " extends below its container");
        assertTrue(inner.getHeight() > 0, child + " has no visible height");
    }

    @Test
    public void help_f1WithMacTypedCharacter_preservesOpeningNotice() throws Exception {
        onFxThread(() -> {
            MainWindow window = filledWindow2();
            try {
                window.show();
                TextField field = (TextField) window.getRoot().getScene().lookup("#commandTextField");
                field.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1,
                        false, false, false, false));
                StackPane help = (StackPane) window.getRoot().getScene().lookup("#helpPagePlaceholder");
                assertTrue(help.isVisible());
                help.getChildren().get(0).fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, "", "",
                        KeyCode.UNDEFINED, false, false, false, false));
                Label notice = (Label) help.lookup("#helpNotice");
                assertEquals("Help mode: press q or Esc to return to commands.", notice.getText());
            } finally {
                window.getPrimaryStage().hide();
            }
            return null;
        });
    }

    @Test
    public void fillInnerParts_buildsEveryPane() throws Exception {
        MainWindow window = filledWindow();

        for (String id : new String[] {"#commandBoxPlaceholder", "#resultDisplayPlaceholder",
            "#studentListPanelPlaceholder", "#statusbarPlaceholder"}) {
            assertNotNull(window.getRoot().getScene().lookup(id), id + " is missing");
        }
    }

    @Test
    public void fillInnerParts_statusBarShowsAbsoluteDataFilePath() throws Exception {
        MainWindow window = filledWindow();

        Label saveLocation = (Label) window.getRoot().getScene().lookup("#saveLocationStatus");

        assertEquals(DATA_FILE.toAbsolutePath().normalize().toString(), saveLocation.getText());
    }

    @Test
    public void resultPane_beforeAnyCommand_takesNoSpace() throws Exception {
        MainWindow window = filledWindow();
        StackPane pane = (StackPane) window.getRoot().getScene().lookup("#resultDisplayPlaceholder");

        // the pane carries padding of its own, so it has to collapse with the area inside it
        // or the gap stays behind under the command line
        assertFalse(pane.isManaged());
        assertFalse(pane.isVisible());
    }

    @Test
    public void commandLine_emptyWithoutConfirmation_keepsResultHidden() throws Exception {
        MainWindow window = filledWindow();
        onFxThread(() -> {
            TextField field = (TextField) window.getRoot().getScene().lookup("#commandTextField");
            field.fireEvent(new ActionEvent());
            StackPane pane = (StackPane) window.getRoot().getScene().lookup("#resultDisplayPlaceholder");
            assertFalse(pane.isVisible());
            assertFalse(pane.isManaged());
            return null;
        });
    }

    @Test
    public void window_cannotBeMadeSmallerThanWhatIsInIt() throws Exception {
        double[] sizes = onFxThread(() -> {
            MainWindow window = filledWindow2();
            Stage stage = window.getPrimaryStage();
            Region root = (Region) stage.getScene().getRoot();
            root.applyCss();
            root.layout();
            return new double[] {stage.getMinWidth(), root.minWidth(-1),
                stage.getMinHeight(), root.minHeight(-1)};
        });

        // a window that can be made smaller than its content shows the content cut off at the
        // edge, which is worse than refusing to shrink
        assertTrue(sizes[0] >= sizes[1],
                "the window may shrink to " + sizes[0] + " but needs " + sizes[1]);
        assertTrue(sizes[2] >= sizes[3],
                "the window may shrink to " + sizes[2] + " but needs " + sizes[3]);
    }

    @Test
    public void resultPane_aLongReplyInAShortWindow_keepsTheRoomItAsksFor() throws Exception {
        double[] measured = onFxThread(() -> {
            MainWindow window = filledWindow2();
            Region root = (Region) window.getPrimaryStage().getScene().getRoot();
            // short enough that the panes together ask for more than there is
            root.resize(740, 320);
            root.applyCss();
            root.layout();

            TextField field = (TextField) root.lookup("#commandTextField");
            field.setText("long");
            field.fireEvent(new ActionEvent());
            // the area counts its rows again once it has drawn the reply, which asks for
            // another pass, as a pulse would give it
            root.layout();
            root.layout();

            Region pane = (Region) root.lookup("#resultDisplayPlaceholder");
            Region area = (Region) root.lookup("#resultDisplay");
            Region list = (Region) root.lookup("#studentList");
            return new double[] {area.getHeight(), area.prefHeight(-1),
                list.getHeight(), list.prefHeight(-1), pane.getHeight()};
        });

        // the one that gives way must be the list, which scrolls what it cannot show, rather
        // than the reply, which is simply cut off
        assertTrue(measured[4] > 0, "the pane holding the reply should be laid out");
        assertTrue(measured[0] >= measured[1] - 1,
                "the reply was squeezed to " + measured[0] + " of " + measured[1]);
        assertTrue(measured[2] <= measured[3],
                "the list should be the pane that gives way");
    }
}
