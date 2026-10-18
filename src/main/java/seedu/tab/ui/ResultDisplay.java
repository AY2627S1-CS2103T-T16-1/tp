package seedu.tab.ui;

import static java.util.Objects.requireNonNull;

import javafx.application.Platform;
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

    @FXML
    private TextArea resultDisplay;

    /** The text node the area draws its reply in, once the area has a skin to draw it. */
    private Text drawn;

    /** Whether an answer is already on its way, so that one pulse does not ask twice. */
    private boolean isCounting;

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
        watchTheTextItDraws();
        countTheRowsAgain();
    }

    /**
     * Follows the text the area draws, so that the rows are counted again whenever the reply
     * is wrapped differently.
     *
     * <p>Listening to the area's own width instead counts them against a width that has not
     * been laid out yet, and leaves the area as tall as a reply that has since been widened
     * back onto one line.
     */
    private void watchTheTextItDraws() {
        if (drawn != null) {
            return;
        }
        drawn = (Text) resultDisplay.lookup(".text");
        if (drawn == null) {
            // the area has no skin yet; the next reply finds one and starts following it
            return;
        }
        drawn.layoutBoundsProperty().addListener((unused, old, bounds) -> countTheRowsAgain());
    }

    /**
     * Counts the rows the reply takes and gives the area that many, on the pulse after this
     * one.
     *
     * <p>The count is asked for from inside a layout pass, because that is when the area has
     * just wrapped the reply and can say where. Changing the rows there changes how tall the
     * area wants to be, and the panes around it have already been laid out by then: the area
     * is left at the height it had, which is why the first reply came up several rows too
     * tall and stayed that way, and why widening the window did not bring a wrapped reply
     * back onto one line. Asking again on the next pulse lets the window lay out around the
     * answer.
     */
    private void countTheRowsAgain() {
        if (isCounting) {
            return;
        }
        isCounting = true;
        Platform.runLater(() -> {
            isCounting = false;
            int rows = (int) Math.clamp(rowsNeededFor(resultDisplay.getText()), 1, MAX_ROWS);
            if (rows != resultDisplay.getPrefRowCount()) {
                resultDisplay.setPrefRowCount(rows);
            }
        });
    }

    /**
     * Returns how many rows {@code text} takes in the area.
     *
     * <p>Counting the lines of the reply counts only the breaks it was written with. A reply
     * is wrapped at the width of the area as well, and one written on a single line is the
     * common case: "Edited student: ..." arrives as one line, so the area took one row and
     * hid everything the first row had no space for.
     *
     * <p>Where it wraps is the area's own business, so the area is asked. Taking its width
     * and subtracting a guess at its padding and its scroll bar put the wrap a few pixels
     * from where the area actually puts it, and a reply that came within those few pixels of
     * the edge was given a row it did not use. A width of nothing needs no guard of its own:
     * JavaFX reads that as "do not wrap", which gives the lines the reply was written with,
     * the same answer as having nothing drawn at all.
     */
    private int rowsNeededFor(String text) {
        long written = Math.max(1, text.lines().count());
        if (drawn == null) {
            // nothing has been drawn yet, so the breaks in the reply are all there is
            return (int) written;
        }

        Text measured = new Text(text);
        measured.setFont(drawn.getFont());
        // a row is as tall as a row of this reply, measured before it is wrapped. Measuring
        // a letter of its own instead compares this reply's tallest glyph against that
        // letter's: in Inter a line of a reply stands 15.0 against the 14.52 of an "X", so
        // every one-line reply rounded up to two rows and carried an empty one
        double rowHeight = measured.getLayoutBounds().getHeight() / written;
        measured.setWrappingWidth(drawn.getWrappingWidth());
        return (int) Math.round(measured.getLayoutBounds().getHeight() / rowHeight);
    }
}
