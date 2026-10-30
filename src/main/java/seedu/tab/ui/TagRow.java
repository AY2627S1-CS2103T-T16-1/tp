package seedu.tab.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import seedu.tab.commons.util.StringUtil;

/**
 * A row of tag chips that shows as many whole chips as the row has room for, and counts the
 * rest in a trailing chip.
 *
 * <p>A plain row would instead share the shortfall among every chip, so a card with several
 * tags came out as a line of chips each cut to two or three letters, which names none of
 * them. Dropping the chips that do not fit keeps the ones that are shown readable, and the
 * trailing chip says how many are missing and names them on hover. A row with space for
 * neither shows the first tag alone, cut to the row: which tag it is says more than how
 * many there are. Narrower again, where what is left of a chip would be an ellipsis and
 * little else, the row counts the tags instead and names them on hover.
 */
public class TagRow extends Region {

    /** The gap between one chip and the next, matching the spacing the card uses. */
    private static final double SPACING = 6;
    /** A truncated chip should retain at least four letters rather than an ambiguous pair. */
    private static final int RECOGNIZABLE_CHARACTERS = 4;
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
     * Fills the row with complete tag labels in the order given.
     */
    public void setTags(List<String> names) {
        tagNames = List.copyOf(names);
        chips.clear();
        getChildren().clear();
        for (String tagName : tagNames) {
            Label chip = new Label();
            UiText.showToFit(chip, tagName);
            chips.add(chip);
        }
        getChildren().addAll(chips);
        getChildren().add(overflow);
        overflow.setTooltip(new Tooltip());
        requestLayout();
    }

    @Override
    protected double computePrefWidth(double height) {
        return measureWidth(chips.size());
    }

    /**
     * Returns the width actually used within {@code available}, returning unused chip or
     * count space to the name. A partially drawn chip uses the remaining allocation.
     */
    double computeFittedWidth(double available) {
        if (chips.isEmpty()) {
            return 0;
        }
        int shown = countThatFit(available);
        if (shown > 0) {
            return measureWidth(shown);
        }
        if (tagNames.size() > 1
                && available - measureOverflowWidth(1) - SPACING < computeRecognizableChipWidth()) {
            return Math.min(available, measureOverflowWidth(0));
        }
        return available;
    }

    /** Returns room for a useful prefix and ellipsis, including the chip's styled padding. */
    private double computeRecognizableChipWidth() {
        Label chip = chips.getFirst();
        // The skin clips at glyph hit-test boundaries; reserve the next character too
        // so a boundary inside it still leaves the useful prefix intact.
        Text prefix = new Text(StringUtil.getFirstCharacters(chip.getText(), RECOGNIZABLE_CHARACTERS + 1)
                + chip.getEllipsisString());
        prefix.setFont(chip.getFont());
        double width = Math.ceil(prefix.getLayoutBounds().getWidth())
                + Math.ceil(chip.getInsets().getLeft()) + Math.ceil(chip.getInsets().getRight());
        return Math.min(measureDrawnWidth(chip), width);
    }

    @Override
    protected double computeMinWidth(double height) {
        if (chips.isEmpty()) {
            return 0;
        }
        // enough for the start of one tag and a count of the others, since a row showing
        // one chip and no count says nothing about the tags it left out
        double widthForCount = tagNames.size() > 1 ? measureOverflowWidth(1) + SPACING : 0;
        return Math.min(computePrefWidth(height), computeRecognizableChipWidth() + widthForCount);
    }

    /** Returns the width needed to show the total count when every tag is hidden. */
    double getCountWidth() {
        return tagNames.size() > 1 ? snapSizeX(measureOverflowWidth(0)) : 0;
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

        int shown = countThatFit(getWidth());
        if (shown == 0) {
            showOneCutChip();
            return;
        }

        describeTheRest(shown);
        double x = 0;
        for (int i = 0; i < shown; i++) {
            x = place(chips.get(i), x);
        }
        if (shown < tagNames.size()) {
            place(overflow, x);
        }
    }

    /**
     * Draws the first tag cut to the row, with a count of the others after it where there
     * is room for one.
     *
     * <p>A row too narrow for a whole chip and a count still has room for part of a chip and
     * a count, and dropping the count there leaves nothing to say the other tags exist.
     * Drawing the chip whole and the count after it anyway put the count half outside the
     * row, where it read as "1 m".
     */
    private void showOneCutChip() {
        if (tagNames.size() == 1) {
            // one tag and nothing to count, so the row goes to it, cut to what there is
            place(chips.get(0), 0, getWidth());
            return;
        }

        double countWidth = measureOverflowWidth(1);
        double widthForChip = getWidth() - countWidth - SPACING;
        if (widthForChip < computeRecognizableChipWidth()) {
            // what is left of the chip would be an ellipsis and little else, which names no
            // tag, so the row says how many there are instead
            describeTheRest(0);
            place(overflow, 0, getWidth());
            return;
        }

        place(chips.get(0), 0, widthForChip);
        describeTheRest(1);
        place(overflow, widthForChip + SPACING, countWidth);
    }

    /**
     * Returns how many chips the row can show whole, leaving room for the trailing chip when
     * it cannot show them all, or none when not even one chip and that trailing chip fit.
     */
    private int countThatFit(double available) {
        double chipsWidth = 0;
        int fitting = 0;
        for (int shown = 1; shown <= chips.size(); shown++) {
            chipsWidth += measureDrawnWidth(chips.get(shown - 1)) + (shown > 1 ? SPACING : 0);
            double needed = chipsWidth;
            if (shown < chips.size()) {
                needed += SPACING + measureOverflowWidth(shown);
            }
            if (needed <= available) {
                fitting = shown;
            }
        }
        return fitting;
    }

    /**
     * Returns the width the row takes to draw {@code shown} chips, together with the trailing
     * chip when that leaves any out.
     */
    private double measureWidth(int shown) {
        double needed = 0;
        for (int i = 0; i < shown; i++) {
            needed += measureDrawnWidth(chips.get(i)) + SPACING;
        }
        if (shown < tagNames.size()) {
            needed += measureOverflowWidth(shown) + SPACING;
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
    private double measureOverflowWidth(int shown) {
        String wasShowing = overflow.getText();
        overflow.setText(formatOverflowText(shown));
        double width = measureDrawnWidth(overflow);
        overflow.setText(wasShowing);
        return width;
    }

    /**
     * Puts the number of chips left out on the trailing chip, and names them on hover.
     */
    private void describeTheRest(int shown) {
        overflow.setText(formatOverflowText(shown));
        overflow.getTooltip().setText(String.join(", ", tagNames.subList(shown, tagNames.size())));
    }

    /**
     * Returns what the trailing chip says about the {@code tagNames.size() - shown} tags the
     * row leaves out.
     *
     * <p>A row too narrow for even a cut chip draws this chip alone, and there is then
     * nothing for the tags to be more <em>than</em>, so it counts them rather than
     * counting what it left out.
     */
    private String formatOverflowText(int shown) {
        int leftOut = tagNames.size() - shown;
        if (shown == 0) {
            return String.format(Locale.ROOT, "%d tag%s", leftOut, StringUtil.getPluralSuffix(leftOut));
        }
        return String.format(Locale.ROOT, "%d more…", leftOut);
    }

    /**
     * Lays {@code chip} out at {@code x}, centered down the row, and returns where the next
     * chip starts.
     */
    private double place(Label chip, double x) {
        return place(chip, x, measureDrawnWidth(chip));
    }

    /**
     * Lays {@code chip} out at {@code x} in {@code room}, centered down the row. A chip is
     * never drawn wider than its own text needs, so a lone tag reads as a tag rather than
     * as a pill stretched across the row.
     */
    private double place(Label chip, double x, double room) {
        double width = Math.min(measureDrawnWidth(chip), room);
        double height = chip.prefHeight(-1);
        chip.resizeRelocate(x, Math.round((getHeight() - height) / 2), width, height);
        chip.setVisible(true);
        return x + width + SPACING;
    }

    /**
     * Returns the natural width of {@code chip}, including its styled padding.
     */
    private static double measureDrawnWidth(Label chip) {
        return Math.min(chip.prefWidth(-1), chip.maxWidth(-1));
    }
}
