package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

public class StatusBarFooterTest {

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    @Test
    public void constructor_longPath_keepsFilenameAndUpdatesTooltipOnResize() throws Exception {
        FxToolkit.onFxThread(() -> {
            Path path = Path.of("long directory name".repeat(20), "data", "tab.json");
            String fullPath = path.toAbsolutePath().normalize().toString();
            Region footer = footerAt(path, 490);
            Label label = (Label) footer.lookup("#saveLocationStatus");
            assertEquals(fullPath, label.getText());
            assertClippedPath(label, fullPath);

            footer.resize(label.prefWidth(-1) + 100, 40);
            footer.layout();
            assertEquals(fullPath, ((Text) label.lookup(".text")).getText());
            assertNull(label.getTooltip());

            footer.resize(490, 40);
            footer.layout();
            assertClippedPath(label, fullPath);
            return null;
        });
    }

    @Test
    public void constructor_shortPath_showsNormalizedPathWithoutTooltip() throws Exception {
        FxToolkit.onFxThread(() -> {
            Path path = Path.of("data", "unused", "..", "tab.json");
            Region footer = footerAt(path, 2000);
            Label label = (Label) footer.lookup("#saveLocationStatus");
            String fullPath = path.toAbsolutePath().normalize().toString();
            assertEquals(fullPath, label.getText());
            assertEquals(fullPath, ((Text) label.lookup(".text")).getText());
            assertNull(label.getTooltip());
            return null;
        });
    }

    private static Region footerAt(Path path, double width) {
        Region footer = new StatusBarFooter(path).getRoot();
        Scene scene = new Scene(footer, width, 40);
        scene.getStylesheets().add(UiPart.class.getResource("/view/DarkTheme.css").toExternalForm());
        footer.applyCss();
        footer.resize(width, 40);
        footer.layout();
        return footer;
    }

    private static void assertClippedPath(Label label, String fullPath) {
        String drawn = ((Text) label.lookup(".text")).getText();
        assertTrue(drawn.length() < fullPath.length());
        assertTrue(drawn.endsWith("tab.json"), drawn);
        assertNotNull(label.getTooltip());
        assertEquals(fullPath, label.getTooltip().getText());
    }
}
