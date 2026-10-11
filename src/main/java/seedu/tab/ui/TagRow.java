package seedu.tab.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/**
 * A row of tag chips that shows as many whole chips as the row has room for, and counts the
 * rest in a trailing chip.
 *
 * <p>A plain row would instead share the shortfall among every chip, so a card with several
 * tags came out as a line of chips each cut to two or three letters, which names none of
 * them. Dropping the chips that do not fit keeps the ones that are shown readable, and the
 * trailing chip says how many are missing and names them on hover.
 */
public class TagRow extends Region {

    /** The gap between one chip and the next, matching the spacing the card uses. */
    private static final double SPACING = 6;
    /** Enough of a chip to read the start of a tag, kept even beside a very long name. */
    private static final double READABLE_CHIP_WIDTH = 70;
    /**
     * How much of the tags a card shows. A card is a summary, and the chips share the row
     * with the name, so a long tag is cut and a long list is counted rather than letting the
     * tags take the card. The whole of what is cut is a hover away.
     */
    private static final int MAX_TAG_CHARACTERS = 12;
    private static final int MAX_CHIPS = 3;

    private final List<Label> chips = new ArrayList<>();
    private final Label overflow = new Label();
    private final Rectangle clip = new Rectangle();
    private List<String> tagNames = List.of();

    /**
     * Creates an empty row. The row takes no space until it is given tags.
     */
    public TagRow() {
        overflow.getStyleClass().add("tag-overflow");
        setClip(clip);
    }

    /**
     * Fills the row with a chip for each of {@code names}, in the order given.
     */
    public void setTags(List<String> names) {
        tagNames = List.copyOf(names);
        chips.clear();
        getChildren().clear();
        for (String tagName : tagNames) {
            Label chip = new Label();
            UiText.showShortened(chip, tagName, MAX_TAG_CHARACTERS);
            chips.add(chip);
        }
        getChildren().addAll(chips);
        getChildren().add(overflow);
        overflow.setTooltip(new Tooltip());
        requestLayout();
    }

    @Override
    protected double computePrefWidth(double height) {
        // only what the row would ever draw, since a student with twenty tags would otherwise
        // ask for a row twenty chips wide and leave the name beside it nothing to shrink into
        return widthOf(Math.min(chips.size(), MAX_CHIPS));
    }

    @Override
    protected double computeMinWidth(double height) {
        return chips.isEmpty() ? 0 : Math.min(READABLE_CHIP_WIDTH, computePrefWidth(height));
    }

    @Override
    protected double computePrefHeight(double width) {
        double tallest = 0;
        for (Label chip : chips) {
            tallest = Math.max(tallest, chip.prefHeight(-1));
        }
        return tallest;
    }

    @Override
    protected void layoutChildren() {
        clip.setWidth(getWidth());
        clip.setHeight(getHeight());
        getChildren().forEach(child -> child.setVisible(false));
        if (chips.isEmpty()) {
            return;
        }

        int shown = countThatFit();
        describeTheRest(shown);
        double x = 0;
        for (int i = 0; i < shown; i++) {
            x = place(chips.get(i), x);
        }
        if (shown < chips.size()) {
            place(overflow, x);
        }
    }

    /**
     * Returns how many chips the row can show whole, leaving room for the trailing chip when
     * it cannot show them all. Shows at least one chip, partly cut if need be, so that a row
     * too narrow for any whole chip still says something about the student.
     */
    private int countThatFit() {
        for (int shown = Math.min(chips.size(), MAX_CHIPS); shown > 0; shown--) {
            if (fits(shown)) {
                return shown;
            }
        }
        return chips.size() == 1 ? 1 : 0;
    }

    private boolean fits(int shown) {
        return widthOf(shown) <= getWidth();
    }

    /**
     * Returns the width the row takes to draw {@code shown} chips, together with the trailing
     * chip when that leaves any out.
     */
    private double widthOf(int shown) {
        double needed = 0;
        for (int i = 0; i < shown; i++) {
            needed += drawnWidthOf(chips.get(i)) + SPACING;
        }
        if (shown < chips.size()) {
            needed += widthOfTheRest(shown) + SPACING;
        }
        return Math.max(0, needed - SPACING);
    }

    /**
     * Returns what the trailing chip takes to say that {@code shown} chips are drawn.
     *
     * <p>The chip itself is asked, and put back as it was. A chip built to one side to
     * measure instead is not in the window, so it is not dressed by the stylesheet and comes
     * out a little narrower than the one drawn, which is enough to fit one chip too many on
     * a row that is nearly full.
     */
    private double widthOfTheRest(int shown) {
        String wasShowing = overflow.getText();
        overflow.setText(textForTheRest(shown));
        double width = drawnWidthOf(overflow);
        overflow.setText(wasShowing);
        return width;
    }

    /**
     * Puts the number of chips left out on the trailing chip, and names them on hover.
     */
    private void describeTheRest(int shown) {
        overflow.setText(textForTheRest(shown));
        overflow.getTooltip().setText(String.join(", ", tagNames.subList(shown, chips.size())));
    }

    private String textForTheRest(int shown) {
        return (chips.size() - shown) + " more…";
    }

    /**
     * Lays {@code chip} out at {@code x}, centred down the row, and returns where the next
     * chip starts.
     */
    private double place(Label chip, double x) {
        double width = drawnWidthOf(chip);
        double height = chip.prefHeight(-1);
        chip.resizeRelocate(x, Math.round((getHeight() - height) / 2), width, height);
        chip.setVisible(true);
        return x + width + SPACING;
    }

    /**
     * Returns the width {@code chip} is drawn at: what its text needs, within the cap the
     * stylesheet puts on a chip so that one long tag cannot take the row by itself.
     */
    private static double drawnWidthOf(Label chip) {
        return Math.min(chip.prefWidth(-1), chip.maxWidth(-1));
    }
}
