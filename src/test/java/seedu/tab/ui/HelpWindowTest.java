package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class HelpWindowTest {


    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        return FxToolkit.onFxThread(work);
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

    @Test
    public void window_onceOpen_cannotBeShrunkUntilItClips() throws Exception {
        double[] limits = onFxThread(() -> {
            Stage stage = new Stage();
            HelpWindow window = new HelpWindow(stage);
            Region content = (Region) stage.getScene().getRoot();
            content.applyCss();
            content.layout();
            return new double[] {stage.getMinWidth(), content.prefWidth(-1),
                stage.getMinHeight(), content.prefHeight(-1)};
        });

        // the window may be resized, so it needs a floor: without one it can be dragged
        // narrower than the address it exists to carry
        assertTrue(limits[0] >= limits[1] - 0.5,
                "the window may shrink to " + limits[0] + " for content needing " + limits[1]);
        assertTrue(limits[2] >= limits[3] - 0.5,
                "the window may shrink to " + limits[2] + " for content needing " + limits[3]);
    }

    @Test
    public void frameAround_aWindowThatHasBeenShown_isWhatTheSceneIsNot() {
        assertEquals(16, HelpWindow.frameAround(736, 720), 0.01,
                "the frame is whatever the window is wider than its scene");
    }

    @Test
    public void frameAround_aWindowNotYetShown_isNothing() {
        // a window that has not been shown has no size to measure, and no frame yet either
        assertEquals(0, HelpWindow.frameAround(Double.NaN, 720), 0.01);
        assertEquals(0, HelpWindow.frameAround(736, Double.NaN), 0.01);
        // nor can a scene be wider than the window around it
        assertEquals(0, HelpWindow.frameAround(700, 720), 0.01);
    }

    @Test
    public void show_afterDeferredResize_fitsContentInsideTheScene() throws Exception {
        HelpWindow window = onFxThread(() -> {
            HelpWindow helpWindow = new HelpWindow(new Stage());
            helpWindow.show();
            return helpWindow;
        });
        try {
            // A second dispatch runs after show()'s queued re-fit, while the window stays open.
            double[] measured = onFxThread(() -> {
                assertTrue(window.isShowing(), "the window should stay open until it is measured");
                Stage stage = window.getRoot();
                Region content = (Region) stage.getScene().getRoot();
                return new double[] {stage.getScene().getWidth(), content.prefWidth(-1),
                    stage.getScene().getHeight(), content.prefHeight(-1)};
            });
            assertTrue(measured[0] >= measured[1] - 0.5,
                    "the scene is " + measured[0] + " wide for content needing " + measured[1]);
            assertTrue(measured[2] >= measured[3] - 0.5,
                    "the scene is " + measured[2] + " tall for content needing " + measured[3]);
        } finally {
            onFxThread(() -> {
                window.hide();
                assertFalse(window.isShowing(), "the window should be hidden after the test");
                return null;
            });
        }
    }
}
