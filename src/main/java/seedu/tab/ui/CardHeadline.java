package seedu.tab.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/**
 * The first line of a student card: the name, with the tags against it.
 *
 * <p>The name and tags show their full values when they fit. On a crowded row, the name
 * keeps a readable allowance while the tags collapse, yielding some of it for a complete
 * count. Any space left by whole chips or a count is returned to the name, which shortens
 * itself only to the width still available.
 */
public class CardHeadline extends Pane {

    /** The gap between the name and the tags beside it. */
    static final double SPACING = 8;
    /**
     * Preferred room for a readable start of the name. A complete tag count may take
     * some of it, while the name retains space for a character from each end.
     */
    static final double READABLE_NAME_WIDTH = 64;

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
        double forTheTags = tags.fittedWidth(roomForTheTags());
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
        // Prefer a readable name, but allow it to yield enough room for a complete
        // count across fonts. At extreme widths, keep some name letters visible.
        double countWidth = Math.min(tags.getCountWidth(),
                Math.max(0, getWidth() - Math.ceil(name.minWidth(-1)) - SPACING));
        double sparedForTheName = Math.max(countWidth, getWidth() - READABLE_NAME_WIDTH - SPACING);
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
