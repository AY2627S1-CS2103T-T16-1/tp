package seedu.tab.ui;

import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import seedu.tab.commons.core.LogsCenter;

/**
 * Controller for a help page
 */
public class HelpWindow extends UiPart<Stage> {

    public static final String USERGUIDE_URL = "https://ay2627s1-cs2103t-t16-1.github.io/tp/UserGuide.html";
    public static final String HELP_MESSAGE = "Refer to the user guide: " + USERGUIDE_URL;

    private static final Logger logger = LogsCenter.getLogger(HelpWindow.class);
    private static final String FXML = "HelpWindow.fxml";

    @FXML
    private Button copyButton;

    @FXML
    private Label helpMessage;

    /**
     * Creates a new HelpWindow.
     *
     * @param root Stage to use as the root of the HelpWindow.
     */
    public HelpWindow(Stage root) {
        super(FXML, root);
        UiFonts.applyTo(root.getScene());
        helpMessage.setText(HELP_MESSAGE);
        fitToContent();
    }

    /**
     * Creates a new HelpWindow.
     */
    public HelpWindow() {
        this(new Stage());
    }

    /**
     * Shows the help window.
     * @throws IllegalStateException
     *     <ul>
     *         <li>
     *             if this method is called on a thread other than the JavaFX Application Thread.
     *         </li>
     *         <li>
     *             if this method is called during animation or layout processing.
     *         </li>
     *         <li>
     *             if this method is called on the primary stage.
     *         </li>
     *         <li>
     *             if {@code dialogStage} is already showing.
     *         </li>
     *     </ul>
     */
    public void show() {
        logger.fine("Showing help page about the application.");
        getRoot().show();
        // now that the window has a frame, its size can account for it
        fitToContent();
        getRoot().centerOnScreen();
    }

    /**
     * Sizes the window to what it draws.
     *
     * <p>A stage sizes itself to its scene before the stylesheets have dressed what is in
     * it, so the padding and the border a button takes from the theme arrive after the
     * window has been measured. The window cannot be resized, so what no longer fits would
     * stay cut off: the address read as "…/UserGuide.ht…" and the button as "Copy U…". The
     * row inside holds its own size as well. The scene is measured when the layout file is
     * read, which is before the address is put on the label, so it is measured against the
     * word "Label".
     */
    private void fitToContent() {
        Stage stage = getRoot();
        Parent content = stage.getScene().getRoot();
        content.applyCss();
        content.layout();
        // before the window is shown there is no frame to measure, and the scene is the
        // whole of it; afterwards the frame is whatever the window is wider than its scene
        double frameWidth = frameAround(stage.getWidth(), stage.getScene().getWidth());
        double frameHeight = frameAround(stage.getHeight(), stage.getScene().getHeight());
        stage.setWidth(content.prefWidth(-1) + frameWidth);
        stage.setHeight(content.prefHeight(-1) + frameHeight);
    }

    /**
     * Returns how much of {@code window} is frame rather than {@code scene}. A window that
     * has not been shown has no size at all to measure, and no frame around it yet either.
     */
    private static double frameAround(double window, double scene) {
        return Double.isNaN(window) || Double.isNaN(scene) ? 0 : Math.max(0, window - scene);
    }

    /**
     * Returns true if the help window is currently being shown.
     */
    public boolean isShowing() {
        return getRoot().isShowing();
    }

    /**
     * Hides the help window.
     */
    public void hide() {
        getRoot().hide();
    }

    /**
     * Focuses on the help window.
     */
    public void focus() {
        getRoot().requestFocus();
    }

    /**
     * Copies the URL to the user guide to the clipboard.
     */
    @FXML
    private void copyUrl() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent url = new ClipboardContent();
        url.putString(USERGUIDE_URL);
        clipboard.setContent(url);
    }
}
