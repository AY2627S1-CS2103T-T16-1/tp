package seedu.tab.ui;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

/**
 * A UI component that displays the result of a command execution.
 */
public class ResultDisplay extends UiPart<Region> {

    private static final String FXML = "ResultDisplay.fxml";

    /**
     * The most rows the area grows to before it scrolls, so that a long reply cannot push the
     * student list off the window while still being reachable.
     */
    private static final int MAX_ROWS = 6;

    @FXML
    private TextArea resultDisplay;

    /**
     * Creates a {@code ResultDisplay}, which stays out of the window until it has something to
     * report.
     */
    public ResultDisplay() {
        super(FXML);
        fitToText("");
    }

    public void setFeedbackToUser(String feedbackToUser) {
        requireNonNull(feedbackToUser);
        resultDisplay.setText(feedbackToUser);
        fitToText(feedbackToUser);
    }

    /**
     * Sizes the area to what it has to say, and takes it out of the window when that is
     * nothing.
     *
     * <p>The area takes a row per line of the reply rather than a fixed number of them, which
     * is what left a band of empty space under the command line. Past {@link #MAX_ROWS} it
     * scrolls rather than growing, and it stays a text area rather than becoming a label so
     * that a reply too long to show at once can still be scrolled to and selected.
     */
    private void fitToText(String text) {
        boolean hasSomethingToSay = !text.isBlank();
        getRoot().setManaged(hasSomethingToSay);
        getRoot().setVisible(hasSomethingToSay);
        if (!hasSomethingToSay) {
            return;
        }
        resultDisplay.setPrefRowCount((int) Math.clamp(text.lines().count(), 1, MAX_ROWS));
    }


}
