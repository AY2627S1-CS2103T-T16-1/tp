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
    /** How many widths the sweep tries, four pixels apart from 300. */
    private static final int WIDTHS_SWEPT = 60;
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

    @Test
    public void setFeedbackToUser_aLongReplyOnOneLine_takesTheRowsItNeeds() throws Exception {
        // "Edited student: ..." arrives as a single line and is wrapped by the area, so
        // counting the breaks it was written with gave it one row and hid the rest of it
        String reply = "Edited student: Siti Nur-Aisyah Binte Abdullah Rahman; "
                + "Phone: 91234567; Email: e1234567@u.nus.edu; "
                + "Tags: needsconsultation urgent; Needs follow-up";

        int rows = onFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            Region root = display.getRoot();
            assertNotNull(styledScene(root, 450, 200));
            display.setFeedbackToUser(reply);
            root.layout();
            return ((TextArea) root.lookup("#resultDisplay")).getPrefRowCount();
        });

        assertTrue(rows > 1, "a reply too long for one row should take more than one: " + rows);
    }

    @Test
    public void setFeedbackToUser_aShortReply_staysOneRow() throws Exception {
        int rows = onFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            Region root = display.getRoot();
            assertNotNull(styledScene(root, 450, 200));
            display.setFeedbackToUser("Listed all students.");
            root.layout();
            return ((TextArea) root.lookup("#resultDisplay")).getPrefRowCount();
        });

        assertEquals(1, rows, "a reply that fits should not leave empty space under it");
    }

    @Test
    public void setFeedbackToUser_theRepliesSeenMost_takeOneRow() throws Exception {
        // the reply a User sees after almost every command is one short line, and an empty
        // row under it is a band of nothing where the list could be
        for (String reply : List.of("Listed all students.", "Opened help window.",
                "Student book has been cleared!", "Invalid command format!")) {
            int rows = onFxThread(() -> {
                ResultDisplay display = new ResultDisplay();
                Region root = display.getRoot();
                assertNotNull(styledScene(root, 740, 200));
                display.setFeedbackToUser(reply);
                root.layout();
                return ((TextArea) root.lookup("#resultDisplay")).getPrefRowCount();
            });

            assertEquals(1, rows, "\"" + reply + "\" should take one row, not " + rows);
        }
    }

    @Test
    public void setFeedbackToUser_inAnyFontTheWindowMayUse_countsRowsTheSameWay() throws Exception {
        // the rows are worked out from the font the area is dressed in, which differs from
        // one machine to the next, so a reply that fits has to fit in any of them
        for (String family : List.of("Inter", "System", "Monospaced", "Serif")) {
            int rows = onFxThread(() -> {
                ResultDisplay display = new ResultDisplay();
                Region root = display.getRoot();
                assertNotNull(styledScene(root, 740, 200));
                root.lookup("#resultDisplay").setStyle("-fx-font-family: \"" + family + "\";");
                root.applyCss();
                root.layout();
                display.setFeedbackToUser("Listed all students.");
                root.layout();
                return ((TextArea) root.lookup("#resultDisplay")).getPrefRowCount();
            });

            assertEquals(1, rows, "one line in " + family + " came to " + rows + " rows");
        }
    }

    @Test
    public void setFeedbackToUser_anyReply_takesTheRowsItIsDrawnIn() throws Exception {
        // measured against what the area draws rather than against a font's own numbers, so
        // this holds whichever font the machine dresses the area in
        int[] counted = onFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            Region root = display.getRoot();
            assertNotNull(styledScene(root, 450, 300));
            TextArea area = (TextArea) root.lookup("#resultDisplay");

            display.setFeedbackToUser("Hi");
            root.applyCss();
            root.layout();
            Text drawn = (Text) area.lookup(".text");
            assertNotNull(drawn, "the area should have drawn its reply");
            double oneRow = drawn.getLayoutBounds().getHeight();

            int[] rows = new int[REPLIES.size() * 2];
            for (int i = 0; i < REPLIES.size(); i++) {
                display.setFeedbackToUser(REPLIES.get(i));
                root.layout();
                rows[i * 2] = (int) Math.round(drawn.getLayoutBounds().getHeight() / oneRow);
                rows[i * 2 + 1] = area.getPrefRowCount();
            }
            return rows;
        });

        for (int i = 0; i < REPLIES.size(); i++) {
            int drawnRows = Math.min(counted[i * 2], MAX_ROWS_UNDER_TEST);
            assertEquals(drawnRows, counted[i * 2 + 1],
                    "\"" + REPLIES.get(i) + "\" is drawn in " + counted[i * 2]
                    + " rows but the area keeps " + counted[i * 2 + 1]);
        }
    }

    @Test
    public void setFeedbackToUser_atEveryWidth_takesTheRowsItIsDrawnIn() throws Exception {
        // a reply whose last word lands within a few pixels of the edge is where a guess at
        // the area's wrap width shows: it puts the wrap a few pixels from where the area
        // puts it, and the reply is given a row it does not use
        String reply = "Edited student: Siti Nur-Aisyah; Phone: 91234567; Tags: urgent";

        int[][] counted = onFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            Region root = display.getRoot();
            assertNotNull(styledScene(root, 400, 300));
            TextArea area = (TextArea) root.lookup("#resultDisplay");
            display.setFeedbackToUser("Hi");
            root.applyCss();
            root.layout();
            Text drawn = (Text) area.lookup(".text");
            double oneRow = drawn.getLayoutBounds().getHeight();

            int[][] atWidth = new int[WIDTHS_SWEPT][3];
            for (int i = 0; i < WIDTHS_SWEPT; i++) {
                int width = 300 + i * 4;
                root.resize(width, 300);
                display.setFeedbackToUser(reply);
                root.layout();
                atWidth[i][0] = width;
                atWidth[i][1] = (int) Math.round(drawn.getLayoutBounds().getHeight() / oneRow);
                atWidth[i][2] = area.getPrefRowCount();
            }
            return atWidth;
        });

        for (int[] at : counted) {
            assertEquals(Math.min(at[1], MAX_ROWS_UNDER_TEST), at[2],
                    "at " + at[0] + " wide the reply is drawn in " + at[1]
                    + " rows but the area keeps " + at[2]);
        }
    }
}
