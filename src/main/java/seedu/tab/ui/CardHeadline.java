package seedu.tab.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/**
 * The first line of a student card: the name, with the tags against it.
 *
 * <p>A plain row shares out what it cannot fit between the two, so a long name and a row of
 * chips each lose about half of what they asked for and neither reads. This gives way in one
 * order instead. The name is drawn at the length the card cuts it to while the row has space
 * for it, so the same name breaks off at the same place from one card to the next. Below
 * that, the name gives way and is cut again with an ellipsis, down to the point where it
 * would crowd out the tags: a card that has tags always shows at least the start of one.
 */
public class CardHeadline extends Pane {

    /** The gap between the name and the tags beside it. */
    private static final double SPACING = 8;
    /**
     * Enough of the name for a few letters of it, which the tags beside it never take. A
     * name is what tells one card from another, so it gives way last.
     */
    private static final double READABLE_NAME_WIDTH = 64;
    /** Enough of the tags to read the start of one, kept even beside a very long name. */
    private static final double READABLE_TAGS_WIDTH = TagRow.READABLE_CHIP_WIDTH;

    private Label name;
    private TagRow tags;

    @Override
    protected double computePrefWidth(double height) {
        read();
        return name.prefWidth(height) + SPACING + tags.prefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width) {
        read();
        return Math.max(name.prefHeight(width), tags.prefHeight(width));
    }

    @Override
    protected double computeMinWidth(double height) {
        read();
        return name.minWidth(height) + SPACING + tags.minWidth(height);
    }

    @Override
    protected void layoutChildren() {
        read();
        double forTheTags = roomForTheTags();
        double leftOver = Math.max(0, getWidth() - forTheTags - (forTheTags > 0 ? SPACING : 0));
        // the name is drawn no wider than its own text, so the tags sit against the name
        // rather than out at the edge of whatever room the line happens to have
        double forTheName = Math.min(name.prefWidth(-1), leftOver);

        layInRow(name, 0, forTheName);
        layInRow(tags, forTheName + SPACING, forTheTags);
    }

    /**
     * Returns how much of the row the tags are drawn in: what they ask for while the name has
     * room for all of itself, and enough for one chip to be read once it has not.
     */
    private double roomForTheTags() {
        double wanted = tags.prefWidth(-1);
        if (wanted == 0) {
            return 0;
        }
        double spare = getWidth() - name.prefWidth(-1) - SPACING;
        // enough for a chip and a count of the tags it leaves out, so that a card beside a
        // long name still says there are more rather than showing one chip and nothing else
        double floor = Math.min(wanted, tags.minWidth(-1));
        // but never so much that the name is left with nothing: a card whose name reads as
        // an ellipsis says less about the student than one whose tag count is missing, and
        // on a narrow window every flagged card with two tags came out that way
        double sparedForTheName = getWidth() - READABLE_NAME_WIDTH - SPACING;
        return Math.clamp(spare, Math.min(floor, Math.max(0, sparedForTheName)), wanted);
    }

    private void layInRow(Region child, double x, double width) {
        double height = child.prefHeight(-1);
        child.resizeRelocate(x, Math.round((getHeight() - height) / 2), width, height);
    }

    /**
     * Picks the name and the tags out of what the card put in the row, by what they are
     * rather than by the order they were written in, so that the row still lays itself out
     * if the two are ever swapped round in the layout file.
     */
    private void read() {
        if (name != null) {
            return;
        }
        name = firstOf(Label.class);
        tags = firstOf(TagRow.class);
    }

    private <T> T firstOf(Class<T> kind) {
        return getChildren().stream()
                .filter(kind::isInstance)
                .map(kind::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "a card's first line needs a " + kind.getSimpleName()));
    }
}
