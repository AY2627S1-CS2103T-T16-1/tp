package seedu.tab.ui;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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

    private final CommandExecutor commandExecutor;

    @FXML
    private TextField commandTextField;

    @FXML
    private TextFlow highlighted;

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

        boolean fits = highlighted.prefWidth(-1) <= commandTextField.getWidth();
        highlighted.setVisible(fits);
        commandTextField.pseudoClassStateChanged(COLOURED, fits);
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
