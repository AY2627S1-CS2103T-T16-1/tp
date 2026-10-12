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
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class StudentListPanelTest {

    private static final int TIMEOUT_SECONDS = 10;
    /**
     * A list narrow enough that a card of its own size would outgrow it. At a comfortable
     * width the card fits whether or not it is held to the list, so the test passed with the
     * binding taken out and guarded nothing.
     */
    private static final double LIST_WIDTH = 300;

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
                .withPhone("91112222")
                .withEmail("a.very.long.address.indeed@u.nus.edu")
                .withTags("AY2627 Sem 1 CS2103T T16")
                .withFlag(true).build();
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
            boolean cellsFit = cells.stream()
                    .allMatch(cell -> cell.getLayoutBounds().getWidth() <= LIST_WIDTH);
            // and the pill at the trailing edge is the first thing a card that outgrew the
            // list carries off the side of it
            Node pill = root.lookup("#followUp");
            assertNotNull(pill, "a flagged student should carry a pill to be checked");
            assertTrue(pill.isVisible(), "the pill should be drawn on a flagged student");
            boolean pillShows = pill.localToScene(pill.getBoundsInLocal()).getMaxX() <= LIST_WIDTH;
            // a phone number cut in half is of no use, so it keeps its width whatever else
            // the card has to give up
            Label phone = (Label) root.lookup("#phone");
            boolean phoneWhole = phone.getWidth() >= phone.prefWidth(-1) - 0.5;
            assertTrue(phoneWhole, "the phone was cut to " + phone.getWidth()
                    + " of the " + phone.prefWidth(-1) + " it needs");
            return cellsFit && pillShows;
        });

        assertTrue(fits, "a card outgrew the list and would scroll it sideways");
    }
}
