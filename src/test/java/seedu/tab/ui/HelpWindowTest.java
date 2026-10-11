package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class HelpWindowTest {

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

    @Test
    public void window_onceItIsDressed_isWideEnoughForWhatItSays() throws Exception {
        double[] measured = onFxThread(() -> {
            Stage stage = new Stage();
            HelpWindow window = new HelpWindow(stage);
            Parent content = stage.getScene().getRoot();
            content.applyCss();
            content.layout();

            Label message = (Label) content.lookup("#helpMessage");
            Button copy = (Button) content.lookup("#copyButton");
            return new double[] {stage.getWidth(), content.prefWidth(-1),
                stage.getHeight(), content.prefHeight(-1),
                message.prefWidth(-1), copy.prefWidth(-1)};
        });

        // the scene is measured when the layout file is read, before the address is put on
        // the label and before the theme has dressed the button, and the window cannot be
        // resized: what did not fit stayed cut off, as "…/UserGuide.ht…" and "Copy U…"
        assertTrue(measured[0] >= measured[1] - 0.5,
                "the window is " + measured[0] + " wide for content needing " + measured[1]);
        assertTrue(measured[2] >= measured[3] - 0.5,
                "the window is " + measured[2] + " tall for content needing " + measured[3]);
        assertTrue(measured[1] >= measured[5],
                "the row should ask for at least the width of the button");
    }

    @Test
    public void window_atTheSizeItOpens_showsTheWholeAddressAndButton() throws Exception {
        boolean[] whole = onFxThread(() -> {
            Stage stage = new Stage();
            HelpWindow window = new HelpWindow(stage);
            Region content = (Region) stage.getScene().getRoot();
            content.applyCss();
            // the size the window opens at, as the layout file sets it
            content.resize(stage.getScene().getWidth(), stage.getScene().getHeight());
            content.layout();

            Label message = (Label) content.lookup("#helpMessage");
            Button copy = (Button) content.lookup("#copyButton");
            return new boolean[] {
                message.getWidth() >= message.prefWidth(-1) - 0.5,
                copy.getWidth() >= copy.prefWidth(-1) - 0.5};
        });

        // an address with its end missing is of no use, and this window exists to carry it
        assertTrue(whole[0], "the address was cut");
        assertTrue(whole[1], "the button was cut");
    }
}
