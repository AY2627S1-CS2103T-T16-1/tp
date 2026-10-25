package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

public class UiFontsTest {

    /**
     * The families the window is meant to prefer, in order. Held here rather than read back
     * from {@code UiFonts}, so that the test states what the list should be instead of
     * agreeing with whatever it happens to be.
     */
    private static final List<String> PREFERRED =
            List.of("Inter", "Segoe UI", "Helvetica Neue", "Cantarell", "Noto Sans");

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        return FxToolkit.onFxThread(work);
    }

    @Test
    public void bodyFamily_isOneTheMachineHas() throws Exception {
        String family = onFxThread(UiFonts::bodyFamily);

        assertTrue(Font.getFamilies().contains(family) || family.equals(Font.getDefault().getFamily()),
                "the window should be written in a family the machine has, not " + family);
    }

    @Test
    public void applyTo_aScene_writesWhatIsInItInThatFamily() throws Exception {
        String[] families = onFxThread(() -> {
            Label label = new Label("Siti Nur-Aisyah");
            StackPane root = new StackPane(label);
            Scene scene = new Scene(root, 200, 60);
            scene.getStylesheets().add(
                    UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
            UiFonts.applyTo(scene);
            root.applyCss();
            root.layout();
            return new String[] {label.getFont().getFamily(), UiFonts.bodyFamily()};
        });

        // a stylesheet naming several families falls back to the system font rather than
        // trying the next one, so the family is chosen here and inherited from the window
        assertEquals(families[1], families[0]);
    }

    @Test
    public void bodyFamily_aMachineWithAPreferredFamily_usesTheFirstOfThem() throws Exception {
        List<String> installedPreferred = onFxThread(() -> {
            List<String> installed = Font.getFamilies();
            return PREFERRED.stream().filter(installed::contains).toList();
        });
        // the families a machine carries are its own affair, so this one is reported skipped
        // rather than passed where there is nothing to choose
        assumeFalse(installedPreferred.isEmpty(),
                "this machine carries none of the families the window prefers");

        assertEquals(installedPreferred.get(0), onFxThread(UiFonts::bodyFamily),
                "the window should be written in the first preferred family the machine has");
    }

    @Test
    public void firstOf_aPreferredFamilyInstalled_doesNotKeepTheMachinesOwn() throws Exception {
        String chosen = onFxThread(() -> UiFonts.firstOf(
                List.of("Segoe UI"), List.of(Font.getDefault().getFamily(), "Segoe UI")));

        assertEquals("Segoe UI", chosen,
                "a machine carrying a preferred family should use it rather than its own");
    }

    @Test
    public void firstOf_aMachineWithNoneOfThem_keepsItsOwn() throws Exception {
        String chosen = onFxThread(() -> UiFonts.firstOf(
                List.of("Inter", "Segoe UI"), List.of("Some Other Face", "And Another")));

        assertEquals(Font.getDefault().getFamily(), chosen,
                "a machine carrying none of the preferred families should keep its own");
    }

    @Test
    public void firstOf_severalInstalled_takesTheFirstPreferred() throws Exception {
        String chosen = onFxThread(() -> UiFonts.firstOf(
                List.of("Inter", "Segoe UI"), List.of("Segoe UI", "Inter")));

        assertEquals("Inter", chosen, "the order preferred decides, not the order installed");
    }
}
