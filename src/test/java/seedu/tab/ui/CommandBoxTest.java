package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.control.skin.TextInputControlSkin.Direction;
import javafx.scene.control.skin.TextInputControlSkin.TextUnit;
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
    /** A command that runs past the right-hand edge of a line of either width below. */
    private static final String LONG_COMMAND =
            "find Siti Nur-Aisyah Binte Abdullah Rahman and some more besides thousand";
    private static final double NARROW_LINE = 490;
    private static final double WIDE_LINE = 1000;
    private static final double LINE_HEIGHT = 60;
    /** A line too short for the prompt's own text, so a command reading as it scrolls. */
    private static final double TIGHT_LINE = 190;
    /** How much narrower a window dragged in by its edge is left, as 1000 px becomes 850. */
    private static final double DRAGGED_IN_BY = 150;
    private static final int A_CUT = 8;
    private static final int TWO_WORDS = 12;
    private static final int A_FEW_CHARACTERS = 4;
    /** Every state the field re-scrolls its own text in, by what leaves it there. */
    private static final List<String> RESCROLLING_EDITS = List.of(
            "backspace", "backspace thrice", "ctrl-backspace", "select and type",
            "cut the end", "delete", "narrow the line");
    /** Where a click is aimed along the line, and how far apart those aims are. */
    private static final double FIRST_AIM = 20;
    private static final double BETWEEN_AIMS = 37;

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
        // font instead, and the caret came to rest partway through a character. What is
        // compared is the advance from one character to the next, not the distance from
        // the edge: the caret is centred on the point it marks, and the field insets its
        // text, so where it starts is not one character's width
        double step = offsets[1] - offsets[0];
        assertTrue(step > 0, "the caret should move as characters are typed");
        for (int i = 2; i < offsets.length; i++) {
            assertEquals(step, offsets[i] - offsets[i - 1], 0.75,
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
        assertTrue(faintTokensOf(false).isEmpty(),
                "these tokens disappear into the selection: " + faintTokensOf(false));
    }

    @Test
    public void selection_onARefusedCommand_leavesItLegible() throws Exception {
        // a refused command is written in one colour, and selecting it to correct it must
        // not take it away
        assertTrue(faintTokensOf(true).isEmpty(),
                "a refused command disappears into the selection: " + faintTokensOf(true));
    }

    /** Returns the tokens that do not stand out from the band drawn behind a selection. */
    private static List<String> faintTokensOf(boolean refused) throws Exception {
        CommandBox box = boxShowing("add \"Siti Nur-Aisyah\" -p 98765432 -t T1");
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 500, 60)));

        return onFxThread(() -> {
            TextField field = (TextField) root.lookup("#commandTextField");
            if (refused) {
                field.getStyleClass().add(CommandBox.ERROR_STYLE_CLASS);
                root.lookup("#highlighted").getStyleClass().add("command-error");
                root.applyCss();
            }
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
    }

    @Test
    public void edits_ofEveryKind_leaveTheCopyOnTheFieldsOwnText() throws Exception {
        // the copy is compared with the field's own text node rather than with a model of
        // where the field ought to have scrolled to. Every model tried here has been right
        // for some edits and wrong for others: typing, deleting, cutting and undoing scroll
        // a field by different rules
        CommandBox box = onFxThread(() -> new CommandBox(unused -> null));
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 320, 60)));
        String command = "find Siti Nur-Aisyah Binte Abdullah Rahman and some more besides";

        List<String> adrift = new ArrayList<>();
        for (String edit : List.of("type", "end", "home", "backspace", "delete",
                "cut", "select-and-type", "undo")) {
            double apart = onFxThread(() -> {
                TextField field = (TextField) root.lookup("#commandTextField");
                field.setText(command);
                field.positionCaret(field.getLength());
                applyEdit(field, edit);
                root.layout();

                TextFlow copy = (TextFlow) root.lookup("#highlighted");
                Text drawn = fieldTextOf(field);
                assertNotNull(drawn, "the field should be drawing its own text");
                return Math.abs(copy.getLayoutX()
                        - (drawn.getLayoutX() + drawn.getTranslateX()));
            });
            if (apart > 0.5) {
                adrift.add(edit + " by " + apart);
            }
        }

        assertTrue(adrift.isEmpty(), "the copy left the field's own text: " + adrift);
    }

    /** Does to {@code field} what the User would, by the name of the edit. */
    private static void applyEdit(TextField field, String edit) {
        if (edit.equals("type")) {
            field.appendText("!");
        } else if (edit.equals("end")) {
            field.end();
        } else if (edit.equals("home")) {
            field.home();
        } else if (edit.equals("backspace")) {
            field.deletePreviousChar();
        } else if (edit.equals("delete")) {
            field.positionCaret(field.getLength() / 2);
            field.deleteNextChar();
        } else if (edit.equals("cut")) {
            field.selectRange(field.getLength() - 8, field.getLength());
            field.replaceSelection("");
        } else if (edit.equals("select-and-type")) {
            field.selectRange(field.getLength() - 12, field.getLength());
            field.replaceSelection("x");
        } else if (edit.equals("undo")) {
            field.undo();
        } else {
            throw new IllegalArgumentException(edit);
        }
    }

    /** Returns the node the field draws its own text in. */
    private static Text fieldTextOf(TextField field) {
        return field.lookupAll(".text").stream()
                .filter(node -> node instanceof Text text
                        && text.getText().equals(field.getText()))
                .map(node -> (Text) node)
                .findFirst()
                .orElse(null);
    }

    /** What has to hold of the command line once the field has re-scrolled its own text. */
    private interface LineCheck {
        /** Returns what is wrong with the line as it stands, or nothing if it is right. */
        String complaintAbout(Region root, TextField field, TextFieldSkin skin);
    }

    /**
     * Returns what {@code check} finds wrong in each state the field re-scrolls its text in,
     * at each of the two widths the line is checked at.
     */
    private static List<String> complaintsIn(LineCheck check) throws Exception {
        return onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            assertNotNull(styledScene(root, WIDE_LINE, LINE_HEIGHT));
            TextField field = (TextField) root.lookup("#commandTextField");
            TextFieldSkin skin = (TextFieldSkin) field.getSkin();
            assertNotNull(skin, "an edit can only be driven through a skin the field has");

            List<String> complaints = new ArrayList<>();
            for (double width : List.of(NARROW_LINE, WIDE_LINE)) {
                for (String edit : RESCROLLING_EDITS) {
                    putTheLineIn(edit, width, root, field, skin);
                    String wrong = check.complaintAbout(root, field, skin);
                    if (!wrong.isEmpty()) {
                        complaints.add(edit + " at " + width + ": " + wrong);
                    }
                }
            }
            return complaints;
        });
    }

    /**
     * Leaves a command longer than the line in the state {@code edit} names, laid out.
     *
     * <p>The field's skin re-scrolls its text after the edit it was given, so the line only
     * reaches the state the User would see it in once that layout has run.
     */
    private static void putTheLineIn(String edit, double width, Region root,
            TextField field, TextFieldSkin skin) {
        root.resize(width, LINE_HEIGHT);
        root.layout();
        field.setText(LONG_COMMAND);
        field.positionCaret(field.getLength());
        root.layout();
        if (edit.equals("narrow the line")) {
            root.resize(width - DRAGGED_IN_BY, LINE_HEIGHT);
        } else {
            editThroughTheSkin(edit, field, skin);
        }
        root.layout();
    }

    /**
     * Does to {@code field} what the key {@code edit} names would, through its skin.
     *
     * <p>A {@code TextField} method goes round the skin, which is what re-scrolls the text
     * after a delete or a replace, so an edit driven that way leaves the line in a state the
     * product never reaches.
     */
    private static void editThroughTheSkin(String edit, TextField field, TextFieldSkin skin) {
        if (edit.equals("backspace")) {
            skin.deleteChar(true);
        } else if (edit.equals("backspace thrice")) {
            skin.deleteChar(true);
            skin.deleteChar(true);
            skin.deleteChar(true);
        } else if (edit.equals("ctrl-backspace")) {
            field.selectPreviousWord();
            skin.replaceText(field.getSelection().getStart(), field.getSelection().getEnd(), "");
        } else if (edit.equals("select and type")) {
            skin.replaceText(field.getLength() - TWO_WORDS, field.getLength(), "x");
        } else if (edit.equals("cut the end")) {
            skin.replaceText(field.getLength() - A_CUT, field.getLength(), "");
        } else if (edit.equals("delete")) {
            field.positionCaret(field.getLength() / 2);
            skin.deleteChar(false);
        } else {
            throw new IllegalArgumentException(edit);
        }
    }

    @Test
    public void editsThroughTheSkin_atEitherWidth_leaveTheCopyOnTheFieldsOwnText() throws Exception {
        // the skin re-scrolls the field's text after a delete, a replace or a change of
        // width, after the caret and the text the colouring was told about have settled.
        // Nothing else says it has, so a copy that is not following the field's own text
        // node is left drawing the command over characters the field no longer has there
        List<String> adrift = complaintsIn((root, field, skin) -> {
            Text drawn = fieldTextOf(field);
            assertNotNull(drawn, "the field should be drawing its own text");
            TextFlow copy = (TextFlow) root.lookup("#highlighted");
            double apart = Math.abs(copy.getLayoutX()
                    - (drawn.getLayoutX() + drawn.getTranslateX()));
            return apart > 0.5 ? "the copy is " + apart + " from the field's own text" : "";
        });

        assertTrue(adrift.isEmpty(), "the copy left the field's own text: " + adrift);
    }

    @Test
    public void click_afterAnEditOrAResize_landsOnTheCharacterUnderThePointer() throws Exception {
        // the field decides which character a click lands on, from where it draws its own
        // text, while the User aims at the copy. The two have to read a point alike
        List<String> misread = complaintsIn((root, field, skin) -> {
            TextFlow copy = (TextFlow) root.lookup("#highlighted");
            StringBuilder wrong = new StringBuilder();
            for (double aim = FIRST_AIM; aim < field.getWidth() - FIRST_AIM; aim += BETWEEN_AIMS) {
                int byTheField = skin.getIndex(aim, field.getHeight() / 2).getInsertionIndex();
                double onTheCopy = copy.sceneToLocal(field.localToScene(aim, 0)).getX();
                int byTheCopy = copy.hitTest(new Point2D(onTheCopy, copy.getHeight() / 2))
                        .getInsertionIndex();
                if (byTheField != byTheCopy) {
                    wrong.append(" at ").append(aim).append(" the field reads ").append(byTheField)
                            .append(" where the copy shows ").append(byTheCopy);
                }
            }
            return wrong.toString();
        });

        assertTrue(misread.isEmpty(), "a click lands on a character it was not aimed at:" + misread);
    }

    @Test
    public void caretPastTheEnd_isDrawnInsideTheLineWithTheCopyOnTheFieldsText() throws Exception {
        double[] measured = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            assertNotNull(styledScene(root, NARROW_LINE, LINE_HEIGHT));
            TextField field = (TextField) root.lookup("#commandTextField");
            TextFlow copy = (TextFlow) root.lookup("#highlighted");
            Region mark = (Region) root.lookup("#caret");
            Region lane = (Region) root.lookup("#lane");

            field.setText(LONG_COMMAND);
            field.positionCaret(field.getLength());
            root.layout();

            Text drawn = fieldTextOf(field);
            assertNotNull(drawn, "the field should be drawing its own text");
            return new double[] {copy.getLayoutX(), drawn.getLayoutX() + drawn.getTranslateX(),
                mark.getLayoutX() + mark.getWidth(), lane.getWidth(), mark.getWidth()};
        });

        // both at once: the copy where the field's own text sits, so that a click lands
        // where it was aimed, and the caret inside the line, where the lane is not clipped
        // away and there is something of it left to see
        assertEquals(measured[1], measured[0], 0.5,
                "the copy is drawn from " + measured[0] + " against the field's " + measured[1]);
        assertTrue(measured[4] > 0, "the caret should have a width to be seen by");
        assertTrue(measured[2] <= measured[3] + 0.01,
                "the caret ends at " + measured[2] + ", past the line's " + measured[3]);
    }

    @Test
    public void caretMovedLeft_inACommandLongerThanTheLine_leavesTheLineWhereItIs() throws Exception {
        double[] drawnFrom = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            assertNotNull(styledScene(root, NARROW_LINE, LINE_HEIGHT));
            TextField field = (TextField) root.lookup("#commandTextField");
            TextFlow copy = (TextFlow) root.lookup("#highlighted");
            TextFieldSkin skin = (TextFieldSkin) field.getSkin();

            field.setText(LONG_COMMAND);
            field.positionCaret(field.getLength());
            root.layout();
            Text drawn = fieldTextOf(field);
            assertNotNull(drawn, "the field should be drawing its own text");
            double atTheEnd = copy.getLayoutX();
            double fieldAtTheEnd = drawn.getLayoutX() + drawn.getTranslateX();

            for (int step = 0; step < A_FEW_CHARACTERS; step++) {
                skin.moveCaret(TextUnit.CHARACTER, Direction.LEFT, false);
            }
            root.layout();
            return new double[] {atTheEnd, copy.getLayoutX(), fieldAtTheEnd,
                drawn.getLayoutX() + drawn.getTranslateX()};
        });

        // the field scrolls only when the caret would leave the line, so a copy that scrolls
        // on every move shows a different part of the command from the one the field
        // hit-tests a click against, and a click then lands on the wrong character. What it
        // is held to is the field's own text node rather than a guess at where the field
        // ought to have scrolled to, which says nothing on a runner that gives it no focus
        assertEquals(drawnFrom[2], drawnFrom[0], 0.5, "the copy should start on the field's own text");
        assertEquals(drawnFrom[3], drawnFrom[1], 0.5, "the copy should stay on the field's own text");
        assertEquals(drawnFrom[0], drawnFrom[1], 0.5,
                "moving the caret back within the line moved it from " + drawnFrom[0]
                + " to " + drawnFrom[1]);
    }

    @Test
    public void typing_theLinesOwnPromptText_leavesTheCopyOnTheFieldsOwnText() throws Exception {
        double[] measured = onFxThread(() -> {
            CommandBox box = new CommandBox(unused -> null);
            Region root = box.getRoot();
            assertNotNull(styledScene(root, TIGHT_LINE, LINE_HEIGHT));
            TextField field = (TextField) root.lookup("#commandTextField");
            TextFlow copy = (TextFlow) root.lookup("#highlighted");

            // the field's own text node is named while nothing else can be taken for it,
            // so that the prompt's node cannot stand in for it once the two read alike
            field.setText("list");
            root.layout();
            Text drawn = fieldTextOf(field);
            assertNotNull(drawn, "the field should be drawing its own text");

            field.setText(field.getPromptText());
            field.positionCaret(field.getLength());
            root.layout();
            return new double[] {copy.getLayoutX(), drawn.getLayoutX() + drawn.getTranslateX()};
        });

        // an empty line shows the prompt, and the field keeps the node it drew that in and
        // hides it rather than throwing it away. A command that reads as the prompt matches
        // that node too, and the field leaves it where it is however far the command runs
        // past the edge, so a copy drawn on it shows one character where the field
        // hit-tests another
        assertEquals(measured[1], measured[0], 0.5,
                "the copy is drawn from " + measured[0] + " where the field's text is at "
                + measured[1]);
    }

    @Test
    public void laidOut_withTheLineEmpty_drawsTheCopyAtTheFieldsInset() throws Exception {
        // an empty line shows the prompt, which the field draws in a text node of its own:
        // the copy must not take that for the command's text
        CommandBox box = onFxThread(() -> new CommandBox(unused -> null));
        Region root = box.getRoot();
        assertNotNull(onFxThread(() -> styledScene(root, 300, 60)));

        double drawnFrom = onFxThread(() -> {
            TextField field = (TextField) root.lookup("#commandTextField");
            assertTrue(field.getText().isEmpty(), "the line should be empty");
            assertFalse(field.lookupAll(".text").isEmpty(),
                    "the field should be drawing its prompt");
            root.layout();
            return ((TextFlow) root.lookup("#highlighted")).getLayoutX();
        });

        assertTrue(drawnFrom >= 0, "the copy should be placed at the field's own inset");
    }

    @Test
    public void laidOut_beforeTheFieldHasASkin_drawsTheCopyAtTheFieldsInset() throws Exception {
        // a command box that has never been in a window has no text node to read, so the
        // copy falls back to the inset a field uses
        CommandBox box = onFxThread(() -> {
            CommandBox fresh = new CommandBox(unused -> null);
            setTextOf(fresh, "list");
            return fresh;
        });

        double drawnFrom = onFxThread(() -> {
            TextField field = (TextField) box.getRoot().lookup("#commandTextField");
            assertNull(fieldTextOf(field), "a box outside a window should have no text node");
            return ((TextFlow) box.getRoot().lookup("#highlighted")).getLayoutX();
        });

        assertTrue(drawnFrom >= 0, "the copy should still be placed, not left off the line");
    }
}
