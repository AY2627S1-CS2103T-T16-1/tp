package seedu.tab.ui;

import java.util.function.BooleanSupplier;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Path;
import javafx.scene.text.TextFlow;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.commands.CommandResult;
import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    /** Marks the colored copy, which is the text the User actually sees. */
    private static final String COPY_ERROR_STYLE_CLASS = "command-error";
    private static final String FXML = "CommandBox.fxml";

    private final CommandHistory history = new CommandHistory();
    private final CommandExecutor commandExecutor;
    private final BooleanSupplier isAwaitingConfirmation;

    /** Paints what the User reads, since a text field cannot color its own content. */
    private final HighlightedTextOverlay overlay;

    @FXML
    private TextField commandTextField;

    @FXML
    private TextFlow highlighted;

    @FXML
    private Region caret;

    @FXML
    private Pane lane;

    @FXML
    private Path selection;

    /**
     * Creates a {@code CommandBox} with the given {@code CommandExecutor}.
     */
    public CommandBox(CommandExecutor commandExecutor) {
        this(commandExecutor, () -> false);
    }

    /**
     * Creates a command box that handles confirmation replies separately from reusable commands.
     */
    public CommandBox(CommandExecutor commandExecutor, BooleanSupplier isAwaitingConfirmation) {
        super(FXML);
        this.commandExecutor = commandExecutor;
        this.isAwaitingConfirmation = isAwaitingConfirmation;
        commandTextField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleHistoryKeyPressed);
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> setStyleToDefault());
        overlay = new HighlightedTextOverlay(commandTextField, highlighted, caret, lane, selection);
        commandTextField.textProperty().addListener((unused, old, typed) -> overlay.recolor(typed));
        overlay.recolor(commandTextField.getText());
        // every task is meant to be reachable from the keyboard alone, so the line the User
        // types into is where the caret starts rather than somewhere they have to click
        Platform.runLater(commandTextField::requestFocus);
    }

    /** Focuses the input field with the unfinished command unselected and the caret at its end. */
    public void focusDraftAtEnd() {
        commandTextField.requestFocus();
        commandTextField.end();
    }

    /**
     * Handles the Enter button pressed event.
     */
    @FXML
    private void handleCommandEntered() {
        String commandText = commandTextField.getText();
        boolean isConfirmationReply = isAwaitingConfirmation.getAsBoolean();
        if (StringUtil.isBlankInput(commandText) && !isConfirmationReply) {
            return;
        }

        if (isConfirmationReply) {
            history.resetNavigation();
        } else {
            history.add(commandText);
        }
        try {
            commandExecutor.execute(commandText);
            commandTextField.setText("");
        } catch (CommandException | ParseException e) {
            setStyleToIndicateCommandFailure();
        }
    }

    /** Recalls a session submission while preserving ordinary modified-arrow shortcuts. */
    private void handleHistoryKeyPressed(KeyEvent event) {
        if (event.isAltDown() || event.isControlDown() || event.isMetaDown() || event.isShiftDown()) {
            return;
        }
        String current = commandTextField.getText();
        String recalled;
        switch (event.getCode()) {
            case UP:
                recalled = history.recallPrevious(current);
                break;
            case DOWN:
                recalled = history.recallNext(current);
                break;
            default:
                return;
        }
        commandTextField.setText(recalled);
        commandTextField.positionCaret(recalled.length());
        event.consume();
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
        ObservableList<String> styleClasses = commandTextField.getStyleClass();

        if (styleClasses.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClasses.add(ERROR_STYLE_CLASS);
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
