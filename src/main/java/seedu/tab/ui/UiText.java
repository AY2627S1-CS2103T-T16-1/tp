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
     * Puts {@code value} on {@code label} and names the whole of it on hover whenever the
     * label is drawn narrower than the value needs.
     *
     * <p>Cutting by a count of characters catches only one of the two ways a value is cut.
     * A value under the count is still cut by the width the label is given, and a tag such
     * as "CS2103T-TUTORIALS" then read as "CS2103T-TUTORI…" with nothing to say what it was.
     */
    static void showShortenedToFit(Label label, String value, int limit) {
        showShortened(label, value, limit);
        label.widthProperty().addListener((unused, old, width) -> nameWhatIsCut(label, value));
        nameWhatIsCut(label, value);
    }

    private static void nameWhatIsCut(Label label, String value) {
        boolean isCut = label.getWidth() > 0 && label.getWidth() < label.prefWidth(-1) - 0.5;
        if (isCut || !label.getText().equals(value)) {
            label.setTooltip(new Tooltip(value));
            return;
        }
        label.setTooltip(null);
    }

    /**
     * Puts {@code value} on {@code label}, cut to {@code limit} characters with an ellipsis if
     * it runs longer. What was cut is still readable: the label carries the whole value as its
     * tooltip, so a card summarises without the record becoming unreachable.
     */
    static void showShortened(Label label, String value, int limit) {
        String shown = firstCharacters(value, limit);
        if (shown.equals(value)) {
            label.setText(value);
            return;
        }
        // the ellipsis reads as part of the word it follows, so it does not sit off
        // behind a space the cut happened to land on
        label.setText(shown.stripTrailing() + ELLIPSIS);
        label.setTooltip(new Tooltip(value));
    }

    /**
     * Returns the first {@code limit} characters of {@code value}, counted as a reader counts
     * them rather than as Java stores them.
     *
     * <p>Cutting the string at an index cuts it between the two halves of an emoji, or
     * between a letter and the accent that belongs to it, and what is left is drawn as a
     * broken glyph. A name or a tag may hold either. Counting them the same way also keeps a
     * value that is long in the way Java stores it, and short in the way it reads, from
     * being marked as cut when nothing was taken off it.
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
