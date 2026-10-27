package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static seedu.tab.ui.FxToolkit.onFxThread;

import java.util.List;

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
    private static final List<String> PREFERRED_FAMILIES =
            List.of("Inter", "Segoe UI", "Helvetica Neue", "Cantarell", "Noto Sans");

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }


    @Test
    public void chooseBodyFamily_isOneTheMachineHas() throws Exception {
        String family = onFxThread(UiFonts::chooseBodyFamily);

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
            return new String[] {label.getFont().getFamily(), UiFonts.chooseBodyFamily()};
        });

        // a stylesheet naming several families falls back to the system font rather than
        // trying the next one, so the family is chosen here and inherited from the window
        assertEquals(families[1], families[0]);
    }

    @Test
    public void chooseBodyFamily_aMachineWithAPreferredFamily_usesTheFirstOfThem() throws Exception {
        List<String> installedPreferred = onFxThread(() -> {
            List<String> installedFamilies = Font.getFamilies();
            return PREFERRED_FAMILIES.stream().filter(installedFamilies::contains).toList();
        });
        // the families a machine carries are its own affair, so this one is reported skipped
        // rather than passed where there is nothing to choose
        assumeFalse(installedPreferred.isEmpty(),
                "this machine carries none of the families the window prefers");

        assertEquals(installedPreferred.get(0), onFxThread(UiFonts::chooseBodyFamily),
                "the window should be written in the first preferred family the machine has");
    }

    @Test
    public void findFirstInstalledFamily_aPreferredFamilyInstalled_doesNotKeepTheMachinesOwn() throws Exception {
        String chosen = onFxThread(() -> UiFonts.findFirstInstalledFamily(
                List.of("Segoe UI"), List.of(Font.getDefault().getFamily(), "Segoe UI")));

        assertEquals("Segoe UI", chosen,
                "a machine carrying a preferred family should use it rather than its own");
    }

    @Test
    public void findFirstInstalledFamily_aMachineWithNoneOfThem_keepsItsOwn() throws Exception {
        String chosen = onFxThread(() -> UiFonts.findFirstInstalledFamily(
                List.of("Inter", "Segoe UI"), List.of("Some Other Face", "And Another")));

        assertEquals(Font.getDefault().getFamily(), chosen,
                "a machine carrying none of the preferred families should keep its own");
    }

    @Test
    public void findFirstInstalledFamily_severalInstalled_takesTheFirstPreferred() throws Exception {
        String chosen = onFxThread(() -> UiFonts.findFirstInstalledFamily(
                List.of("Inter", "Segoe UI"), List.of("Segoe UI", "Inter")));

        assertEquals("Inter", chosen, "the order preferred decides, not the order installed");
    }
}
