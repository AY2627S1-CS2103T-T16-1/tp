package seedu.tab.ui;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.text.Font;

/**
 * Chooses the typeface the window is written in.
 *
 * <p>A stylesheet cannot do this. JavaFX reads only the first family of an
 * {@code -fx-font-family} list and falls back to the system font when it is missing, rather
 * than trying the next one as a browser does, so a list naming a font for each platform ends
 * up using none of them. The choice is therefore made here, against the families the machine
 * actually has, and set on the window for everything in it to inherit.
 */
final class UiFonts {

    /**
     * The families the window prefers, in order. Each is the usual interface font of a
     * platform the product supports; a machine with none of them keeps its own.
     */
    private static final List<String> PREFERRED_FAMILIES =
            List.of("Inter", "Segoe UI", "Helvetica Neue", "Cantarell", "Noto Sans");

    private UiFonts() {} // this class only chooses a font

    /**
     * Returns the first preferred family the machine has, or the font it would use anyway.
     */
    static String chooseBodyFamily() {
        return findFirstInstalledFamily(PREFERRED_FAMILIES, Font.getFamilies());
    }

    /**
     * Returns the first preferred family present in {@code installedFamilies}, or the default family
     * when none of {@code preferredFamilies} is installed.
     */
    static String findFirstInstalledFamily(List<String> preferredFamilies, List<String> installedFamilies) {
        return preferredFamilies.stream()
                .filter(installedFamilies::contains)
                .findFirst()
                .orElseGet(() -> Font.getDefault().getFamily());
    }

    /**
     * Writes {@code scene} in the chosen family. Every node in it inherits the family, so a
     * rule in the stylesheet only has to name a family where it wants a different one.
     */
    static void applyTo(Scene scene) {
        scene.getRoot().setStyle("-fx-font-family: \"" + chooseBodyFamily() + "\";");
    }
}
