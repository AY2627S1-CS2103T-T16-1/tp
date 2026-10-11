package seedu.tab.ui;

import java.text.BreakIterator;

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
        label.setText(firstCharacters(value, limit).stripTrailing() + ELLIPSIS);
        label.setTooltip(new Tooltip(value));
    }

    /**
     * Returns the first {@code limit} characters of {@code value}, counted as a reader counts
     * them rather than as Java stores them.
     *
     * <p>Cutting the string at an index cuts it between the two halves of an emoji, or
     * between a letter and the accent that belongs to it, and what is left is drawn as a
     * broken glyph. A name or a tag may hold either.
     */
    private static String firstCharacters(String value, int limit) {
        BreakIterator characters = BreakIterator.getCharacterInstance();
        characters.setText(value);
        int end = characters.first();
        for (int counted = 0; counted < limit; counted++) {
            int next = characters.next();
            if (next == BreakIterator.DONE) {
                return value;
            }
            end = next;
        }
        return value.substring(0, end);
    }
}
