package seedu.tab.ui;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Rectangle2D;
import seedu.tab.commons.core.GuiSettings;

/** Restores saved window bounds within one display's usable area. */
final class WindowBounds {

    private WindowBounds() {}

    /** Returns display bounds with the primary first and without repeating its bounds. */
    static List<Rectangle2D> prioritizePrimary(Rectangle2D primary, List<Rectangle2D> displays) {
        List<Rectangle2D> orderedDisplays = new ArrayList<>();
        orderedDisplays.add(primary);
        displays.stream().filter(display -> !display.equals(primary)).forEach(orderedDisplays::add);
        return orderedDisplays;
    }

    /** Returns fitted bounds; the first display is the primary display. */
    static Rectangle2D restore(GuiSettings settings, List<Rectangle2D> displays,
            double minWidth, double minHeight) {
        GuiSettings defaults = new GuiSettings();
        double width = usableDimension(settings.getWindowWidth(), defaults.getWindowWidth());
        double height = usableDimension(settings.getWindowHeight(), defaults.getWindowHeight());
        Point position = settings.getWindowCoordinates();
        Rectangle2D saved = position == null ? null
                : new Rectangle2D(position.getX(), position.getY(), width, height);
        Rectangle2D display = selectDisplay(saved, displays);
        width = Math.min(Math.max(width, minWidth), display.getWidth());
        height = Math.min(Math.max(height, minHeight), display.getHeight());
        boolean hasVisiblePosition = saved != null && overlap(saved, display) > 0;
        double x = hasVisiblePosition ? saved.getMinX() : display.getMinX() + (display.getWidth() - width) / 2;
        double y = hasVisiblePosition ? saved.getMinY() : display.getMinY() + (display.getHeight() - height) / 2;
        return new Rectangle2D(clamp(x, display.getMinX(), display.getMaxX() - width),
                clamp(y, display.getMinY(), display.getMaxY() - height), width, height);
    }

    private static double usableDimension(double dimension, double fallback) {
        return Double.isFinite(dimension) && dimension > 0 ? dimension : fallback;
    }

    private static Rectangle2D selectDisplay(Rectangle2D saved, List<Rectangle2D> displays) {
        Rectangle2D selected = displays.get(0);
        double largestOverlap = 0;
        if (saved == null) {
            return selected;
        }
        for (Rectangle2D display : displays) {
            double area = overlap(saved, display);
            if (area > largestOverlap) {
                selected = display;
                largestOverlap = area;
            }
        }
        return selected;
    }

    private static double overlap(Rectangle2D first, Rectangle2D second) {
        double width = Math.max(0, Math.min(first.getMaxX(), second.getMaxX())
                - Math.max(first.getMinX(), second.getMinX()));
        double height = Math.max(0, Math.min(first.getMaxY(), second.getMaxY())
                - Math.max(first.getMinY(), second.getMinY()));
        return width * height;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
