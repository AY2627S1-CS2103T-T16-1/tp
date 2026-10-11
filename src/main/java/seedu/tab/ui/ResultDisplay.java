package seedu.tab.ui;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

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

    /** What the area keeps for its own padding and its scroll bar when text is wrapped. */
    private static final double WRAPPING_ALLOWANCE = 26;

    @FXML
    private TextArea resultDisplay;

    /**
     * Creates a {@code ResultDisplay}, which stays out of the window until it has something to
     * report.
     */
    public ResultDisplay() {
        super(FXML);
        // a reply wraps at the width of the area, so a window being resized changes how many
        // rows the same reply needs
        resultDisplay.widthProperty().addListener((unused, old, width) ->
                fitToText(resultDisplay.getText()));
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
     * <p>The area takes the rows the reply needs rather than a fixed number of them, which is
     * what left a band of empty space under the command line. Past {@link #MAX_ROWS} it
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
        resultDisplay.setPrefRowCount((int) Math.clamp(rowsNeededFor(text), 1, MAX_ROWS));
    }

    /**
     * Returns how many rows {@code text} takes in the area.
     *
     * <p>Counting the lines of the reply counts only the breaks it was written with. A reply
     * is wrapped at the width of the area as well, and one written on a single line is the
     * common case: "Edited student: ..." arrives as one line, so the area took one row and
     * hid everything the first row had no space for.
     */
    private int rowsNeededFor(String text) {
        double wrapAt = resultDisplay.getWidth() - WRAPPING_ALLOWANCE;
        if (wrapAt <= 0) {
            // the area has not been laid out yet, so the breaks in the reply are all there is
            return (int) text.lines().count();
        }
        Text wrapped = new Text(text);
        wrapped.setFont(resultDisplay.getFont());
        wrapped.setWrappingWidth(wrapAt);
        Text oneRow = new Text("X");
        oneRow.setFont(resultDisplay.getFont());
        return (int) Math.ceil(wrapped.getLayoutBounds().getHeight()
                / oneRow.getLayoutBounds().getHeight());
    }


}
