package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class UiTextTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int LIMIT = 18;

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

    /** Returns a label holding {@code value}, laid out at {@code width}. */
    private static Label labelAt(String value, double width) {
        Label label = new Label();
        UiText.showShortenedToFit(label, value, LIMIT);
        StackPane holder = new StackPane(label);
        assertNotNull(new Scene(holder, width, 40));
        holder.applyCss();
        holder.layout();
        label.resize(width, label.prefHeight(-1));
        return label;
    }

    @Test
    public void showShortenedToFit_aValueTheWidthCuts_namesItOnHover() throws Exception {
        // under the count of characters it would be cut to, but too wide for the label
        String whole = "CS2103T-TUTORIAL";
        Label label = onFxThread(() -> labelAt(whole, 40));

        assertEquals(whole, label.getText(), "the text is under the count it is cut to");
        assertNotNull(label.getTooltip(), "a value the width cut should still be readable");
        assertEquals(whole, label.getTooltip().getText());
    }

    @Test
    public void showShortenedToFit_aValueThatFits_needsNoTooltip() throws Exception {
        Label label = onFxThread(() -> labelAt("T1", 200));

        assertEquals("T1", label.getText());
        assertNull(label.getTooltip(), "a value shown whole needs no tooltip");
    }

    @Test
    public void showShortenedToFit_aValueLongerThanTheCount_namesItOnHover() throws Exception {
        String whole = "attendance-and-participation";
        Label label = onFxThread(() -> labelAt(whole, 400));

        assertTrue(label.getText().length() < whole.length(), "the value should be cut");
        assertEquals(whole, label.getTooltip().getText());
    }
}
