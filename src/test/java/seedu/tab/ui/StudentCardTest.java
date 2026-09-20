package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.StudentBuilder;

public class StudentCardTest {

    private static final int TIMEOUT_SECONDS = 10;

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

    /** Runs {@code work} on the thread JavaFX requires its nodes to be built on. */
    private static <T> T onFxThread(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static Label emailLabelOf(Student student) throws Exception {
        return onFxThread(() -> (Label) new StudentCard(student, 1).getRoot().lookup("#email"));
    }

    @Test
    public void constructor_studentWithEmail_showsTheAddress() throws Exception {
        Student student = new StudentBuilder().withEmail("alice@example.com").build();
        Label email = emailLabelOf(student);

        assertEquals("alice@example.com", email.getText());
        assertTrue(email.isManaged());
        assertTrue(email.isVisible());
    }

    @Test
    public void constructor_studentWithoutEmail_leavesTheRowOut() throws Exception {
        Student student = new StudentBuilder().withoutEmail().build();
        Label email = emailLabelOf(student);

        // the row is unmanaged as well as hidden, so the card does not keep a blank line for it
        assertFalse(email.isManaged());
        assertFalse(email.isVisible());
    }

    @Test
    public void constructor_showsTheNameAndPhoneWhicheverWay() throws Exception {
        Student student = new StudentBuilder().withName("Ravi s/o Kumaran").withPhone("+65 9123 4567")
                .withoutEmail().build();
        StudentCard card = onFxThread(() -> new StudentCard(student, 7));

        assertEquals("Ravi s/o Kumaran", ((Label) card.getRoot().lookup("#name")).getText());
        assertEquals("+65 9123 4567", ((Label) card.getRoot().lookup("#phone")).getText());
        assertEquals("7. ", ((Label) card.getRoot().lookup("#id")).getText());
    }

    @Test
    public void constructor_tags_areShownInOrder() throws Exception {
        Student student = new StudentBuilder().withTags("T1", "Lab 3", "needs-followup").build();
        FlowPane tags = onFxThread(() -> (FlowPane) new StudentCard(student, 1).getRoot().lookup("#tags"));

        List<String> shown = tags.getChildren().stream()
                .map(node -> ((Label) node).getText())
                .collect(Collectors.toList());
        assertEquals(List.of("Lab 3", "T1", "needs-followup"), shown);
    }
}
