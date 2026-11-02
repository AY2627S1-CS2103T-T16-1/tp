package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.ui.FxToolkit.onFxThread;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.javafx.stage.WindowHelper;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Checks that the menu can be read, wherever the stylesheet draws it.
 *
 * <p>The bar is built here rather than taken from {@code MainWindow}, because what decides
 * the colours is the stylesheet the window is dressed with rather than the menus it holds.
 */
public class MenuBarTest {

    /** The stylesheets {@code MainWindow.fxml} names, which colour everything below it. */
    private static final List<String> STYLESHEETS = List.of("DarkTheme.css", "Extensions.css");

    /** The contrast WCAG AA asks of text this size, as a ratio of relative luminances. */
    private static final double READABLE_CONTRAST = 4.5;

    /**
     * The colour behind a node that sits on nothing of its own, which is what the window's
     * root is painted. It mirrors {@code -tab-bg} in {@code DarkTheme.css}; the stylesheet
     * paints the root with the name rather than the value, so the two are kept in step by
     * hand.
     */
    private static final Color WINDOW_BACKGROUND = Color.web("#15181c");

    /** The colour an item is picked out with, mirroring {@code -tab-accent} the same way. */
    private static final Color ACCENT = Color.web("#68c4ee");

    private final MenuBar bar = new MenuBar();
    private final Menu file = new Menu("File");
    private Stage stage;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    @BeforeEach
    public void showMenuBar() throws Exception {
        file.getItems().add(new MenuItem("Exit"));
        Menu help = new Menu("Help");
        help.getItems().add(new MenuItem("Help F1"));
        bar.getMenus().addAll(file, help);

        stage = onFxThread(() -> {
            Stage window = new Stage();
            Scene scene = new Scene(new VBox(bar), 400, 200);
            for (String stylesheet : STYLESHEETS) {
                scene.getStylesheets().add(
                        MenuBarTest.class.getResource("/view/" + stylesheet).toExternalForm());
            }
            window.setScene(scene);
            window.show();
            file.show();
            return window;
        });
    }

    @AfterEach
    public void hideMenuBar() throws Exception {
        onFxThread(() -> {
            file.hide();
            stage.hide();
            return null;
        });
    }

    @Test
    public void openMenu_nothingPickedOut_everyLabelReads() throws Exception {
        // the item is drawn in a popup of its own, which the toolkit lays out and styles a
        // pulse or more after the menu is asked to open
        FxToolkit.awaitAssertion(() -> assertEveryLabelReads("File", "Help", "Exit"));
    }

    @Test
    public void openMenu_itemPickedOut_everyLabelReads() throws Exception {
        // an item moved onto with the arrow keys is drawn on the accent colour instead, which
        // a rule of its own darkens the text for
        FxToolkit.awaitAssertion(() -> {
            pickOutFirstItem();
            assertPickedOut("Exit");
            assertEveryLabelReads("File", "Help", "Exit");
        });
    }

    /** Puts the keyboard on the first item of the open menu, as arrowing onto it does. */
    private void pickOutFirstItem() {
        for (Window window : Window.getWindows()) {
            if (!isPopupOf(window, stage) || window.getScene() == null) {
                continue;
            }
            // a node counts as focused only inside a window that is, and a machine running
            // the tests without a desktop never activates the popup, so it is activated here
            // rather than waited for
            WindowHelper.setFocused(window, true);
            Node item = window.getScene().getRoot().lookup(".menu-item");
            if (item != null) {
                item.requestFocus();
            }
        }
    }

    /** Asserts that the item named is the one the open menu draws as picked out. */
    private void assertPickedOut(String item) {
        List<String> onTheAccent = labelsShown().stream()
                .filter(label -> colourBehind(label).equals(ACCENT))
                .map(Label::getText)
                .toList();

        // without this the check below would pass on a menu whose item was never moved onto,
        // which is the state the other test already covers
        assertTrue(onTheAccent.contains(item),
                "expected <" + item + "> to be picked out, but the accent is behind " + onTheAccent);
    }

    /**
     * Asserts that every label shown in the window or in a popup of its own stands out from
     * the colour behind it, and that the labels named are among them.
     */
    private void assertEveryLabelReads(String... expected) {
        List<Label> labels = labelsShown();
        List<String> shown = labels.stream().map(Label::getText).toList();
        // a label the walk misses would otherwise pass the loop below without being read
        assertTrue(shown.containsAll(List.of(expected)),
                "expected " + List.of(expected) + " to be shown, but the menu shows " + shown);

        for (Label label : labels) {
            Color fill = label.getTextFill() instanceof Color colour ? colour : Color.TRANSPARENT;
            Color behind = colourBehind(label);
            assertTrue(contrast(fill, behind) >= READABLE_CONTRAST,
                    "<" + label.getText() + "> is drawn in " + fill + " on " + behind
                            + ", a contrast of " + contrast(fill, behind));
        }
    }

    /** Every label the window and its popups draw, each of which a reader has to make out. */
    private List<Label> labelsShown() {
        List<Label> labels = new ArrayList<>();
        for (Window window : Window.getWindows()) {
            boolean isWindowOrItsPopup = window == stage || isPopupOf(window, stage);
            if (isWindowOrItsPopup && window.getScene() != null) {
                collectLabels(window.getScene().getRoot(), labels);
            }
        }
        return labels;
    }

    /** Returns true if {@code window} is a popup that {@code owner} put on the screen. */
    private static boolean isPopupOf(Window window, Stage owner) {
        return window instanceof PopupWindow popup && popup.getOwnerWindow() == owner;
    }

    private static void collectLabels(Node node, List<Label> labels) {
        if (node instanceof Label label && !label.getText().isEmpty()) {
            labels.add(label);
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> collectLabels(child, labels));
        }
    }

    /** The colour drawn behind {@code node}, which is the first one an ancestor fills with. */
    private static Color colourBehind(Node node) {
        if (node instanceof Region region && region.getBackground() != null
                && !region.getBackground().getFills().isEmpty()
                && region.getBackground().getFills().get(0).getFill() instanceof Color fill
                && !fill.equals(Color.TRANSPARENT)) {
            return fill;
        }
        return node.getParent() == null ? WINDOW_BACKGROUND : colourBehind(node.getParent());
    }

    /** The WCAG contrast ratio of two colours, which runs from 1 to 21. */
    private static double contrast(Color one, Color other) {
        double brighter = Math.max(relativeLuminance(one), relativeLuminance(other));
        double darker = Math.min(relativeLuminance(one), relativeLuminance(other));
        return (brighter + 0.05) / (darker + 0.05);
    }

    /** A colour's relative luminance, as WCAG defines it. */
    private static double relativeLuminance(Color colour) {
        return 0.2126 * channel(colour.getRed()) + 0.7152 * channel(colour.getGreen())
                + 0.0722 * channel(colour.getBlue());
    }

    private static double channel(double value) {
        return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
