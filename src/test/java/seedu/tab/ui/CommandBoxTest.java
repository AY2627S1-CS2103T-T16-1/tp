package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.logic.parser.exceptions.ParseException;

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

    /**
     * Returns a scene holding {@code root}, dressed in the stylesheet the window uses. A bare
     * scene leaves every node at its default font, so anything measured in one is measuring
     * text the product never shows.
     */
    private static Scene styledScene(Region root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
                UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
        root.applyCss();
        root.layout();
        return scene;
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
            assertNotNull(styledScene(root, 600, 60));

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
            assertNotNull(styledScene(root, 600, 60));

            TextField field = (TextField) root.lookup("#commandTextField");
            Region caret = (Region) root.lookup("#caret");

            field.positionCaret(0);
            double atStart = caret.getLayoutX();
            field.positionCaret("add ".length());
            double midway = caret.getLayoutX();
            field.positionCaret("add John".length());
            return new double[] {atStart, midway, caret.getLayoutX()};
        });

        assertEquals(0.0, offsets[0], "the caret should sit at the start of an unscrolled line");
        assertTrue(offsets[1] > offsets[0], "the caret should sit further along for a later position");
        assertTrue(offsets[2] > offsets[1], "the caret should sit further along for a later position");
    }

    @Test
    public void caret_followsEachCharacterAsItIsTyped() throws Exception {
        double[] offsets = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            assertNotNull(styledScene(root, 600, 60));

            TextField field = (TextField) root.lookup("#commandTextField");
            Region ownCaret = (Region) root.lookup("#caret");
            double[] seen = new double[4];
            int at = 0;
            for (char typed : "list".toCharArray()) {
                field.appendText(String.valueOf(typed));
                root.layout();
                seen[at++] = ownCaret.getLayoutX();
            }
            return seen;
        });

        // the line is monospace, so every character advances the caret by the same amount.
        // Measuring a span before the stylesheet reached it gave the default proportional
        // font instead, and the caret came to rest partway through a character.
        double step = offsets[0];
        for (int i = 1; i < offsets.length; i++) {
            assertEquals(step * (i + 1), offsets[i], 0.75,
                    "the caret should advance evenly, but stopped at " + offsets[i]);
        }
    }

    @Test
    public void colours_whenTheCommandOutgrowsTheLine_scrollWithIt() throws Exception {
        Object[] state = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            assertNotNull(styledScene(root, 300, 60));

            TextField field = (TextField) root.lookup("#commandTextField");
            TextFlow flow = (TextFlow) root.lookup("#highlighted");
            Region lane = (Region) root.lookup("#lane");
            double emptyWidth = lane.getWidth();

            field.setText("add \"Siti Nur-Aisyah\" -p 84420917 -t T1 -e someone@u.nus.edu --follow-up");
            field.positionCaret(field.getLength());
            root.layout();

            return new Object[] {flow.isVisible(), flow.getLayoutX(), lane.getWidth(),
                emptyWidth, lane.getClip() != null, flow.getHeight()};
        });

        // the colours stay on however long the command is: they scroll with it rather than
        // switching off, which is what the field itself does with its own text
        assertTrue((Boolean) state[0], "the colours should stay on");
        assertTrue((Double) state[1] < 0, "the copy should be scrolled to keep the caret in view");
        assertTrue((Boolean) state[4], "the lane should be clipped so the copy cannot spill out");
        assertEquals((Double) state[3], (Double) state[2], 1.0,
                "a long command must not drag the command box wider");
        assertTrue((Double) state[5] < 40,
                "the copy must stay on one line rather than wrapping and growing the box");
    }

    @Test
    public void caret_onAnEmptyLine_isOneLineTall() throws Exception {
        double[] heights = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            assertNotNull(styledScene(root, 300, 60));

            TextField field = (TextField) root.lookup("#commandTextField");
            Region ownCaret = (Region) root.lookup("#caret");

            field.setText("add ".repeat(60));
            root.layout();
            double afterLong = ownCaret.getPrefHeight();
            field.setText("");
            root.layout();
            return new double[] {afterLong, ownCaret.getPrefHeight()};
        });

        // clearing a long command used to leave the caret at the height that command needed
        assertEquals(heights[0], heights[1], 4.0,
                "the caret should go back to one line once the command is cleared");
    }

    @Test
    public void selection_isDrawnOverTheCharactersItCovers() throws Exception {
        double[] band = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            assertNotNull(styledScene(root, 400, 60));

            TextField field = (TextField) root.lookup("#commandTextField");
            Region selection = (Region) root.lookup("#selection");
            TextFlow flow = (TextFlow) root.lookup("#highlighted");

            field.setText("add John");
            field.selectAll();
            root.layout();
            return new double[] {selection.getWidth(), selection.getHeight(), flow.getWidth()};
        });

        // the field's own highlight is turned off, because it is drawn at the offset the
        // field has scrolled to and broke into fragments behind the copy
        assertEquals(band[2], band[0], 1.0, "the band should cover exactly the selected text");
        assertTrue(band[1] > 0, "the band should be as tall as the line");
    }

    @Test
    public void clearingTheField_leavesNothingPainted() throws Exception {
        CommandBox box = boxShowing("add John");
        onFxThread(() -> setTextOf(box, ""));

        assertEquals(List.of(), colouredTextOf(box));
    }

    /** A box whose executor records what it was asked to run, and may refuse to run it. */
    private static CommandBox boxExecutedBy(List<String> ran, boolean refuses) throws Exception {
        return onFxThread(() -> new CommandBox(command -> {
            ran.add(command);
            if (refuses) {
                throw new ParseException("no");
            }
            return new CommandResult("done");
        }));
    }

    /** Presses Enter on the command line, as the field's own action does. */
    private static void pressEnter(CommandBox box) throws Exception {
        onFxThread(() -> {
            box.getRoot().lookup("#commandTextField").fireEvent(new ActionEvent());
            return null;
        });
    }

    private static List<String> styleClassesOf(CommandBox box, String id) {
        return List.copyOf(box.getRoot().lookup(id).getStyleClass());
    }

    @Test
    public void handleCommandEntered_aCommand_runsItAndClearsTheLine() throws Exception {
        List<String> ran = new ArrayList<>();
        CommandBox box = boxExecutedBy(ran, false);
        onFxThread(() -> setTextOf(box, "list"));

        pressEnter(box);

        assertEquals(List.of("list"), ran);
        assertEquals("", onFxThread(() -> ((TextField) box.getRoot()
                .lookup("#commandTextField")).getText()), "the line should be ready for the next command");
    }

    @Test
    public void handleCommandEntered_anEmptyLine_runsNothing() throws Exception {
        List<String> ran = new ArrayList<>();
        CommandBox box = boxExecutedBy(ran, false);

        pressEnter(box);

        // Enter on an empty line is how a User checks they have one, not a command
        assertEquals(List.of(), ran);
    }

    @Test
    public void handleCommandEntered_aCommandThatFails_keepsItAndMarksTheLine() throws Exception {
        List<String> ran = new ArrayList<>();
        CommandBox box = boxExecutedBy(ran, true);
        onFxThread(() -> setTextOf(box, "add"));

        pressEnter(box);

        // what was typed stays, so the User can correct it rather than type it again
        assertEquals("add", onFxThread(() -> ((TextField) box.getRoot()
                .lookup("#commandTextField")).getText()));
        assertTrue(styleClassesOf(box, "#commandTextField").contains(CommandBox.ERROR_STYLE_CLASS));
        assertTrue(styleClassesOf(box, "#highlighted").contains("command-error"),
                "the copy the User reads is the one that has to turn red");
    }

    @Test
    public void handleCommandEntered_failingTwice_marksTheLineOnce() throws Exception {
        List<String> ran = new ArrayList<>();
        CommandBox box = boxExecutedBy(ran, true);
        onFxThread(() -> setTextOf(box, "add"));

        pressEnter(box);
        pressEnter(box);

        long marks = styleClassesOf(box, "#commandTextField").stream()
                .filter(CommandBox.ERROR_STYLE_CLASS::equals).count();
        assertEquals(1, marks, "the mark should not be added again on top of itself");
    }

    @Test
    public void typing_afterAFailure_takesTheMarkOff() throws Exception {
        List<String> ran = new ArrayList<>();
        CommandBox box = boxExecutedBy(ran, true);
        onFxThread(() -> setTextOf(box, "add"));
        pressEnter(box);

        onFxThread(() -> setTextOf(box, "add "));

        assertFalse(styleClassesOf(box, "#commandTextField").contains(CommandBox.ERROR_STYLE_CLASS),
                "a line being corrected should stop reading as a failure");
    }

    @Test
    public void focus_leavingAndReturning_leavesTheColoursInPlace() throws Exception {
        CommandBox box = boxShowing("find Siti");
        onFxThread(() -> {
            Region root = (Region) box.getRoot();
            assertNotNull(styledScene(root, 400, 60));
            root.lookup("#commandTextField").requestFocus();
            root.layout();
            return null;
        });

        assertEquals(List.of("find", " ", "Siti"), colouredTextOf(box));
    }

    @Test
    public void typing_aCharacterAtATime_keepsTheColoursWithTheText() throws Exception {
        CommandBox box = onFxThread(() -> new CommandBox(unused -> null));
        TextField field = onFxThread(() -> (TextField) box.getRoot().lookup("#commandTextField"));
        assertNotNull(onFxThread(() -> styledScene((Region) box.getRoot(), 400, 60)));

        // typing moves the caret before it reports the new text, which the colouring has to
        // sit out rather than measure the command before it against the text after it
        for (char typed : "find Siti".toCharArray()) {
            onFxThread(() -> {
                field.insertText(field.getLength(), String.valueOf(typed));
                return null;
            });
        }

        assertEquals(List.of("find", " ", "Siti"), colouredTextOf(box));
    }

    @Test
    public void caretMoved_beforeTheCopyCaughtUp_leavesTheCopyAlone() throws Exception {
        CommandBox box = boxShowing("find Siti");
        Region root = (Region) box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 400, 60)));
        TextFlow copy = (TextFlow) root.lookup("#highlighted");
        TextField field = (TextField) root.lookup("#commandTextField");

        // JavaFX moves the caret before it reports the new text, so the colouring is asked
        // where to put a copy of the command before it, against the text after it
        double before = onFxThread(() -> {
            copy.getChildren().setAll(new Text("find Sit"));
            copy.setLayoutX(-12);
            field.selectRange(2, 5);
            return copy.getLayoutX();
        });

        assertEquals(-12, before, "a copy that has fallen behind should be left where it is");
        // and the recolour that follows puts both the copy and its place right
        onFxThread(() -> setTextOf(box, "find Sitie"));
        assertEquals(List.of("find", " ", "Sitie"), colouredTextOf(box));
    }

    @Test
    public void caretMovedLeft_inACommandLongerThanTheLine_leavesTheLineWhereItIs() throws Exception {
        CommandBox box = onFxThread(() -> new CommandBox(unused -> null));
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 300, 60)));
        TextFlow copy = (TextFlow) root.lookup("#highlighted");
        TextField field = (TextField) root.lookup("#commandTextField");

        double[] drawnFrom = onFxThread(() -> {
            field.setText("find Siti Nur-Aisyah Binte Abdullah Rahman and some more besides");
            field.positionCaret(field.getLength());
            double atTheEnd = copy.getLayoutX();
            field.positionCaret(field.getLength() - 4);
            return new double[] {atTheEnd, copy.getLayoutX()};
        });

        // the field scrolls only when the caret would leave the line, so a copy that scrolls
        // on every move shows a different part of the command from the one the field hit-tests
        // a click against, and a click then lands on the wrong character
        assertTrue(drawnFrom[0] < 0, "a command longer than the line should have scrolled");
        assertEquals(drawnFrom[0], drawnFrom[1], 0.01,
                "moving the caret back within the line should not move the line");
    }

    @Test
    public void caretMovedToTheStart_ofALongCommand_bringsTheLineBack() throws Exception {
        CommandBox box = onFxThread(() -> new CommandBox(unused -> null));
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 300, 60)));
        TextFlow copy = (TextFlow) root.lookup("#highlighted");
        TextField field = (TextField) root.lookup("#commandTextField");

        double drawnFrom = onFxThread(() -> {
            field.setText("find Siti Nur-Aisyah Binte Abdullah Rahman and some more besides");
            field.positionCaret(field.getLength());
            field.positionCaret(0);
            return copy.getLayoutX();
        });

        assertEquals(0.0, drawnFrom, 0.01, "the start of the command should be back in view");
    }

    @Test
    public void laidOut_fromTheStart_leavesTheLineSomethingToDrawIn() throws Exception {
        CommandBox box = onFxThread(() -> new CommandBox(unused -> null));
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 300, 60)));

        double[] clip = onFxThread(() -> {
            Region lane = (Region) root.lookup("#lane");
            Rectangle cut = (Rectangle) lane.getClip();
            return new double[] {cut.getWidth(), cut.getHeight(), lane.getWidth(), lane.getHeight()};
        });

        // a region is given its width before its height, so a clip cut to the size the lane
        // had when its width arrived is as tall as the lane was then, which is nothing: the
        // line came up blank and stayed blank however much was typed into it
        assertEquals(clip[2], clip[0], 0.01, "the clip should be as wide as the lane");
        assertEquals(clip[3], clip[1], 0.01, "the clip should be as tall as the lane");
        assertTrue(clip[1] > 0, "a line clipped to no height shows nothing");
    }

    @Test
    public void typedText_onAFreshLine_isVisible() throws Exception {
        CommandBox box = onFxThread(() -> new CommandBox(unused -> null));
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 300, 60)));

        boolean drawn = onFxThread(() -> {
            setTextOf(box, "list");
            root.layout();
            Region lane = (Region) root.lookup("#lane");
            Rectangle cut = (Rectangle) lane.getClip();
            TextFlow copy = (TextFlow) root.lookup("#highlighted");
            return cut.getHeight() >= copy.getHeight() && copy.getHeight() > 0;
        });

        assertTrue(drawn, "what was typed should be inside the part of the lane that is drawn");
    }

    @Test
    public void caretPastTheEnd_scrollsNoFurtherThanTheFieldDoes() throws Exception {
        CommandBox box = onFxThread(() -> new CommandBox(unused -> null));
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 300, 60)));
        TextFlow copy = (TextFlow) root.lookup("#highlighted");
        TextField field = (TextField) root.lookup("#commandTextField");

        double[] measured = onFxThread(() -> {
            field.setText("find Siti Nur-Aisyah Binte Abdullah Rahman and some more besides");
            field.positionCaret(field.getLength());
            Region lane = (Region) root.lookup("#lane");
            Region mark = (Region) root.lookup("#caret");
            return new double[] {-copy.getLayoutX(), copy.getWidth() - lane.getWidth(),
                mark.getLayoutX() + mark.getWidth(), lane.getWidth(), mark.getWidth()};
        });

        // a field brings the caret back to its edge and no further. Keeping a margin beyond
        // it leaves the copy a margin's worth from where the field scrolled to, and a click
        // then lands a character or two from the one it was aimed at
        assertEquals(measured[1], measured[0], measured[4] + 0.5,
                "the copy should end at the edge of the line, as the field's text does");
        // and the caret itself has to be inside the line, not hanging on the edge of it
        // where the lane is clipped, or there is nothing of it left to see
        assertTrue(measured[2] <= measured[3],
                "the caret ends at " + measured[2] + ", past the line's " + measured[3]);
        assertTrue(measured[4] > 0, "the caret should have a width to be seen by");
    }

    /** Returns how light {@code colour} is, as WCAG weighs a colour's parts. */
    private static double luminanceOf(Color colour) {
        double[] parts = {colour.getRed(), colour.getGreen(), colour.getBlue()};
        double[] weights = {0.2126, 0.7152, 0.0722};
        double luminance = 0;
        for (int i = 0; i < parts.length; i++) {
            double part = parts[i] <= 0.03928
                    ? parts[i] / 12.92
                    : Math.pow((parts[i] + 0.055) / 1.055, 2.4);
            luminance += weights[i] * part;
        }
        return luminance;
    }

    /** Returns the contrast between two colours, as WCAG states it. */
    private static double contrastBetween(Color one, Color other) {
        double lighter = Math.max(luminanceOf(one), luminanceOf(other));
        double darker = Math.min(luminanceOf(one), luminanceOf(other));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static Color backgroundOf(Region region) {
        return (Color) region.getBackground().getFills().get(0).getFill();
    }

    @Test
    public void selection_againstTheLineBehindIt_isVisible() throws Exception {
        CommandBox box = boxShowing("find Siti");
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 400, 60)));

        double contrast = onFxThread(() -> {
            TextField field = (TextField) root.lookup("#commandTextField");
            field.selectAll();
            root.layout();
            Region band = (Region) root.lookup("#selection");
            assertTrue(band.isVisible(), "selecting the command should draw a band behind it");
            Color behind = backgroundOf(root);
            Color over = backgroundOf(band);
            // the band is translucent, so what the User sees is it over the line behind it
            Color seen = behind.interpolate(over, over.getOpacity());
            return contrastBetween(seen, behind);
        });

        assertTrue(contrast >= 2.0,
                "a selection the User cannot make out is no selection: contrast was " + contrast);
    }

    @Test
    public void selection_withTheCommandOnTopOfIt_leavesEveryTokenLegible() throws Exception {
        CommandBox box = boxShowing("add \"Siti Nur-Aisyah\" -p 98765432 -t T1");
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 500, 60)));

        List<String> faint = onFxThread(() -> {
            TextField field = (TextField) root.lookup("#commandTextField");
            field.selectAll();
            root.layout();
            Region band = (Region) root.lookup("#selection");
            Color behind = backgroundOf(root);
            Color over = backgroundOf(band);
            Color seen = behind.interpolate(over, over.getOpacity());

            TextFlow copy = (TextFlow) root.lookup("#highlighted");
            List<String> tooFaint = new ArrayList<>();
            for (Node run : copy.getChildren()) {
                Text token = (Text) run;
                if (token.getText().isBlank()) {
                    continue;
                }
                double contrast = contrastBetween((Color) token.getFill(), seen);
                if (contrast < 3.0) {
                    tooFaint.add(token.getText() + " at " + Math.round(contrast * 100) / 100.0);
                }
            }
            return tooFaint;
        });

        // selecting a command must not hide half of it: every colour it is written in has to
        // stay legible against the band drawn behind it
        assertTrue(faint.isEmpty(), "these tokens disappear into the selection: " + faint);
    }
}
