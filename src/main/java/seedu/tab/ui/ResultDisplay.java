package seedu.tab.ui;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * A UI component that displays the result of a command execution.
 */
public class ResultDisplay extends UiPart<Region> {

    private static final String FXML = "ResultDisplay.fxml";

    @FXML
    private Label resultDisplay;

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
     * Takes the line out of the window when there is nothing to report.
     *
     * <p>A label is used rather than a text area because a text area reserves room for a fixed
     * number of rows whatever it holds, which left a band of empty space under the command
     * line. A label is exactly as tall as what it has to say, and wraps rather than scrolling,
     * so a command reporting several bad fields hides none of them.
     */
    private void fitToText(String text) {
        boolean hasSomethingToSay = !text.isBlank();
        getRoot().setManaged(hasSomethingToSay);
        getRoot().setVisible(hasSomethingToSay);
    }

}
