package seedu.tab.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
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
    /** Set while the coloured copy is showing, which is when the field hides its own text. */
    private static final PseudoClass COLOURED = PseudoClass.getPseudoClass("coloured");
    private static final int BLINK_MILLIS = 530;

    private final CommandExecutor commandExecutor;

    @FXML
    private TextField commandTextField;

    @FXML
    private TextFlow highlighted;

    @FXML
    private Region caret;

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

        commandTextField.caretPositionProperty().addListener((unused, old, at) -> placeCaret());
        commandTextField.textProperty().addListener((unused, old, typed) -> placeCaret());
        commandTextField.focusedProperty().addListener((unused, old, focused) -> placeCaret());
        // the line narrows when the window does, which can take a command that fitted and
        // make it wrap, so the decision is remade whenever the width changes
        commandTextField.widthProperty().addListener((unused, old, width) -> decideWhetherColoursFit());
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
        decideWhetherColoursFit();
    }

    /**
     * Shows the coloured copy only while it fits on one line beside the field.
     *
     * <p>The copy cannot scroll with the field, so once the command outgrows the line the
     * field shows its own text again. The hidden copy is unmanaged as well as invisible: a
     * TextFlow wraps, so a long command left managed keeps asking for the height of every
     * wrapped line and the command box grows to match. The caret goes with it, because the
     * field draws its own once its text is visible again, and a second one placed without
     * regard to the field's scrolling would sit in the wrong place.
     */
    private void decideWhetherColoursFit() {
        boolean fits = highlighted.prefWidth(-1) <= commandTextField.getWidth();
        highlighted.setVisible(fits);
        highlighted.setManaged(fits);
        caret.setManaged(fits);
        commandTextField.pseudoClassStateChanged(COLOURED, fits);
        placeCaret();
    }

    /**
     * Moves the caret to the character the User is at.
     *
     * <p>The run of text before the caret is measured in the very font the coloured copy is
     * drawn in. Counting characters and multiplying by one character's width looks simpler and
     * is wrong: it assumes the field and the copy resolved to the same font, and when they do
     * not the caret drifts further from the text with every character typed.
     */
    private void placeCaret() {
        caret.setVisible(commandTextField.isFocused() && highlighted.isVisible());

        caret.setTranslateX(widthBefore(commandTextField.getCaretPosition()));
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
