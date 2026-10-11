package seedu.tab.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    /** Marks the coloured copy, which is the text the User actually sees. */
    private static final String COPY_ERROR_STYLE_CLASS = "command-error";
    private static final String FXML = "CommandBox.fxml";
    private static final int BLINK_MILLIS = 530;

    private final CommandExecutor commandExecutor;

    /**
     * How far along the command the line is drawn from, kept between one change and the next
     * so that it moves only when the caret would otherwise leave the line.
     */
    private double scrolled;

    @FXML
    private TextField commandTextField;

    @FXML
    private TextFlow highlighted;

    @FXML
    private Region caret;

    @FXML
    private Pane lane;

    @FXML
    private Region selection;

    /**
     * Creates a {@code CommandBox} with the given {@code CommandExecutor}.
     */
    public CommandBox(CommandExecutor commandExecutor) {
        super(FXML);
        this.commandExecutor = commandExecutor;
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> setStyleToDefault());
        commandTextField.textProperty().addListener((unused, old, typed) -> recolour(typed));
        recolour(commandTextField.getText());
        // every task is meant to be reachable from the keyboard alone, so the line the User
        // types into is where the caret starts rather than somewhere they have to click
        Platform.runLater(commandTextField::requestFocus);

        // where the copy sits follows the caret, the selection, and how much of the line the
        // field has to show it in, so each of them asks for the same decision again. The last
        // of them covers the line narrowing with the window, which can take a command that
        // fitted and make it scroll.
        ChangeListener<Object> layOutColours = (unused, old, now) -> layOutColours();
        commandTextField.caretPositionProperty().addListener(layOutColours);
        commandTextField.focusedProperty().addListener(layOutColours);
        commandTextField.selectionProperty().addListener(layOutColours);
        commandTextField.widthProperty().addListener(layOutColours);
        clipTheLane();
        lane.widthProperty().addListener((unused, old, width) -> layOutColours());
        blink();
    }

    /**
     * Handles the Enter button pressed event.
     */
    @FXML
    private void handleCommandEntered() {
        String commandText = commandTextField.getText();
        if (commandText.equals("")) {
            return;
        }

        try {
            commandExecutor.execute(commandText);
            commandTextField.setText("");
        } catch (CommandException | ParseException e) {
            setStyleToIndicateCommandFailure();
        }
    }

    /**
     * Repaints the coloured text behind the field so that it reads as what was typed.
     *
     * <p>The field keeps every key the User presses and decides where a click lands; only its
     * own text is transparent, so what is read is the coloured copy underneath, and the caret
     * and the selection are drawn here to go with it. A command longer than the line scrolls
     * the copy the way the field scrolls its own text, so the colours hold however long the
     * command is.
     */
    private void recolour(String typed) {
        highlighted.getChildren().clear();
        for (CommandHighlighter.Span span : CommandHighlighter.highlight(typed)) {
            Text text = new Text(span.text());
            text.getStyleClass().addAll("command-token", styleClassFor(span.kind()));
            highlighted.getChildren().add(text);
        }

        // a Text added just now has not had the stylesheet applied to it, so asking for its
        // font would give the default proportional one. The caret is measured from these
        // fonts, so it has to be the font they will actually be drawn in.
        highlighted.applyCss();
        layOutColours();
    }

    /**
     * Returns true if the coloured copy is of the text the field now holds.
     */
    private boolean copyMatchesField() {
        StringBuilder painted = new StringBuilder();
        for (Node node : highlighted.getChildren()) {
            painted.append(((Text) node).getText());
        }
        return painted.toString().equals(commandTextField.getText());
    }

    /**
     * Lays the coloured copy out on one line and scrolls it so that the caret is in view.
     *
     * <p>A TextFlow wraps at the width it is given, which would turn a long command into
     * several lines and grow the box. It is sized to its content instead and the lane around
     * it is clipped, so the copy runs off the edge the way the field's own text does. The
     * field scrolls to keep its caret in view and so does this, by the same rule, since the
     * field does not report the offset it has scrolled to.
     */
    private void layOutColours() {
        // JavaFX moves the caret before it reports the new text, so this runs once against
        // the copy of the previous command. The recolour that follows puts it right.
        if (!copyMatchesField()) {
            return;
        }

        double content = widthBefore(commandTextField.getLength());
        // the copy is taken out of the lane's layout and placed by hand. Left to the
        // StackPane it would be resized to the lane, and a TextFlow wraps at the width it is
        // given, so a long command became several lines and made the box grow.
        highlighted.setPrefWidth(content);
        highlighted.autosize();
        caret.autosize();

        double visible = Math.max(lane.getWidth(), 1);
        double caretAt = widthBefore(commandTextField.getCaretPosition());
        // the caret is sized first, because how far the line scrolls depends on how wide the
        // caret is: it is the caret that has to end up inside the line, not the point it
        // stands at
        matchCaretToTheLine();
        scrolled = content <= visible ? 0 : scrollThatKeepsTheCaretInView(caretAt, content, visible);

        double middle = lane.getHeight() / 2;
        showSelection(scrolled, middle);
        highlighted.relocate(-scrolled, middle - highlighted.getHeight() / 2);
        caret.relocate(caretAt - scrolled, middle - caret.getHeight() / 2);
        caret.setVisible(commandTextField.isFocused());
    }

    /**
     * Returns how far along the command the line is drawn from, once the caret has moved to
     * {@code caretAt}.
     *
     * <p>A field scrolls only when the caret would leave the part of the command it is
     * showing, and then only far enough to bring it back. Working the offset out from the
     * caret alone instead drags the command back under the caret every time the caret moves
     * left, and the copy then shows a different part of the command from the one the field
     * hit-tests a click against, so a click lands on the wrong character.
     *
     * <p>What is brought back is the caret itself rather than the point it stands at. The
     * caret is drawn a couple of pixels wide, so stopping at the point leaves it hanging on
     * the edge of the line where the lane is clipped, and at the end of a command long
     * enough to scroll there is nothing left of it to see.
     */
    private double scrollThatKeepsTheCaretInView(double caretAt, double content, double visible) {
        double caretWidth = Math.max(caret.getWidth(), caret.prefWidth(-1));
        // the caret stands after the last character, so the line scrolls a caret's width
        // further than the text alone would need; stopping at the text leaves the caret at
        // the very edge of the line, where the lane is clipped and nothing of it shows
        double furthest = Math.max(0, content + caretWidth - visible);
        double from = Math.clamp(scrolled, 0, furthest);
        if (caretAt + caretWidth - from > visible) {
            from = caretAt + caretWidth - visible;
        } else if (caretAt - from < 0) {
            from = caretAt;
        }
        return Math.clamp(from, 0, furthest);
    }

    /**
     * Keeps the lane's clip the size of the lane, so a command longer than the line is cut off
     * at its edge rather than spilling over the rest of the window.
     *
     * <p>The clip follows both sides of the lane rather than being cut to the size the lane
     * has at the time. A region is given its width before its height, so a clip made when the
     * width arrives is as tall as the lane was then, which is nothing: the line came up blank
     * and stayed blank however much was typed into it.
     */
    private void clipTheLane() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(lane.widthProperty());
        clip.heightProperty().bind(lane.heightProperty());
        lane.setClip(clip);
    }

    /**
     * Draws the selected run of the command behind the copy.
     *
     * <p>The field draws its own highlight at the offset it has scrolled to, which is not the
     * offset the copy is drawn at, so the two disagreed and the highlight came out in
     * fragments between the characters. The field's highlight is turned off in the stylesheet
     * and this stands in for it, measured the same way as everything else on the line.
     */
    private void showSelection(double scrolled, double middle) {
        IndexRange range = commandTextField.getSelection();
        boolean hasSelection = range.getLength() > 0;
        selection.setVisible(hasSelection);
        if (!hasSelection) {
            return;
        }
        double from = widthBefore(range.getStart());
        double to = widthBefore(range.getEnd());
        // a managed child is laid out at its preferred size, so resizing it has no effect
        selection.setPrefSize(to - from, caret.getHeight());
        selection.autosize();
        selection.relocate(from - scrolled, middle - caret.getHeight() / 2);
    }

    /**
     * Gives the caret the height and the vertical place of the text beside it.
     *
     * <p>A bar of a fixed height centred in the line does not sit where the text does: a
     * font's ink is not centred in its line box, so the two are out by a point or so. Taking
     * the height from a run of the text itself keeps them together whatever the font, and an
     * empty line falls back to one line of the field's own font rather than keeping the
     * height the command before it needed.
     */
    private void matchCaretToTheLine() {
        if (highlighted.getChildren().isEmpty()) {
            // nothing is painted, so the caret takes the height of one line of the field's
            // own font. Returning early instead left it at whatever the last command needed,
            // and a command that had wrapped left a caret several lines tall on an empty line.
            Text ruler = new Text("X");
            ruler.setFont(commandTextField.getFont());
            caret.setPrefHeight(ruler.getLayoutBounds().getHeight());
            caret.autosize();
            return;
        }
        Text run = (Text) highlighted.getChildren().get(0);
        Bounds ink = run.getLayoutBounds();
        caret.setPrefHeight(ink.getHeight());
        caret.autosize();
        // the run is laid out in the same StackPane cell, so matching its centre lines the
        // two up without having to know anything about the font's metrics

    }

    /**
     * Returns how wide the copy is up to {@code caretPosition}.
     *
     * <p>Each run is measured in its own font. The command word is drawn bold and so is wider
     * than the rest, which is why one font cannot measure the whole line: doing that pushed
     * the caret past the end of the text by about the extra width of that first word.
     */
    private double widthBefore(int caretPosition) {
        double width = 0;
        int seen = 0;
        for (Node node : highlighted.getChildren()) {
            Text run = (Text) node;
            if (seen >= caretPosition) {
                break;
            }
            int take = Math.min(run.getText().length(), caretPosition - seen);
            width += measure(run.getText().substring(0, take), run.getFont());
            seen += run.getText().length();
        }
        return width;
    }

    /**
     * Returns the width {@code text} takes in {@code font}.
     */
    private static double measure(String text, Font font) {
        Text ruler = new Text(text);
        ruler.setFont(font);
        return ruler.getLayoutBounds().getWidth();
    }

    /**
     * Blinks the caret, which the field would otherwise do for a caret of its own.
     */
    private void blink() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, event -> caret.setOpacity(1)),
                new KeyFrame(Duration.millis(BLINK_MILLIS), event -> caret.setOpacity(0)),
                new KeyFrame(Duration.millis(BLINK_MILLIS * 2)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Returns the style class that colours {@code kind}.
     */
    private static String styleClassFor(CommandHighlighter.Kind kind) {
        return "command-" + kind.name().toLowerCase().replace('_', '-');
    }

    /**
     * Sets the command box style to use the default style.
     */
    private void setStyleToDefault() {
        commandTextField.getStyleClass().remove(ERROR_STYLE_CLASS);
        highlighted.getStyleClass().remove(COPY_ERROR_STYLE_CLASS);
    }

    /**
     * Sets the command box style to indicate a failed command.
     */
    private void setStyleToIndicateCommandFailure() {
        ObservableList<String> styleClass = commandTextField.getStyleClass();

        if (styleClass.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClass.add(ERROR_STYLE_CLASS);
        highlighted.getStyleClass().add(COPY_ERROR_STYLE_CLASS);
    }

    /**
     * Represents a function that can execute commands.
     */
    @FunctionalInterface
    public interface CommandExecutor {
        /**
         * Executes the command and returns the result.
         *
         * @see seedu.tab.logic.Logic#execute(String)
         */
        CommandResult execute(String commandText) throws CommandException, ParseException;
    }

}
