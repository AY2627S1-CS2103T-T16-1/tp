package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

public class ResultDisplayTest {

    private static final int TIMEOUT_SECONDS = 10;
    /** Enough pulses for an answer asked for on one to be acted on by the window. */
    private static final int PULSES = 3;
    /** How many widths the sweep tries, four pixels apart from 300. */
    private static final int WIDTHS_SWEPT = 20;
    /** The most rows the area grows to, as the area states it. */
    private static final int MAX_ROWS_UNDER_TEST = 6;
    /** A short reply, one that wraps, and one written on two lines. */
    private static final List<String> REPLIES = List.of(
            "Listed all students.",
            "Edited student: Siti Nur-Aisyah Binte Abdullah Rahman; Phone: 91234567; "
                    + "Email: e1234567@u.nus.edu; Tags: needsconsultation urgent",
            "Invalid command format!\nadd: Adds a student to the student book.");

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

    /**
     * Returns a scene holding {@code root}, dressed in the stylesheet the window uses, so
     * that a reply is wrapped at the width and in the font the product shows it in.
     */
    private static Scene styledScene(Region root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
                UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
        root.applyCss();
        root.layout();
        return scene;
    }

    /**
     * Runs a pulse: lays the scene out and lets anything the layout asked for on the next
     * pulse happen, which is what the running window gives it. Driving two layouts by hand
     * instead hides work that never happens in the app.
     */
    private static void pulse(Region root) throws Exception {
        for (int beat = 0; beat < PULSES; beat++) {
            onFxThread(() -> {
                root.applyCss();
                root.layout();
                return null;
            });
        }
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

    /** A display laid out in a scene of its own, for sizing to be measured on. */
    private static ResultDisplay displayAt(double width) throws Exception {
        return onFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            assertNotNull(styledScene(display.getRoot(), width, 300));
            return display;
        });
    }

    /** Says {@code reply} and lets the window settle around it. */
    private static void say(ResultDisplay display, String reply) throws Exception {
        onFxThread(() -> {
            display.setFeedbackToUser(reply);
            return null;
        });
        pulse(display.getRoot());
    }

    /** Lays {@code display} out at {@code width}, as a window being resized would. */
    private static void widen(ResultDisplay display, double width) throws Exception {
        Region root = (Region) display.getRoot();
        onFxThread(() -> {
            root.resize(width, 300);
            return null;
        });
    }

    private static int rowsOf(ResultDisplay display) throws Exception {
        return onFxThread(() -> ((TextArea) display.getRoot().lookup("#resultDisplay"))
                .getPrefRowCount());
    }

    /** How many rows the area is drawing the reply in, measured against one row of it. */
    private static int rowsDrawnIn(ResultDisplay display, double oneRow) throws Exception {
        return onFxThread(() -> {
            Text drawn = (Text) display.getRoot().lookup("#resultDisplay").lookup(".text");
            assertNotNull(drawn, "the area should have drawn its reply");
            return (int) Math.round(drawn.getLayoutBounds().getHeight() / oneRow);
        });
    }

    private static double oneRowOf(ResultDisplay display) throws Exception {
        return onFxThread(() -> {
            Text drawn = (Text) display.getRoot().lookup("#resultDisplay").lookup(".text");
            return drawn.getLayoutBounds().getHeight();
        });
    }

    @Test
    public void setFeedbackToUser_anyReply_takesTheRowsItIsDrawnIn() throws Exception {
        ResultDisplay display = displayAt(450);
        say(display, "Hi");
        double oneRow = oneRowOf(display);

        for (String reply : REPLIES) {
            say(display, reply);
            int drawn = Math.min(rowsDrawnIn(display, oneRow), MAX_ROWS_UNDER_TEST);
            assertEquals(drawn, rowsOf(display),
                    "\"" + reply + "\" is drawn in " + drawn + " rows but the area keeps "
                    + rowsOf(display));
        }
    }

    @Test
    public void setFeedbackToUser_theFirstReplyAfterLaunch_takesOneRow() throws Exception {
        // the reply a User sees first, on a display that has said nothing before: the area
        // had no skin to measure when it was built, and was left several rows too tall
        ResultDisplay display = displayAt(740);
        say(display, "Listed all students.");

        assertEquals(1, rowsOf(display), "the first reply should take one row");
    }

    @Test
    public void setFeedbackToUser_aWrappedReplyThenAWiderWindow_comesBackToOneRow()
            throws Exception {
        ResultDisplay display = displayAt(260);
        say(display, "Edited student: Siti Nur-Aisyah; Phone: 91234567");
        int whenNarrow = rowsOf(display);

        widen(display, 900);
        pulse(display.getRoot());

        assertTrue(whenNarrow > 1, "the reply should have wrapped when narrow");
        assertEquals(1, rowsOf(display),
                "a reply that no longer wraps should not leave the area as tall as it was");
    }

    @Test
    public void setFeedbackToUser_atEveryWidth_takesTheRowsItIsDrawnIn() throws Exception {
        // a reply whose last word lands within a few pixels of the edge is where a guess at
        // the area's wrap width shows: it puts the wrap a few pixels from where the area
        // puts it, and the reply is given a row it does not use
        String reply = "Edited student: Siti Nur-Aisyah; Phone: 91234567; Tags: urgent";
        ResultDisplay display = displayAt(400);
        say(display, "Hi");
        double oneRow = oneRowOf(display);

        for (int i = 0; i < WIDTHS_SWEPT; i++) {
            double width = 300 + i * 8;
            widen(display, width);
            say(display, reply);
            int drawn = Math.min(rowsDrawnIn(display, oneRow), MAX_ROWS_UNDER_TEST);
            assertEquals(drawn, rowsOf(display),
                    "at " + width + " wide the reply is drawn in " + drawn
                    + " rows but the area keeps " + rowsOf(display));
        }
    }

}
