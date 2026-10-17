package seedu.tab.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
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

    /** Enough of a chip to read the start of a tag, kept even beside a very long name. */
    static final double READABLE_CHIP_WIDTH = 70;

    /** The gap between one chip and the next, matching the spacing the card uses. */
    private static final double SPACING = 6;
    /**
     * Enough of a chip for the first few letters of a tag, which is what the row keeps when
     * the alternative is saying nothing about how many tags were left out.
     */
    private static final double RECOGNISABLE_CHIP_WIDTH = 44;
    /**
     * How many characters of a tag a chip shows. A card is a summary, and the chips share
     * the row with the name, so a long tag is cut and a long list is counted rather than
     * letting the tags take the card. The whole of what is cut is a hover away.
     *
     * <p>The length is set by the tags a course actually uses: "attendance-low" and
     * "needs-consult" are the length of an ordinary tag, and cutting shorter than this cuts
     * most of what anyone types. How many chips then fit is decided by the row rather than
     * by this, so a longer limit costs chips on a narrow card, not a broken layout.
     */
    private static final int MAX_TAG_CHARACTERS = 16;
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
     * Fills the row with up to {@code MAX_CHIPS} chips in the order given, retaining all names for overflow.
     */
    public void setTags(List<String> names) {
        tagNames = List.copyOf(names);
        chips.clear();
        getChildren().clear();
        for (String tagName : tagNames.subList(0, Math.min(tagNames.size(), MAX_CHIPS))) {
            Label chip = new Label();
            UiText.showShortenedToFit(chip, tagName, MAX_TAG_CHARACTERS);
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
        return widthOf(chips.size());
    }

    @Override
    protected double computeMinWidth(double height) {
        if (chips.isEmpty()) {
            return 0;
        }
        // enough for the start of one tag and a count of the others, since a row showing
        // one chip and no count says nothing about the tags it left out
        double forACount = tagNames.size() > 1 ? widthOfTheRest(1) + SPACING : 0;
        return Math.min(computePrefWidth(height), RECOGNISABLE_CHIP_WIDTH + forACount);
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

        double countWidth = widthOfTheRest(1);
        double forTheChip = getWidth() - countWidth - SPACING;
        if (forTheChip < RECOGNISABLE_CHIP_WIDTH) {
            // what is left of the chip would be an ellipsis and little else, which names no
            // tag, so the row says how many there are instead
            describeTheRest(0);
            place(overflow, 0, getWidth());
            return;
        }

        place(chips.get(0), 0, forTheChip);
        describeTheRest(1);
        place(overflow, forTheChip + SPACING, countWidth);
    }

    /**
     * Returns how many chips the row can show whole, leaving room for the trailing chip when
     * it cannot show them all, or none when not even one chip and that trailing chip fit.
     */
    private int countThatFit() {
        for (int shown = chips.size(); shown > 0; shown--) {
            if (fits(shown)) {
                return shown;
            }
        }
        return 0;
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
        if (shown < tagNames.size()) {
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
    private String textForTheRest(int shown) {
        int leftOut = tagNames.size() - shown;
        if (shown == 0) {
            return leftOut + " tag" + StringUtil.getPluralSuffix(leftOut);
        }
        return leftOut + " more…";
    }

    /**
     * Lays {@code chip} out at {@code x}, centred down the row, and returns where the next
     * chip starts.
     */
    private double place(Label chip, double x) {
        return place(chip, x, drawnWidthOf(chip));
    }

    /**
     * Lays {@code chip} out at {@code x} in {@code room}, centred down the row. A chip is
     * never drawn wider than its own text needs, so a lone tag reads as a tag rather than
     * as a pill stretched across the row.
     */
    private double place(Label chip, double x, double room) {
        double width = Math.min(drawnWidthOf(chip), room);
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
