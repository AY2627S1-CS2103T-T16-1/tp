package seedu.tab.ui;

import java.util.function.BooleanSupplier;

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
    private final BooleanSupplier canSubmitEmptyReply;
    private final ChangeListener<Number> theFieldScrolled = (unused, old, now) -> layOutColours();

    /** The node the field draws its own text in, while the copy is following it. */
    private Text followed;

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
        this(commandExecutor, () -> false);
    }

    /**
     * Creates a command box that can submit an empty reply while confirmation is pending.
     */
    public CommandBox(CommandExecutor commandExecutor, BooleanSupplier canSubmitEmptyReply) {
        super(FXML);
        this.commandExecutor = commandExecutor;
        this.canSubmitEmptyReply = canSubmitEmptyReply;
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
        if (commandText.isEmpty() && !canSubmitEmptyReply.getAsBoolean()) {
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
     * Lays the coloured copy out on one line, where the field is drawing its own text.
     *
     * <p>A TextFlow wraps at the width it is given, which would turn a long command into
     * several lines and grow the box. It is sized to its content instead and the lane around
     * it is clipped, so the copy runs off the edge the way the field's own text does. Which
     * part of a long command is in view is the field's to decide, and the copy is drawn
     * wherever the field has decided rather than scrolled by any rule of its own.
     */
    private void layOutColours() {
        followTheFieldsText();
        // JavaFX moves the caret before it reports the new text, so this runs once against
        // the copy of the previous command. The recolour that follows puts it right.
        if (!copyMatchesField()) {
            return;
        }

        double content = widthBefore(commandTextField.getLength());
        // the copy is sized to the command rather than to the lane holding it, because a
        // TextFlow wraps at the width it is given: sized to the lane, a long command came
        // out on several lines and made the box grow down the window.
        highlighted.setPrefWidth(content);
        highlighted.autosize();
        caret.autosize();

        double caretAt = widthBefore(commandTextField.getCaretPosition());
        matchCaretToTheLine();

        // where the field is drawing its own text, which is the one thing the copy has to
        // agree with: the field decides which character a click lands on, and it scrolls by
        // rules of its own that differ between typing, deleting, cutting and undoing. Every
        // attempt to work them out again here has been wrong in some case or other.
        double drawnFrom = whereTheFieldDrawsItsText();
        double middle = lane.getHeight() / 2;
        showSelection(-drawnFrom, middle);
        highlighted.relocate(drawnFrom, middle - highlighted.getHeight() / 2);
        caret.relocate(inTheLine(drawnFrom + caretAt - caret.getWidth() / 2),
                middle - caret.getHeight() / 2);
        caret.setVisible(commandTextField.isFocused());
    }

    /**
     * Returns where the field draws its own text, in the lane's own terms.
     *
     * <p>The field and the lane sit on each other, so the copy belongs wherever the field's
     * text is. Asking the field means the two can never drift: it insets its text by half a
     * caret and it re-scrolls on rules of its own, which differ between typing, deleting and
     * cutting and between JavaFX releases, and none of that has to be worked out again here.
     *
     * <p>Before the field has a skin there is nothing to ask, so the copy falls back to the
     * inset the field will use.
     */
    private double whereTheFieldDrawsItsText() {
        Text drawn = fieldText();
        return drawn == null ? halfACaret() : drawn.getLayoutX() + drawn.getTranslateX();
    }

    /**
     * Returns where a caret wanting to be drawn from {@code left} is drawn instead.
     *
     * <p>The caret is centred on the point it marks, as the field's own caret is, except at
     * either edge of the line, where half of it would fall outside and the lane's clip would
     * take that half away. How far in from the right edge a field scrolls the end of a
     * command differs between JavaFX releases, so a caret there lands outside the line on
     * one of them and is never seen.
     */
    private double inTheLine(double left) {
        return Math.clamp(left, 0, Math.max(0, lane.getWidth() - caret.getWidth()));
    }

    /**
     * Keeps the copy following the node the field draws its own text in.
     *
     * <p>The field's skin moves that node on its own account, after a delete, a replace or a
     * change of width, and long after the caret and the text it was told about have settled.
     * Nothing else says that it has, so the copy would be left where the command used to be
     * drawn and a click would land on the character the command used to have there.
     *
     * <p>The node is followed rather than remembered: an empty line shows the prompt, which
     * the field draws in a node of the same kind, so which node carries the command changes
     * with the command itself, and a listener left on the one before it stops firing.
     */
    private void followTheFieldsText() {
        Text drawn = fieldText();
        if (drawn == followed) {
            return;
        }
        if (followed != null) {
            followed.layoutXProperty().removeListener(theFieldScrolled);
            followed.translateXProperty().removeListener(theFieldScrolled);
        }
        followed = drawn;
        if (drawn != null) {
            drawn.layoutXProperty().addListener(theFieldScrolled);
            drawn.translateXProperty().addListener(theFieldScrolled);
        }
    }

    /**
     * Returns the node the field draws its own text in, or null before it has a skin.
     *
     * <p>An empty line shows the prompt, which the field draws in a node of the same kind,
     * so the one carrying the command is the one whose text is the command.
     */
    private Text fieldText() {
        return commandTextField.lookupAll(".text").stream()
                .filter(Text.class::isInstance)
                .map(Text.class::cast)
                .filter(text -> text.getText().equals(commandTextField.getText()))
                .findFirst()
                .orElse(null);
    }


    /**
     * Returns the inset the field draws its own text at, which the copy has to match.
     *
     * <p>A text field leaves half a caret's width at each edge, so that a caret at either
     * end of the command is drawn rather than cut in half by the edge. The copy is drawn
     * behind the field's own text, so it is drawn at the same inset.
     */
    private double halfACaret() {
        return Math.max(caret.getWidth(), caret.prefWidth(-1)) / 2;
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
    private void showSelection(double drawnFrom, double middle) {
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
        selection.relocate(from - drawnFrom, middle - caret.getHeight() / 2);
    }

    /**
     * Gives the caret the height of the text beside it.
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
