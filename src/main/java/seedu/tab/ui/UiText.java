package seedu.tab.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import seedu.tab.commons.util.StringUtil;

/**
 * Text helpers for the parts of the window that show a value too long for the room they
 * have: a card's name and tags, and the tags the summary panel lists.
 */
final class UiText {

    // Subpixel rounding should not give an otherwise fully visible label a tooltip.
    private static final double TRUNCATION_TOLERANCE = 0.5;

    private static final String ELLIPSIS = "…";

    /**
     * How many characters a cut from the middle keeps from the close of a value. Two names
     * collide where a reader starts reading them, so what tells them apart is their close:
     * nine characters hold a final word with the particle in front of it, as in "bin Ahmad".
     * The rest of the count goes to the opening, which is what a reader scans the list by.
     */
    private static final int CLOSING_CHARACTERS = 9;

    private UiText() {} // this class only holds helpers

    /**
     * Shows {@code value} in full, with a tooltip whenever the available width cuts it.
     */
    static void showToFit(Label label, String value) {
        label.setText(value);
        observeTruncation(label, value);
    }

    /**
     * Observes both text and layout changes so the tooltip follows the rendered truncation.
     */
    private static void observeTruncation(Label label, String value) {
        label.widthProperty().addListener((unused, old, width) -> updateTooltip(label, value));
        // the width is known before the text that goes in it: a label that cuts its own text
        // to the room it has does so while laying out, after the width has changed, so a
        // check made on the width alone reads the text the label is about to replace
        label.textProperty().addListener((unused, old, text) -> updateTooltip(label, value));
        updateTooltip(label, value);
    }

    /**
     * Shows the full value only when the displayed label is shortened or clipped.
     */
    private static void updateTooltip(Label label, String value) {
        boolean isCut = label.getWidth() > 0 && label.getWidth() < label.prefWidth(-1) - TRUNCATION_TOLERANCE;
        if (isCut || !label.getText().equals(value)) {
            label.setTooltip(new Tooltip(value));
            return;
        }
        label.setTooltip(null);
    }

    /**
     * Returns {@code value} cut from its middle to {@code limit} characters with an ellipsis
     * stood in for what was taken, or {@code value} itself when it is already that short.
     *
     * <p>The ellipsis takes a place of its own, so a value one character over {@code limit} is
     * no shorter cut than whole. A value that close to the count is left whole rather than
     * hiding a character behind a mark that buys no room back.
     */
    static String shortenMiddle(String value, int limit) {
        if (StringUtil.getFirstCharacters(value, limit + 1).equals(value)) {
            return value;
        }
        int closing = Math.min(CLOSING_CHARACTERS, limit / 2);
        String opening = StringUtil.getFirstCharacters(value, limit - closing);
        // the ellipsis reads as part of the words it joins, so it sits against both of them
        // rather than off behind a space either cut happened to land on
        return opening.stripTrailing() + ELLIPSIS
                + StringUtil.getLastCharacters(value, closing).stripLeading();
    }
}
