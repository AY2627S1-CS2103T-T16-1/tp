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
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class CommandBoxTest {

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

    /** A command box that executes nothing, since only the colouring is under test here. */
    private static CommandBox boxShowing(String typed) throws Exception {
        return onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            setTextOf(box, typed);
            return box;
        });
    }

    /** Puts {@code typed} in the field and returns nothing, so it can be run on the FX thread. */
    private static Object setTextOf(CommandBox box, String typed) {
        TextField field = (TextField) box.getRoot().lookup("#commandTextField");
        field.setText(typed);
        return null;
    }

    private static List<String> colouredTextOf(CommandBox box) {
        TextFlow flow = (TextFlow) box.getRoot().lookup("#highlighted");
        return flow.getChildren().stream().map(node -> ((Text) node).getText()).toList();
    }

    @Test
    public void typing_aCommand_paintsItBehindTheField() throws Exception {
        CommandBox box = boxShowing("add John -p 123");

        // joined back together, the coloured copy must read as what was typed
        assertEquals("add John -p 123", String.join("", colouredTextOf(box)));
    }

    @Test
    public void typing_aCommand_coloursTheCommandWordApartFromItsOptions() throws Exception {
        CommandBox box = boxShowing("add John -p 123");
        TextFlow flow = (TextFlow) box.getRoot().lookup("#highlighted");

        assertTrue(((Text) flow.getChildren().get(0)).getStyleClass().contains("command-command-word"));
        boolean hasOption = flow.getChildren().stream()
                .anyMatch(node -> node.getStyleClass().contains("command-option"));
        assertTrue(hasOption, "the -p option should be coloured as an option");
    }

    @Test
    public void typing_anUnclosedQuote_stillPaints() throws Exception {
        // a command is coloured while it is being typed, so a quote not yet closed is ordinary
        CommandBox box = boxShowing("add \"Siti Nur-Aisyah");

        assertEquals("add \"Siti Nur-Aisyah", String.join("", colouredTextOf(box)));
    }

    @Test
    public void colouredCopy_isSetInTheSameFontAsTheField() throws Exception {
        // the copy is painted behind the field rather than inside it, so the two drift apart
        // on screen the moment their fonts differ. Nothing else keeps them lined up.
        Font[] fonts = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            setTextOf(box, "add John -p 123");
            Region root = box.getRoot();
            Scene scene = new Scene(root, 600, 60);
            assertNotNull(scene);
            root.applyCss();
            root.layout();

            TextField field = (TextField) root.lookup("#commandTextField");
            TextFlow flow = (TextFlow) root.lookup("#highlighted");
            return new Font[] {field.getFont(), ((Text) flow.getChildren().get(0)).getFont()};
        });

        assertEquals(fonts[0].getFamily(), fonts[1].getFamily());
        assertEquals(fonts[0].getSize(), fonts[1].getSize());
    }

    @Test
    public void caret_movesWithTheCharacterTheUserIsOn() throws Exception {
        // the field's own caret is drawn in its text colour, which is transparent while the
        // colours show, so the line carries one of its own and has to place it
        double[] offsets = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            setTextOf(box, "add John");
            Region root = box.getRoot();
            Scene scene = new Scene(root, 600, 60);
            assertNotNull(scene);
            root.applyCss();
            root.layout();

            TextField field = (TextField) root.lookup("#commandTextField");
            Region caret = (Region) root.lookup("#caret");

            field.positionCaret(0);
            double atStart = caret.getTranslateX();
            field.positionCaret("add John".length());
            return new double[] {atStart, caret.getTranslateX()};
        });

        assertEquals(0.0, offsets[0]);
        assertTrue(offsets[1] > offsets[0], "the caret should sit further along for a later position");
    }

    @Test
    public void colours_whenTheCommandOutgrowsTheLine_stepAsideCompletely() throws Exception {
        boolean[] state = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            Scene scene = new Scene(root, 600, 60);
            assertNotNull(scene);
            root.applyCss();
            root.layout();

            setTextOf(box, "add ".repeat(300));
            root.layout();

            TextFlow flow = (TextFlow) root.lookup("#highlighted");
            Region ownCaret = (Region) root.lookup("#caret");
            return new boolean[] {flow.isVisible(), flow.isManaged(), ownCaret.isManaged()};
        });

        // hidden is not enough: a TextFlow wraps, so a long command left managed keeps asking
        // for the height of every wrapped line and the command box grows to match
        assertFalse(state[0], "the colours should step aside once the command does not fit");
        assertFalse(state[1], "the hidden copy should stop taking part in layout");
        assertFalse(state[2], "the field draws its own caret again, so ours should go");
    }

    @Test
    public void colours_whenTheLineNarrows_stepAsideWithoutRetyping() throws Exception {
        boolean[] state = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            Scene scene = new Scene(root, 900, 60);
            assertNotNull(scene);
            root.applyCss();
            root.layout();
            setTextOf(box, "add Siti Nur-Aisyah -p 84420917 -t T1 --follow-up");
            root.layout();
            boolean wide = ((TextFlow) root.lookup("#highlighted")).isVisible();

            // the window narrowing is not a change of text, so nothing would have rechecked
            scene.getWindow();
            root.resize(180, 60);
            root.layout();
            return new boolean[] {wide, ((TextFlow) root.lookup("#highlighted")).isVisible()};
        });

        assertTrue(state[0], "the command should be coloured while it fits");
        assertFalse(state[1], "narrowing the line should hand the text back to the field");
    }

    @Test
    public void clearingTheField_leavesNothingPainted() throws Exception {
        CommandBox box = boxShowing("add John");
        onFxThread(() -> setTextOf(box, ""));

        assertEquals(List.of(), colouredTextOf(box));
    }
}
