package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class ResultDisplayTest {

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

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static ResultDisplay showing(String feedback) throws Exception {
        return onFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            if (!feedback.isEmpty()) {
                display.setFeedbackToUser(feedback);
            }
            return display;
        });
    }

    private static int rowsOf(ResultDisplay display) {
        return ((TextArea) display.getRoot().lookup("#resultDisplay")).getPrefRowCount();
    }

    @Test
    public void newDisplay_hasNothingToSay_soTakesNoSpace() throws Exception {
        ResultDisplay display = showing("");

        // before the first command the area has nothing in it, and a fixed height left a gap
        // under the command line
        assertFalse(display.getRoot().isManaged());
        assertFalse(display.getRoot().isVisible());
    }

    @Test
    public void setFeedbackToUser_oneLine_takesOneRow() throws Exception {
        ResultDisplay display = showing("Listed all students.");

        assertTrue(display.getRoot().isManaged());
        assertTrue(display.getRoot().isVisible());
        assertEquals(1, rowsOf(display));
    }

    @Test
    public void setFeedbackToUser_severalLines_growsToFitThem() throws Exception {
        // a command reporting more than one bad field must not have its tail cut off
        ResultDisplay display = showing("Phone \"12\" is not valid\nEmail \"nope\" is not valid");

        assertEquals(2, rowsOf(display));
    }

    @Test
    public void setFeedbackToUser_manyLines_stopsGrowingAndScrolls() throws Exception {
        String many = String.join("\n", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        ResultDisplay display = showing(many);

        // beyond the cap the area scrolls rather than pushing the student list off the window
        assertEquals(6, rowsOf(display));
    }

    @Test
    public void setFeedbackToUser_blankFeedback_collapsesAgain() throws Exception {
        ResultDisplay display = showing("Listed all students.");
        onFxThread(() -> {
            display.setFeedbackToUser("   ");
            return null;
        });

        assertFalse(display.getRoot().isManaged());
    }
}
