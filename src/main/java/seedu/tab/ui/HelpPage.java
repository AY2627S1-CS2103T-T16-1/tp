package seedu.tab.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import seedu.tab.logic.commands.HelpCommand;

/**
 * An in-window help pager controlled from the keyboard.
 * Navigation follows the more(1) conventions documented at https://man.openbsd.org/more.1.
 */
public class HelpPage extends UiPart<Region> {

    private static final String MESSAGE_RETURN_TO_COMMANDS = "Press q or Esc to return to the command box.";
    private static final String MESSAGE_GUIDE_COPIED = "User Guide URL copied.";
    private static final String END_MARKER = "(END)";
    private static final String PROGRESS_PREFIX = "--More-- (";
    private static final String PROGRESS_SUFFIX = "%)";
    private static final String LINE_HEIGHT_SAMPLE = "Ag";
    private static final String FXML = "HelpPage.fxml";
    private static final double HALF_PAGE = 0.5;
    private static final int PERCENT = 100;
    private static final String READY_MESSAGE = "Help mode: press q or Esc to return to commands.";

    private final Runnable closeHelp;

    @FXML
    private ScrollPane helpViewport;
    @FXML
    private TextFlow helpText;
    @FXML
    private Label helpProgress;
    @FXML
    private Label helpNotice;

    /**
     * Creates a pager that restores the command view when the user quits help.
     */
    public HelpPage(Runnable closeHelp) {
        super(FXML);
        this.closeHelp = closeHelp;
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        getRoot().addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
        helpText.getChildren().setAll(HelpText.color(HelpCommand.SHOWING_HELP_MESSAGE));
        helpNotice.setText(READY_MESSAGE);
        helpViewport.vvalueProperty().addListener((unused, old, value) -> updateProgress());
        helpViewport.viewportBoundsProperty().addListener((unused, old, bounds) -> updateProgress());
        helpText.layoutBoundsProperty().addListener((unused, old, bounds) -> updateProgress());
        updateProgress();
    }

    /**
     * Opens help at the beginning with immediate keyboard navigation.
     */
    public void show() {
        helpNotice.setText(READY_MESSAGE);
        helpViewport.setVvalue(0);
        getRoot().requestFocus();
    }

    /**
     * Handles navigation by key code, including Enter before its typed newline.
     */
    private void handleKeyPressed(KeyEvent event) {
        if (event.isAltDown() || event.isMetaDown() || event.isControlDown()) {
            return;
        }
        switch (event.getCode()) {
            case PAGE_DOWN -> moveBy(computePageHeight());
            case PAGE_UP -> moveBy(-computePageHeight());
            case DOWN, ENTER -> moveBy(measureLineHeight());
            case UP -> moveBy(-measureLineHeight());
            case HOME -> helpViewport.setVvalue(0);
            case END -> helpViewport.setVvalue(1);
            case ESCAPE -> closeHelp.run();
            default -> {
                return;
            }
        }
        event.consume();
    }

    /**
     * Handles pager letters on KEY_TYPED so quitting cannot type a stray q into the command box.
     */
    private void handleKeyTyped(KeyEvent event) {
        if (event.isAltDown() || event.isMetaDown() || event.isControlDown()) {
            return;
        }
        // Enter and macOS F1 can send a newline or empty typed event after opening help.
        if (event.getCharacter().isEmpty()
                || "\r".equals(event.getCharacter()) || "\n".equals(event.getCharacter())) {
            event.consume();
            return;
        }
        switch (event.getCharacter()) {
            case " ", "f" -> moveBy(computePageHeight());
            case "b" -> moveBy(-computePageHeight());
            case "j" -> moveBy(measureLineHeight());
            case "k" -> moveBy(-measureLineHeight());
            case "d" -> moveBy(computePageHeight() * HALF_PAGE);
            case "u" -> moveBy(-computePageHeight() * HALF_PAGE);
            case "g" -> helpViewport.setVvalue(0);
            case "G" -> helpViewport.setVvalue(1);
            case "q" -> closeHelp.run();
            case "c" -> copyGuideUrl();
            default -> {
                helpNotice.setText(MESSAGE_RETURN_TO_COMMANDS);
                return;
            }
        }
        event.consume();
    }

    private void copyGuideUrl() {
        ClipboardContent content = new ClipboardContent();
        content.putString(HelpCommand.USER_GUIDE_URL);
        Clipboard.getSystemClipboard().setContent(content);
        helpNotice.setText(MESSAGE_GUIDE_COPIED);
    }

    private double measureLineHeight() {
        Text line = new Text(LINE_HEIGHT_SAMPLE);
        // the reference is spans of Text and nothing else, and they carry one monospaced font,
        // so the first of them measures a line of the block
        line.setFont(((Text) helpText.getChildren().getFirst()).getFont());
        return line.getLayoutBounds().getHeight();
    }

    private double computePageHeight() {
        // Keep one line of context between screenfuls, including when text wraps after resizing.
        return Math.max(measureLineHeight(), helpViewport.getViewportBounds().getHeight() - measureLineHeight());
    }

    private double computeOverflowHeight() {
        return Math.max(0, helpText.getHeight() - helpViewport.getViewportBounds().getHeight());
    }

    private void moveBy(double pixels) {
        double overflow = computeOverflowHeight();
        if (overflow > 0) {
            helpViewport.setVvalue(Math.clamp(helpViewport.getVvalue() + pixels / overflow, 0, 1));
        }
    }

    private void updateProgress() {
        if (computeOverflowHeight() == 0 || helpViewport.getVvalue() >= 1) {
            helpProgress.setText(END_MARKER);
            return;
        }
        double bottom = helpViewport.getVvalue() * computeOverflowHeight()
                + helpViewport.getViewportBounds().getHeight();
        int percent = (int) Math.clamp(PERCENT * bottom / helpText.getHeight(), 0, PERCENT - 1);
        helpProgress.setText(PROGRESS_PREFIX + percent + PROGRESS_SUFFIX);
    }
}
