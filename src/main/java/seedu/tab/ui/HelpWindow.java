package seedu.tab.ui;

import java.util.logging.Logger;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
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
        fitToContent();
        // Native frame sizes can arrive during the first pulse after the window is shown.
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                stop();
                Platform.runLater(() -> {
                    fitToContent();
                    getRoot().sizeToScene();
                    getRoot().centerOnScreen();
                });
            }
        }.start();
    }

    /**
     * Sizes the window to its styled content and the frame measured so far. Once native
     * dimensions settle, the deferred fit also asks JavaFX to size the scene itself.
     */
    private void fitToContent() {
        Stage stage = getRoot();
        Parent content = stage.getScene().getRoot();
        content.applyCss();
        content.layout();
        // never smaller than what it holds, now that it may be resized
        stage.setMinWidth(content.prefWidth(-1));
        stage.setMinHeight(content.prefHeight(-1));
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
    static double frameAround(double window, double scene) {
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
