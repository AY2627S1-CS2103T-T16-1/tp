package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

public class UiFontsTest {

    private static final int TIMEOUT_SECONDS = 10;

    @BeforeAll
    public static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            started.countDown();
        }
        // JavaFX shuts itself down when the last window closes, and then runs nothing more,
        // so a test that shows a window would leave every test after it waiting forever
        Platform.runLater(() -> Platform.setImplicitExit(false));
        assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "the toolkit did not start");
    }

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void bodyFamily_isOneTheMachineHas() throws Exception {
        String family = onFxThread(UiFonts::bodyFamily);

        assertTrue(Font.getFamilies().contains(family) || family.equals(Font.getDefault().getFamily()),
                "the window should be written in a family the machine has, not " + family);
    }

    @Test
    public void applyTo_aScene_writesWhatIsInItInThatFamily() throws Exception {
        String[] families = onFxThread(() -> {
            Label label = new Label("Siti Nur-Aisyah");
            StackPane root = new StackPane(label);
            Scene scene = new Scene(root, 200, 60);
            scene.getStylesheets().add(
                    UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
            UiFonts.applyTo(scene);
            root.applyCss();
            root.layout();
            return new String[] {label.getFont().getFamily(), UiFonts.bodyFamily()};
        });

        // a stylesheet naming several families falls back to the system font rather than
        // trying the next one, so the family is chosen here and inherited from the window
        assertEquals(families[1], families[0]);
    }

    @Test
    public void applyTo_aMachineWithAPreferredFamily_doesNotFallBack() throws Exception {
        String family = onFxThread(UiFonts::bodyFamily);

        if (Font.getFamilies().stream().anyMatch(
                name -> name.equals("Inter") || name.equals("Segoe UI")
                        || name.equals("Helvetica Neue") || name.equals("Cantarell")
                        || name.equals("Noto Sans"))) {
            assertNotEquals("System", family,
                    "a machine with one of the preferred families should use it");
        }
    }

    @Test
    public void firstOf_aMachineWithNoneOfThem_keepsItsOwn() throws Exception {
        String chosen = onFxThread(() -> UiFonts.firstOf(
                List.of("Inter", "Segoe UI"), List.of("Some Other Face", "And Another")));

        assertEquals(Font.getDefault().getFamily(), chosen,
                "a machine carrying none of the preferred families should keep its own");
    }

    @Test
    public void firstOf_severalInstalled_takesTheFirstPreferred() throws Exception {
        String chosen = onFxThread(() -> UiFonts.firstOf(
                List.of("Inter", "Segoe UI"), List.of("Segoe UI", "Inter")));

        assertEquals("Inter", chosen, "the order preferred decides, not the order installed");
    }
}
