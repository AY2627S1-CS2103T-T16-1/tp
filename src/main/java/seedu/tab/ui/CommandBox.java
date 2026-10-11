package seedu.tab.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
    private static final String FXML = "CommandBox.fxml";
    private static final int BLINK_MILLIS = 530;
    /** How much of the line is kept clear beyond the caret while scrolling. */
    private static final double CARET_MARGIN = 12;

    private final CommandExecutor commandExecutor;

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

        commandTextField.caretPositionProperty().addListener((unused, old, at) -> layOutColours());
        commandTextField.focusedProperty().addListener((unused, old, focused) -> layOutColours());
        commandTextField.selectionProperty().addListener((unused, old, range) -> layOutColours());
        // the line narrows when the window does, which can take a command that fitted and
        // make it wrap, so the decision is remade whenever the width changes
        commandTextField.widthProperty().addListener((unused, old, width) -> layOutColours());
        lane.widthProperty().addListener((unused, old, width) -> clipTheLane());
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
     * <p>The field keeps the caret, the selection and every key the User presses; only its own
     * text is transparent, so what is seen is the coloured copy underneath. The copy cannot
     * scroll with the field, so once the command outgrows the line the colours are dropped and
     * the field shows its own text again rather than sitting misaligned beneath it.
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
     * field scrolls to keep its caret visible and so does this: the same rule, so the two stay
     * together without the field having to report an offset it does not expose.
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
        double scrolled = content <= visible
                ? 0
                : Math.clamp(caretAt - visible + CARET_MARGIN, 0, content - visible);

        matchCaretToTheLine();
        double middle = lane.getHeight() / 2;
        showSelection(scrolled, middle);
        highlighted.relocate(-scrolled, middle - highlighted.getHeight() / 2);
        caret.relocate(caretAt - scrolled, middle - caret.getHeight() / 2);
        caret.setVisible(commandTextField.isFocused());
    }

    /**
     * Keeps the lane's clip the size of the lane, so a command longer than the line is cut off
     * at its edge rather than spilling over the rest of the window.
     */
    private void clipTheLane() {
        Rectangle clip = new Rectangle(lane.getWidth(), lane.getHeight());
        lane.setClip(clip);
        layOutColours();
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
     * both from a run of the text itself keeps them together whatever the font.
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
