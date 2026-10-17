package seedu.tab.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import seedu.tab.commons.util.StringUtil;

/**
 * Text helpers for the parts of the window that show a value too long for the room they
 * have: a card's name and tags, and the tags the summary panel lists.
 */
final class UiText {

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
     * Puts {@code value} on {@code label} and names the whole of it on hover whenever the
     * label is drawn narrower than the value needs.
     *
     * <p>Cutting by a count of characters catches only one of the two ways a value is cut.
     * A value under the count is still cut by the width the label is given, and a tag such
     * as "attendance-low" beside a long name reads as "attendan\u2026", and a tag the panel
     * lists as "CS2103T-TUTORI\u2026", with nothing in either to say what it was.
     */
    static void showShortenedToFit(Label label, String value, int limit) {
        showShortened(label, value, limit);
        nameWhatIsCutAsItIsDrawn(label, value);
    }

    private static void nameWhatIsCutAsItIsDrawn(Label label, String value) {
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
        String shown = StringUtil.firstCharacters(value, limit);
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
     * Puts {@code value} on {@code label}, cut from its middle to {@code limit} characters with
     * an ellipsis stood in for what was taken, if it runs longer. Both ends of the value stay
     * on the label, so two values that agree over their opening still read as two, and the
     * whole of it is a hover away as the label's tooltip.
     *
     * <p>The ellipsis takes a place of its own, so a value one character over {@code limit} is
     * no shorter cut than whole. A value that close to the count is shown whole rather than
     * hiding a character behind a mark that buys no room back.
     */
    static void showMiddleShortened(Label label, String value, int limit) {
        if (StringUtil.firstCharacters(value, limit + 1).equals(value)) {
            label.setText(value);
            return;
        }
        int closing = Math.min(CLOSING_CHARACTERS, limit / 2);
        String opening = StringUtil.firstCharacters(value, limit - closing);
        // the ellipsis reads as part of the words it joins, so it sits against both of them
        // rather than off behind a space either cut happened to land on
        label.setText(opening.stripTrailing() + ELLIPSIS
                + StringUtil.lastCharacters(value, closing).stripLeading());
        label.setTooltip(new Tooltip(value));
    }

    /**
     * Puts {@code value} on {@code label} cut from its middle, and names the whole of it on
     * hover whenever the label shows less than all of it: cut to the count, or drawn narrower
     * than the text it holds needs.
     *
     * <p>A value within the count is cut all the same by the room its row has left over, and
     * the label's own ellipsis says nothing about what it took. A name is worse off for that
     * than a tag is, since a card holds nothing else that would identify the student.
     */
    static void showMiddleShortenedToFit(Label label, String value, int limit) {
        showMiddleShortened(label, value, limit);
        nameWhatIsCutAsItIsDrawn(label, value);
    }
}
