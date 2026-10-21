package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.geometry.Rectangle2D;
import seedu.tab.commons.core.GuiSettings;

public class WindowBoundsTest {
    private static final Rectangle2D PRIMARY = new Rectangle2D(0, 30, 1920, 1010);
    private static final Rectangle2D SECONDARY = new Rectangle2D(-1280, 0, 1280, 984);
    private static final List<Rectangle2D> DISPLAYS = List.of(PRIMARY, SECONDARY);
    private static final double MIN_WIDTH = 490;
    private static final double MIN_HEIGHT = 600;

    @Test
    public void prioritizePrimary_secondaryListedFirst_preservesSecondaryAndPrimaryFallback() {
        List<Rectangle2D> availableDisplays = new ArrayList<>(List.of(SECONDARY, PRIMARY));
        List<Rectangle2D> orderedDisplays = WindowBounds.prioritizePrimary(PRIMARY, availableDisplays);

        assertEquals(DISPLAYS, orderedDisplays);
        assertEquals(List.of(SECONDARY, PRIMARY), availableDisplays);
        assertBounds(new GuiSettings(800, 650, -1200, 20), orderedDisplays,
                new Rectangle2D(-1200, 20, 800, 650));
        assertBounds(new GuiSettings(800, 650, 99999, 99999), orderedDisplays,
                new Rectangle2D(560, 210, 800, 650));
    }

    @Test
    public void prioritizePrimary_singleDisplay_doesNotRepeatPrimary() {
        assertEquals(List.of(PRIMARY), WindowBounds.prioritizePrimary(PRIMARY, List.of(PRIMARY)));
    }

    @Test
    public void restore_visibleBounds_preservesSizeAndPosition() {
        assertBounds(new GuiSettings(800, 650, 100, 100), DISPLAYS,
                new Rectangle2D(100, 100, 800, 650));
        assertBounds(new GuiSettings(800, 650, -1200, 20), DISPLAYS,
                new Rectangle2D(-1200, 20, 800, 650));
    }

    @Test
    public void restore_disconnectedDisplay_centersOnPrimary() {
        assertBounds(new GuiSettings(800, 650, -1200, 20), List.of(PRIMARY),
                new Rectangle2D(560, 210, 800, 650));
        assertBounds(new GuiSettings(800, 650, 99999, 99999), DISPLAYS,
                new Rectangle2D(560, 210, 800, 650));
    }

    @Test
    public void restore_defaultPosition_centersOnPrimary() {
        assertBounds(new GuiSettings(), DISPLAYS, new Rectangle2D(590, 235, 740, 600));
    }

    @Test
    public void restore_partialOverlap_fitsOnDisplayWithMostOverlap() {
        assertBounds(new GuiSettings(800, 650, -700, 800), DISPLAYS,
                new Rectangle2D(-800, 334, 800, 650));
        assertBounds(new GuiSettings(800, 650, 1700, -100), DISPLAYS,
                new Rectangle2D(1120, 30, 800, 650));
    }

    @Test
    public void restore_equalOverlap_prefersPrimaryDisplay() {
        assertBounds(new GuiSettings(800, 650, -400, 30), DISPLAYS,
                new Rectangle2D(0, 30, 800, 650));
    }

    @Test
    public void restore_spanningAdjacentDisplays_fitsOnDisplayWithMostOverlap() {
        List<Rectangle2D> adjacentDisplays = List.of(new Rectangle2D(0, 0, 1920, 1040),
                new Rectangle2D(1920, 0, 1920, 1040));
        assertBounds(new GuiSettings(800, 650, 1500, 100), adjacentDisplays,
                new Rectangle2D(1120, 100, 800, 650));
    }

    @Test
    public void restore_oversizedOrUndersizedDimensions_fitsUsableArea() {
        assertBounds(new GuiSettings(1.0E9, 1.0E9, 99999, 99999), DISPLAYS, PRIMARY);
        assertBounds(new GuiSettings(1, 1, 100, 100), DISPLAYS,
                new Rectangle2D(100, 100, MIN_WIDTH, MIN_HEIGHT));
        Rectangle2D scaledDisplay = new Rectangle2D(0, 20, 853, 440);
        assertBounds(new GuiSettings(), List.of(scaledDisplay), new Rectangle2D(56.5, 20, 740, 440));
        Rectangle2D tinyDisplay = new Rectangle2D(20, 20, 400, 300);
        assertBounds(new GuiSettings(), List.of(tinyDisplay), tinyDisplay);
    }

    @Test
    public void restore_invalidDimensions_usesDefaults() {
        for (double dimension : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY}) {
            assertBounds(new GuiSettings(dimension, dimension, 100, 100), DISPLAYS,
                    new Rectangle2D(100, 100, 740, 600));
        }
    }

    private static void assertBounds(GuiSettings settings, List<Rectangle2D> displays, Rectangle2D expected) {
        assertEquals(expected, WindowBounds.restore(settings, displays, MIN_WIDTH, MIN_HEIGHT));
    }
}
