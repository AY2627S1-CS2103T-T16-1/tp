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

    private static TextArea areaOf(ResultDisplay display) {
        return (TextArea) display.getRoot().lookup("#resultDisplay");
    }

    private static String textOf(ResultDisplay display) {
        return areaOf(display).getText();
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
    public void setFeedbackToUser_oneLine_showsItAndTakesSpace() throws Exception {
        ResultDisplay display = showing("Listed all students.");

        assertTrue(display.getRoot().isManaged());
        assertTrue(display.getRoot().isVisible());
        assertEquals("Listed all students.", textOf(display));
    }

    @Test
    public void setFeedbackToUser_severalLines_keepsEveryOne() throws Exception {
        // a command reporting more than one bad field must not have its tail cut off
        String both = "Phone \"12\" is not valid\nEmail \"nope\" is not valid";
        ResultDisplay display = showing(both);

        assertEquals(both, textOf(display));
    }

    @Test
    public void setFeedbackToUser_aLongLine_isWrappedRatherThanCutOff() throws Exception {
        ResultDisplay display = showing("a very long line ".repeat(20).trim());

        assertTrue(areaOf(display).isWrapText(), "a long result should wrap, not run off the window");
    }

    @Test
    public void resultArea_staysReadableWhenItCannotAllBeShown() throws Exception {
        // a reply too long for the space has to remain reachable, so the area keeps the
        // scrolling and the selection a text area gives and a label does not
        ResultDisplay display = showing(String.join("\n", java.util.Collections.nCopies(40, "line")));
        TextArea area = areaOf(display);

        assertFalse(area.isEditable(), "the reply is not something the User edits");
        assertTrue(area.isWrapText());
        assertEquals(40, area.getText().lines().count(), "every line must still be there to scroll to");
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
