package seedu.tab.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/**
 * Text helpers shared by the parts of a student card.
 */
final class UiText {

    private static final String ELLIPSIS = "…";

    private UiText() {} // this class only holds helpers

    /**
     * Puts {@code value} on {@code label}, cut to {@code limit} characters with an ellipsis if
     * it runs longer. What was cut is still readable: the label carries the whole value as its
     * tooltip, so a card summarises without the record becoming unreachable.
     */
    static void showShortened(Label label, String value, int limit) {
        if (value.length() <= limit) {
            label.setText(value);
            return;
        }
        // the ellipsis reads as part of the word it follows, so it does not sit off
        // behind a space the cut happened to land on
        label.setText(value.substring(0, limit).stripTrailing() + ELLIPSIS);
        label.setTooltip(new Tooltip(value));
    }
}
