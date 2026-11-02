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
 * It carries the menus the window does, so that every dropdown a user can open is read.
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
    private Stage stage;

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    @BeforeEach
    public void showMenuBar() throws Exception {
        bar.getMenus().addAll(menu("File", "Exit"), menu("Help", "Help"));

        stage = onFxThread(() -> {
            Stage window = new Stage();
            Scene scene = new Scene(new VBox(bar), 400, 200);
            for (String stylesheet : STYLESHEETS) {
                scene.getStylesheets().add(
                        MenuBarTest.class.getResource("/view/" + stylesheet).toExternalForm());
            }
            window.setScene(scene);
            window.show();
            return window;
        });
    }

    @AfterEach
    public void hideMenuBar() throws Exception {
        onFxThread(() -> {
            stage.hide();
            return null;
        });
    }

    private static Menu menu(String name, String item) {
        Menu menu = new Menu(name);
        menu.getItems().add(new MenuItem(item));
        return menu;
    }

    @Test
    public void openMenu_nothingPickedOut_everyLabelReads() throws Exception {
        for (Menu menu : bar.getMenus()) {
            readWholeMenu(menu, false);
        }
    }

    @Test
    public void openMenu_itemPickedOut_everyLabelReads() throws Exception {
        for (Menu menu : bar.getMenus()) {
            readWholeMenu(menu, true);
        }
    }

    /**
     * Opens {@code menu} and asserts that everything it then shows can be read, with the
     * keyboard on its first item or with nothing picked out at all.
     */
    private void readWholeMenu(Menu menu, boolean isPickedOut) throws Exception {
        String item = menu.getItems().get(0).getText();
        onFxThread(() -> {
            menu.show();
            return null;
        });
        try {
            // the item is drawn in a popup of its own, which the toolkit lays out and styles
            // a pulse or more after the menu is asked to open
            FxToolkit.awaitAssertion(() -> {
                if (isPickedOut) {
                    pickOutFirstItem();
                    assertPickedOut(item);
                }
                assertEveryLabelReads(item);
            });
        } finally {
            onFxThread(() -> {
                menu.hide();
                return null;
            });
        }
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
        List<String> onTheAccent = textsOf(labelsInPopups().stream()
                .filter(label -> colourBehind(label).equals(ACCENT))
                .toList());

        // without this the check below would pass on a menu whose item was never moved onto,
        // which is the state the other test already covers
        assertTrue(onTheAccent.contains(item),
                "expected <" + item + "> to be picked out, but the accent is behind " + onTheAccent);
    }

    /**
     * Asserts that every label the window and the open menu show stands out from the colour
     * behind it, and that the dropdown shows the item named.
     */
    private void assertEveryLabelReads(String item) {
        List<Label> inPopups = labelsInPopups();
        List<Label> inWindow = labelsIn(stage.getScene().getRoot());
        // the bar names the two menus whatever is open, so a dropdown the walk misses would
        // otherwise leave the loop below reading the bar alone
        assertTrue(textsOf(inPopups).contains(item),
                "expected the open menu to show <" + item + ">, but it shows " + textsOf(inPopups));
        assertTrue(textsOf(inWindow).containsAll(List.of("File", "Help")),
                "expected the bar to name both menus, but it shows " + textsOf(inWindow));

        List<Label> everything = new ArrayList<>(inWindow);
        everything.addAll(inPopups);
        for (Label label : everything) {
            Color fill = label.getTextFill() instanceof Color colour ? colour : Color.TRANSPARENT;
            Color behind = colourBehind(label);
            assertTrue(contrast(fill, behind) >= READABLE_CONTRAST,
                    "<" + label.getText() + "> is drawn in " + fill + " on " + behind
                            + ", a contrast of " + contrast(fill, behind));
        }
    }

    /** Every label drawn in a popup the window put on the screen, which is the open menu. */
    private List<Label> labelsInPopups() {
        List<Label> labels = new ArrayList<>();
        for (Window window : Window.getWindows()) {
            if (isPopupOf(window, stage) && window.getScene() != null) {
                labels.addAll(labelsIn(window.getScene().getRoot()));
            }
        }
        return labels;
    }

    private static List<String> textsOf(List<Label> labels) {
        return labels.stream().map(Label::getText).toList();
    }

    /** Returns true if {@code window} is a popup that {@code owner} put on the screen. */
    private static boolean isPopupOf(Window window, Stage owner) {
        return window instanceof PopupWindow popup && popup.getOwnerWindow() == owner;
    }

    /** Every label below {@code node}, each of which a reader has to make out. */
    private static List<Label> labelsIn(Node node) {
        List<Label> labels = new ArrayList<>();
        collectLabels(node, labels);
        return labels;
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
