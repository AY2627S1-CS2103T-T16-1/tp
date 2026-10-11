package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class StudentListPanelTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final double LIST_WIDTH = 420;

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

    @Test
    public void cells_longContent_doNotOutgrowTheList() throws Exception {
        Student student = new StudentBuilder()
                .withName("Nurul Izzah binte Hassan Al-Rashid Abdullah Mohamed")
                .withEmail("a.very.long.address.indeed@u.nus.edu")
                .withTags("AY2627 Sem 1 CS2103T T16").build();
        ObservableList<Student> students = FXCollections.observableArrayList(student);

        boolean fits = onFxThread(() -> {
            StudentListPanel panel = new StudentListPanel(students);
            Region root = panel.getRoot();
            Scene scene = new Scene(root, LIST_WIDTH, 400);
            assertNotNull(scene);
            root.applyCss();
            root.layout();

            Set<Node> cells = root.lookupAll(".list-cell");
            assertFalse(cells.isEmpty(), "the list should have laid out at least one cell");
            // a cell wider than the list is what puts a horizontal scroll bar under it, which
            // is what appeared as soon as the sidebar narrowed the list
            return cells.stream().allMatch(cell -> cell.getLayoutBounds().getWidth() <= LIST_WIDTH);
        });

        assertTrue(fits, "a card outgrew the list and would scroll it sideways");
    }
}
