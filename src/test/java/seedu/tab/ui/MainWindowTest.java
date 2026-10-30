package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.ui.FxToolkit.onFxThread;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import seedu.tab.commons.core.GuiSettings;
import seedu.tab.logic.Logic;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.logic.commands.ExitCommand;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class MainWindowTest {

    /** A reply of the length a command reporting several bad fields gives. */
    private static final String FIVE_LINE_REPLY =
            "Siti Nur-Aisyah\nAlex Yeoh\nBernice Yu\nDavid Li\nRoy Balakrishnan";
    private static final long GEOMETRY_TIMEOUT_SECONDS = 10;

    /** The height the window may not shrink below, as {@code MainWindow.fxml} sets it. */
    private static final double MINIMUM_WINDOW_HEIGHT = 400;

    /**
     * The slack a geometry assertion allows, counted in device pixels rather than logical ones.
     * A window's position and its size each snap to the device-pixel grid, and a usable area
     * reduced by desktop panels need not be a whole number of device pixels: on a 1920x1200
     * display at 125% the usable width of 1470 logical pixels is 1837.5 device pixels, so the
     * toolkit rounds the window up to the next whole one and the scene's own snapping adds
     * another. Three covers the position, the size and the scene, and stays far below any real
     * failure.
     */
    private static final int SNAP_ALLOWANCE_DEVICE_PIXELS = 3;
    private static final Path DATA_FILE = Paths.get("data", "tab.json");

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }


    /** A {@code Logic} that holds one student and runs nothing. */
    private static class LogicStub implements Logic {
        private GuiSettings guiSettings = new GuiSettings();
        private final ObservableList<Student> students =
                FXCollections.observableArrayList(new StudentBuilder().build());

        @Override
        public CommandResult execute(String commandText) {
            if (commandText.equals(ExitCommand.COMMAND_WORD)) {
                return new CommandResult(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true);
            }
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
            this.guiSettings = guiSettings;
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
    public void mainWindow_whenLoaded_carriesAnIconThatDecodes() throws Exception {
        MainWindow window = filledWindow();

        Image icon = onFxThread(() -> window.getPrimaryStage().getIcons().get(0));

        assertFalse(icon.isError(), "a window icon that fails to load leaves the title bar blank");
    }

    @Test
    public void keyboardNavigation_tabFromCommandBox_marksStudentListFocus() throws Exception {
        MainWindow window = onFxThread(() -> {
            FxToolkit.FocusStage stage = new FxToolkit.FocusStage();
            MainWindow opened = new MainWindow(stage, new LogicStub(), DATA_FILE);
            opened.fillInnerParts();
            stage.activate();
            stage.getScene().lookup("#commandTextField").requestFocus();
            return opened;
        });
        try {
            FxToolkit.awaitAssertion(() -> assertTrue(
                    window.getRoot().getScene().lookup("#commandTextField").isFocused()));
            onFxThread(() -> {
                press(window.getRoot().getScene().getFocusOwner(), KeyCode.TAB, false);
                return null;
            });
            FxToolkit.awaitAssertion(() -> assertTrue(
                    window.getRoot().getScene().lookup("#studentListView").isFocused()));
            onFxThread(() -> {
                assertStudentFocusVisible(window.getRoot().getScene());
                return null;
            });
        } finally {
            onFxThread(() -> {
                window.getPrimaryStage().hide();
                return null;
            });
        }
    }

    private static void assertStudentFocusVisible(Scene scene) {
        ListView<?> students = (ListView<?>) scene.lookup("#studentListView");
        students.applyCss();
        assertNotNull(students.getBorder(), "keyboard focus must have a visible outline");
        Label prompt = (Label) scene.lookup(".command-prompt");
        assertEquals(prompt.getTextFill(), students.getBorder().getStrokes().getFirst().getTopStroke());
        assertEquals(Insets.EMPTY, students.getInsets(), "the focus outline must not shrink the list");
    }

    @Test
    public void keyboardNavigation_hiddenReply_cyclesThroughStudentsAndTags() throws Exception {
        onFxThread(() -> {
            MainWindow window = filledWindow2();
            try {
                window.show();
                window.getRoot().getScene().lookup("#commandTextField").requestFocus();
                assertTraversal(window, false, "#studentListView", "#tagScroll", "#commandTextField");
                assertTraversal(window, true, "#tagScroll", "#studentListView", "#commandTextField");
            } finally {
                window.getPrimaryStage().hide();
            }
            return null;
        });
    }

    @Test
    public void keyboardNavigation_visibleReply_includesReplyAndPreservesDraftAndHistory() throws Exception {
        onFxThread(() -> {
            MainWindow window = filledWindow2();
            try {
                window.show();
                TextField field = (TextField) window.getRoot().getScene().lookup("#commandTextField");
                field.setText("list");
                field.fireEvent(new ActionEvent());
                field.setText("find -n Alex");
                field.requestFocus();
                assertTraversal(window, false, "#studentListView", "#tagScroll", "#resultDisplay",
                        "#commandTextField");
                assertTraversal(window, true, "#resultDisplay", "#tagScroll", "#studentListView",
                        "#commandTextField");
                assertEquals("find -n Alex", field.getText());
                press(field, KeyCode.UP, false);
                assertEquals("list", field.getText());
                press(field, KeyCode.DOWN, false);
                assertEquals("find -n Alex", field.getText());
            } finally {
                window.getPrimaryStage().hide();
            }
            return null;
        });
    }

    /** Sends Tab through the real window, checking focus eligibility and both traversal directions. */
    private static void assertTraversal(MainWindow window, boolean isBackwards, String... expectedIds) {
        Scene scene = window.getRoot().getScene();
        for (String id : expectedIds) {
            press(scene.getFocusOwner(), KeyCode.TAB, isBackwards);
            Node expected = scene.lookup(id);
            assertTrue(expected.isFocusTraversable(), id + " must remain keyboard reachable");
            assertEquals(expected, scene.getFocusOwner(), "Tab should reach " + id);
        }
    }

    private static void press(Node node, KeyCode key, boolean isShiftDown) {
        node.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key,
                isShiftDown, false, false, false));
    }

    @Test
    public void keyboardNavigation_modifiedTabAndUnlistedFocus_preservesFocusAndDraft() throws Exception {
        onFxThread(() -> {
            MainWindow window = filledWindow2();
            Scene scene = window.getRoot().getScene();
            TextField field = (TextField) scene.lookup("#commandTextField");
            field.setText("find -n Alex");
            field.requestFocus();
            for (int modifier = 0; modifier < 3; modifier++) {
                assertTabIgnored(field, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                        false, modifier == 0, modifier == 1, modifier == 2));
            }
            assertEquals("find -n Alex", field.getText());
            Node page = scene.lookup("#studentPage");
            page.requestFocus();
            assertEquals(page, scene.getFocusOwner());
            assertTabIgnored(page, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                    false, false, false, false));
            return null;
        });
    }

    /** Stops default traversal after checking that the application leaves the event untouched. */
    private static void assertTabIgnored(Node target, KeyEvent event) {
        Node focused = target.getScene().getFocusOwner();
        AtomicBoolean delivered = new AtomicBoolean();
        EventHandler<KeyEvent> observer = received -> {
            delivered.set(true);
            assertEquals(focused, target.getScene().getFocusOwner());
            received.consume();
        };
        target.addEventFilter(KeyEvent.KEY_PRESSED, observer);
        try {
            target.fireEvent(event);
            assertTrue(delivered.get(), "modified or unlisted traversal must reach JavaFX");
            assertEquals(focused, target.getScene().getFocusOwner());
        } finally {
            target.removeEventFilter(KeyEvent.KEY_PRESSED, observer);
        }
    }

    @Test
    public void keyboardNavigation_emptyLists_keepsCommandBoxReachable() throws Exception {
        onFxThread(() -> {
            LogicStub logic = new LogicStub();
            logic.students.clear();
            MainWindow window = new MainWindow(new Stage(), logic, DATA_FILE);
            window.fillInnerParts();
            try {
                window.show();
                window.getRoot().getScene().lookup("#commandTextField").requestFocus();
                assertTraversal(window, false, "#studentListView", "#tagScroll", "#commandTextField");
                assertTraversal(window, true, "#tagScroll", "#studentListView", "#commandTextField");
                assertTrue(logic.students.isEmpty());
            } finally {
                window.getPrimaryStage().hide();
            }
            return null;
        });
    }

    @Test
    public void keyboardNavigation_manyStudents_movesSelectionWithoutChangingRecords() throws Exception {
        onFxThread(() -> {
            LogicStub logic = new LogicStub();
            for (int i = 1; i < 30; i++) {
                logic.students.add(new StudentBuilder().withName("Student " + i).build());
            }
            MainWindow window = new MainWindow(new Stage(), logic, DATA_FILE);
            window.fillInnerParts();
            try {
                window.show();
                Scene scene = window.getRoot().getScene();
                scene.getRoot().applyCss();
                scene.getRoot().layout();
                scene.lookup("#commandTextField").requestFocus();
                assertTraversal(window, false, "#studentListView");
                assertStudentNavigation((ListView<?>) scene.getFocusOwner());
                assertTraversal(window, true, "#commandTextField");
                assertEquals(30, logic.students.size());
            } finally {
                window.getPrimaryStage().hide();
            }
            return null;
        });
    }

    private static void assertStudentNavigation(ListView<?> students) {
        press(students, KeyCode.HOME, false);
        assertEquals(0, students.getSelectionModel().getSelectedIndex());
        press(students, KeyCode.DOWN, false);
        assertEquals(1, students.getSelectionModel().getSelectedIndex());
        press(students, KeyCode.UP, false);
        assertEquals(0, students.getSelectionModel().getSelectedIndex());
        press(students, KeyCode.PAGE_DOWN, false);
        assertTrue(students.getSelectionModel().getSelectedIndex() > 1);
        press(students, KeyCode.PAGE_UP, false);
        assertEquals(0, students.getSelectionModel().getSelectedIndex());
        press(students, KeyCode.END, false);
        assertEquals(students.getItems().size() - 1, students.getSelectionModel().getSelectedIndex());
        press(students, KeyCode.DOWN, false);
        assertEquals(students.getItems().size() - 1, students.getSelectionModel().getSelectedIndex());
        press(students, KeyCode.HOME, false);
        press(students, KeyCode.UP, false);
        assertEquals(0, students.getSelectionModel().getSelectedIndex());
    }

    @Test
    public void keyboardNavigation_helpFromTags_restoresCommandFocusAndDraft() throws Exception {
        onFxThread(() -> {
            MainWindow window = filledWindow2();
            try {
                window.show();
                Scene scene = window.getRoot().getScene();
                TextField field = (TextField) scene.lookup("#commandTextField");
                field.setText("find -n Alex");
                field.requestFocus();
                assertTraversal(window, false, "#studentListView", "#tagScroll");
                press(scene.getFocusOwner(), KeyCode.F1, false);
                assertTrue(scene.lookup("#helpPagePlaceholder").isVisible());
                press(scene.getFocusOwner(), KeyCode.ESCAPE, false);
                assertFalse(scene.lookup("#helpPagePlaceholder").isVisible());
                assertEquals(field, scene.getFocusOwner());
                assertEquals("find -n Alex", field.getText());
                assertTraversal(window, false, "#studentListView");
            } finally {
                window.getPrimaryStage().hide();
            }
            return null;
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
                double scaleX = Screen.getPrimary().getOutputScaleX();
                double scaleY = Screen.getPrimary().getOutputScaleY();
                String geometry = "content=" + content + ", display=" + display
                        + ", stage=" + stage.getWidth() + "x" + stage.getHeight()
                        + ", scale=" + scaleX + "x" + scaleY
                        + ", slack=" + snapAllowance(scaleX) + "x" + snapAllowance(scaleY);
                // Window managers may include invisible frame shadows in the stage origin.
                assertTrue(isBeyondStart(content.getMinX(), display.getMinX(), scaleX),
                        "left edge: " + geometry);
                assertTrue(isBeyondStart(content.getMinY(), display.getMinY(), scaleY),
                        "top edge: " + geometry);
                assertTrue(isWithinLimit(content.getMaxX(), display.getMaxX(), scaleX),
                        "right edge: " + geometry);
                assertTrue(isWithinLimit(content.getMaxY(), display.getMaxY(), scaleY),
                        "bottom edge: " + geometry);
                assertTrue(isWithinLimit(stage.getWidth(), display.getWidth(), scaleX),
                        "window width: " + geometry);
                assertTrue(isWithinLimit(stage.getHeight(), display.getHeight(), scaleY),
                        "window height: " + geometry);
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
        double heightScale = Screen.getPrimary().getOutputScaleY();
        assertTrue(isWithinLimit(stage.getHeight(), MINIMUM_WINDOW_HEIGHT, heightScale),
                "the minimum height prevents resizing to fit: height=" + stage.getHeight()
                        + ", scale=" + heightScale);
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

    @Test
    public void geometrySlack_usableAreaThatSnapsUp_acceptsTheRoundedWindowButNotARealOverflow() {
        // Measured on a 1920x1200 display at 125% scaling, its usable area reduced by a 65 pixel
        // dock and a 32 pixel panel. 1470 logical pixels of usable width is 1837.5 device pixels,
        // so the toolkit rounds the window up to 1838 and the scene snaps to 1839. Neither is the
        // product failing to fit, and no window manager runs under a plain Xvfb to reproduce it.
        double scale = 1.25;
        double usableMaxX = 1535.0;
        double usableWidth = 1470.0;
        assertTrue(isWithinLimit(1536.8000717163086, usableMaxX, scale),
                "the scene snapped up by device pixels and still fits");
        assertTrue(isWithinLimit(1470.4000244140625, usableWidth, scale),
                "the window snapped up by a device pixel and still fits");
        assertTrue(isBeyondStart(65.5999984741211, 65.0, scale),
                "the scene snapped right by a device pixel and still starts inside");
        // A window that genuinely overflows must still fail. The startup dialog in #626 stood
        // 87 pixels past the screen before it was capped, which no snapping slack may absorb.
        assertFalse(isWithinLimit(usableMaxX + 87, usableMaxX, scale),
                "a real overflow must not be hidden by the snapping slack");
    }

    /** Returns {@link #SNAP_ALLOWANCE_DEVICE_PIXELS} in the logical pixels of {@code scale}. */
    private static double snapAllowance(double scale) {
        return SNAP_ALLOWANCE_DEVICE_PIXELS / scale;
    }

    /** Returns whether {@code edge} reaches no further than {@code limit}, allowing for snapping. */
    private static boolean isWithinLimit(double edge, double limit, double scale) {
        return edge <= limit + snapAllowance(scale);
    }

    /** Returns whether {@code edge} starts no earlier than {@code limit}, allowing for snapping. */
    private static boolean isBeyondStart(double edge, double limit, double scale) {
        return edge >= limit - snapAllowance(scale);
    }

    private static void assertInside(Node container, Node child) {
        Bounds outer = container.localToScene(container.getLayoutBounds());
        Bounds inner = child.localToScene(child.getLayoutBounds());
        double scale = Screen.getPrimary().getOutputScaleY();
        assertTrue(isBeyondStart(inner.getMinY(), outer.getMinY(), scale),
                child + " extends above its container, at scale " + scale);
        assertTrue(isWithinLimit(inner.getMaxY(), outer.getMaxY(), scale),
                child + " extends below its container, at scale " + scale);
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

    @Test
    public void commandLine_exitResult_closesWindowAndSavesBounds() throws Exception {
        onFxThread(() -> {
            LogicStub logic = new LogicStub();
            MainWindow window = new MainWindow(new Stage(), logic, DATA_FILE);
            window.fillInnerParts();
            window.show();
            Stage stage = window.getPrimaryStage();
            try {
                GuiSettings expected = new GuiSettings(stage.getWidth(), stage.getHeight(),
                        (int) stage.getX(), (int) stage.getY());
                TextField field = (TextField) stage.getScene().lookup("#commandTextField");
                field.setText(ExitCommand.COMMAND_WORD);
                field.fireEvent(new ActionEvent());
                assertFalse(stage.isShowing());
                assertEquals(expected, logic.guiSettings);
                TextArea feedback = (TextArea) stage.getScene().lookup("#resultDisplay");
                assertEquals(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, feedback.getText());
            } finally {
                stage.hide();
            }
            return null;
        });
    }

}
