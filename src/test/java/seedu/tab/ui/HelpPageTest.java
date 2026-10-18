package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

public class HelpPageTest {
    private static final double TOLERANCE = 0.0001;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    private static HelpPage pageAt(double width, double height, AtomicInteger quitCount) {
        HelpPage page = new HelpPage(quitCount::incrementAndGet);
        Scene scene = new Scene(page.getRoot(), width, height);
        scene.getStylesheets().add(UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
        page.getRoot().applyCss();
        page.getRoot().layout();
        return page;
    }

    private static ScrollPane viewportOf(HelpPage page) {
        return (ScrollPane) page.getRoot().lookup("#helpViewport");
    }

    private static void press(HelpPage page, KeyCode key) {
        page.getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key,
                false, false, false, false));
    }

    private static void type(HelpPage page, String character) {
        page.getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED,
                false, false, false, false));
    }

    @Test
    public void navigation_alternateKeysAndUnknownInput_moveOnlyWhenRequested() throws Exception {
        FxToolkit.onFxThread(() -> {
            AtomicInteger quitCount = new AtomicInteger();
            HelpPage page = pageAt(490, 360, quitCount);
            ScrollPane viewport = viewportOf(page);
            press(page, KeyCode.DOWN);
            double afterLine = viewport.getVvalue();
            assertTrue(afterLine > 0);
            press(page, KeyCode.UP);
            assertEquals(0, viewport.getVvalue(), TOLERANCE);
            press(page, KeyCode.ENTER);
            assertEquals(afterLine, viewport.getVvalue(), TOLERANCE);
            press(page, KeyCode.END);
            assertEquals(1, viewport.getVvalue());
            press(page, KeyCode.HOME);
            assertEquals(0, viewport.getVvalue());
            type(page, "f");
            assertTrue(viewport.getVvalue() > afterLine);
            double beforeUnknown = viewport.getVvalue();
            press(page, KeyCode.X);
            type(page, "x");
            assertEquals(beforeUnknown, viewport.getVvalue());
            assertEquals(0, quitCount.get());
            return null;
        });
    }

    @Test
    public void navigation_modifiedKeys_preserveReadingPositionAndKeepHelpOpen() throws Exception {
        FxToolkit.onFxThread(() -> {
            AtomicInteger quitCount = new AtomicInteger();
            HelpPage page = pageAt(490, 360, quitCount);
            ScrollPane viewport = viewportOf(page);
            for (int modifier = 0; modifier < 3; modifier++) {
                page.getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.END,
                        false, modifier == 0, modifier == 1, modifier == 2));
                page.getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, "q", "", KeyCode.UNDEFINED,
                        false, modifier == 0, modifier == 1, modifier == 2));
                assertEquals(0, viewport.getVvalue(), "modified keys must not navigate");
                assertEquals(0, quitCount.get(), "modified q must not quit help");
            }
            return null;
        });
    }

    @Test
    public void navigation_allTextFits_showsEndAndRemainsAtBeginning() throws Exception {
        FxToolkit.onFxThread(() -> {
            HelpPage page = pageAt(1200, 4000, new AtomicInteger());
            ScrollPane viewport = viewportOf(page);
            Label text = (Label) page.getRoot().lookup("#helpText");
            Label progress = (Label) page.getRoot().lookup("#helpProgress");
            assertTrue(text.getHeight() < viewport.getViewportBounds().getHeight());
            type(page, " ");
            press(page, KeyCode.DOWN);
            assertEquals(0, viewport.getVvalue());
            assertEquals("(END)", progress.getText());
            Region root = page.getRoot();
            root.resize(490, 360);
            root.layout();
            assertTrue(text.getHeight() > viewport.getViewportBounds().getHeight());
            assertTrue(progress.getText().startsWith("--More--"));
            press(page, KeyCode.END);
            assertEquals(1, viewport.getVvalue());
            assertEquals("(END)", progress.getText());
            return null;
        });
    }
}
