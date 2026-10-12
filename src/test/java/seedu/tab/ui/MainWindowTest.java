package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
        private final ObservableList<Student> students =
                FXCollections.observableArrayList(new StudentBuilder().build());

        @Override
        public CommandResult execute(String commandText) {
            return new CommandResult(
                    commandText.equals("long") ? FIVE_LINE_REPLY : commandText);
        }

        @Override
        public ObservableList<Student> getFilteredStudentList() {
            return students;
        }

        @Override
        public GuiSettings getGuiSettings() {
            return new GuiSettings();
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
    public void fillInnerParts_buildsEveryPane() throws Exception {
        MainWindow window = filledWindow();

        for (String id : new String[] {"#commandBoxPlaceholder", "#resultDisplayPlaceholder",
            "#studentListPanelPlaceholder", "#statusbarPlaceholder"}) {
            assertNotNull(window.getRoot().getScene().lookup(id), id + " is missing");
        }
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
