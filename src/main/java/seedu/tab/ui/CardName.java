package seedu.tab.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.text.Text;

/**
 * The name on a student card: a label that cuts its own text down to the room it is given,
 * rather than leaving that to JavaFX.
 *
 * <p>A label told to elide from the middle cuts at a character index, which falls between the
 * two halves of an emoji or between a letter and the accent that belongs to it, and what is
 * left is drawn as a broken glyph. Cutting here goes through {@link UiText}, which counts
 * characters as a reader does. The stylesheet tells the label to clip rather than elide, so a
 * cut this class gets a pixel wrong shows a glyph against the edge instead of half a character.
 *
 * <p>The label asks for the room the whole name needs however little of it is drawn. The row
 * shares its width out from what the name asks for, so a name that asked only for what it is
 * drawn at would be handed less room on every pass and the two would chase each other down.
 */
public class CardName extends Label {

    private final Text measurer = new Text();
    private String whole = "";
    private boolean isDrawingLessThanTheWhole;

    /**
     * Creates a name that holds on to the whole of whatever text it is given, so that what it
     * draws can be cut down without the name itself being forgotten.
     */
    public CardName() {
        textProperty().addListener((unused, old, text) -> {
            if (!isDrawingLessThanTheWhole) {
                whole = text == null ? "" : text;
            }
        });
    }

    @Override
    protected double computePrefWidth(double height) {
        return widthOf(whole) + besideTheText();
    }

    @Override
    protected void layoutChildren() {
        drawWhatFits();
        super.layoutChildren();
    }

    private void drawWhatFits() {
        double room = getWidth() - besideTheText();
        if (widthOf(whole) <= room) {
            draw(whole);
            return;
        }
        draw(UiText.middleShortened(whole, charactersThatFit(room)));
    }

    /**
     * Returns the greatest count of characters whose cut of the whole name is drawn inside
     * {@code room}. A cut to one more character is never the narrower of the two, so the count
     * is found by halving the range rather than by measuring every count in it.
     */
    private int charactersThatFit(double room) {
        int fewest = 0;
        int most = whole.length();
        while (fewest < most) {
            int count = (fewest + most + 1) / 2;
            if (widthOf(UiText.middleShortened(whole, count)) <= room) {
                fewest = count;
            } else {
                most = count - 1;
            }
        }
        return fewest;
    }

    private void draw(String text) {
        if (text.equals(getText())) {
            return;
        }
        isDrawingLessThanTheWhole = true;
        setText(text);
        isDrawingLessThanTheWhole = false;
    }

    private double widthOf(String text) {
        measurer.setFont(getFont());
        measurer.setText(text);
        return measurer.getLayoutBounds().getWidth();
    }

    private double besideTheText() {
        Insets insets = getInsets();
        return insets.getLeft() + insets.getRight();
    }
}
