package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import seedu.tab.commons.core.GuiSettings;
import seedu.tab.logic.Logic;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class MainWindowTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final Path DATA_FILE = Paths.get("data", "tab.json");

    @BeforeAll
    public static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            started.countDown();
        }
        assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the toolkit did not start");
    }

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /** A {@code Logic} that holds one student and runs nothing. */
    private static class LogicStub implements Logic {
        private final ObservableList<Student> students =
                FXCollections.observableArrayList(new StudentBuilder().build());

        @Override
        public CommandResult execute(String commandText) {
            return new CommandResult(commandText);
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
}
