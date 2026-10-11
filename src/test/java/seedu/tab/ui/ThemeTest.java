package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Checks the window's chrome and the stylesheet it is dressed with. A stylesheet JavaFX cannot
 * resolve is dropped without a word at runtime, so the window would simply come up unstyled.
 */
public class ThemeTest {

    private static final Path MAIN_WINDOW = Paths.get(
            "src", "main", "resources", "view", "MainWindow.fxml");
    private static final Path DARK_THEME = Paths.get(
            "src", "main", "resources", "view", "DarkTheme.css");

    /** A colour written in place of a palette name, which the palette exists to prevent. */
    private static final Pattern COLOUR_LITERAL = Pattern.compile(
            "-fx-[a-z-]*color[a-z-]*\\s*:[^;]*(#[0-9a-fA-F]{3,6}|\\bwhite\\b)");

    @Test
    public void mainWindow_isTitledAfterTheProduct() throws IOException {
        String fxml = Files.readString(MAIN_WINDOW, StandardCharsets.UTF_8);

        assertTrue(fxml.contains("title=\"TAB\""),
                "the window should carry the product's name, not AddressBook's");
    }

    @Test
    public void everyStylesheetTheWindowNames_resolves() throws IOException {
        String fxml = Files.readString(MAIN_WINDOW, StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("<URL value=\"@([^\"]+)\" */>").matcher(fxml);

        List<String> named = matcher.results().map(result -> result.group(1)).toList();
        assertEquals(2, named.size(), "expected the dark theme and the extensions");

        for (String stylesheet : named) {
            URL resolved = ThemeTest.class.getResource("/view/" + stylesheet);
            assertNotNull(resolved, stylesheet + " is named by MainWindow.fxml but is not on the classpath");
        }
    }

    @Test
    public void darkTheme_definesThePaletteItUses() throws IOException {
        String css = Files.readString(DARK_THEME, StandardCharsets.UTF_8);

        for (String name : List.of("-tab-bg", "-tab-surface", "-tab-border", "-tab-text",
                "-tab-text-muted", "-tab-accent")) {
            assertTrue(css.contains(name + ":"), name + " is used but never defined");
        }
    }

    @Test
    public void darkTheme_namesEveryColourRatherThanRepeatingIt() throws IOException {
        String css = Files.readString(DARK_THEME, StandardCharsets.UTF_8);
        String belowThePalette = css.substring(css.indexOf('}') + 1);

        Matcher matcher = COLOUR_LITERAL.matcher(belowThePalette);
        List<String> literals = matcher.results().map(MatchResultText::of).toList();

        assertTrue(literals.isEmpty(),
                "these rules set a colour without going through the palette: " + literals);
    }

    /** Pulls the matched text out, so a failure names the rules at fault. */
    private static final class MatchResultText {
        private static String of(java.util.regex.MatchResult result) {
            return result.group().trim();
        }
    }
}
