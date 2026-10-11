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
    /** Beyond this the area scrolls, rather than pushing the student list off the window. */
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
     * Sizes the area to {@code text}, and takes it out of the window when there is nothing to
     * report. A fixed height left a gap under the command line before the first command and
     * after a result shorter than the space reserved for it.
     */
    private void fitToText(String text) {
        boolean hasSomethingToSay = !text.isBlank();
        getRoot().setManaged(hasSomethingToSay);
        getRoot().setVisible(hasSomethingToSay);
        resultDisplay.setPrefRowCount(Math.clamp(text.lines().count(), 1, MAX_ROWS));
    }

}
