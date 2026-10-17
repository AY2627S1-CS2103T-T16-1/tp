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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final Path EXTENSIONS = Paths.get(
            "src", "main", "resources", "view", "Extensions.css");
    private static final Path HELP_WINDOW = Paths.get(
            "src", "main", "resources", "view", "HelpWindow.css");

    /** The stylesheets the window is dressed with, whichever others it may name besides. */
    private static final List<String> INTENDED_STYLESHEETS = List.of("DarkTheme.css", "Extensions.css");

    /**
     * A colour written in place of a palette name, which the palette exists to prevent. Any
     * property can carry one, not only the ones named "color": a rule setting
     * {@code -fx-text-fill} or {@code -fx-fill} paints just as hard. A colour can be written
     * as a hex code, as a function, or by name.
     */
    private static final Pattern COLOUR_LITERAL = Pattern.compile(
            "-fx-[a-z-]+\\s*:[^;]*(#[0-9a-fA-F]{3,8}\\b|\\b(rgba?|hsba?)\\s*\\("
            + "|\\b(white|black|red|green|blue|yellow|orange|purple|pink|brown"
            + "|gr[ae]y|silver|navy|teal|cyan|magenta|maroon|olive|lime|aqua|fuchsia)\\b)");

    /** A palette name, wherever it is defined or used. */
    private static final Pattern PALETTE_NAME = Pattern.compile("-tab-[a-z0-9-]+");

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
        // the loop below checks nothing at all when the window names no stylesheet, which is
        // one of the ways it comes up unstyled, so the ones it is meant to name are asked for
        // by name rather than counted
        assertTrue(named.containsAll(INTENDED_STYLESHEETS),
                "expected the window to name " + INTENDED_STYLESHEETS + ", but it names " + named);

        for (String stylesheet : named) {
            URL resolved = ThemeTest.class.getResource("/view/" + stylesheet);
            assertNotNull(resolved, stylesheet + " is named by MainWindow.fxml but is not on the classpath");
        }
    }

    @Test
    public void darkTheme_definesEveryPaletteNameItUses() throws IOException {
        // a name that is not defined resolves to nothing and JavaFX drops the rule without a
        // word, so a slip of a letter takes a colour off the window silently
        Set<String> defined = definedPaletteNames();
        Set<String> used = new TreeSet<>(usedPaletteNames());
        used.removeAll(defined);

        assertTrue(used.isEmpty(), "these palette names are used but never defined: " + used);
    }

    @Test
    public void darkTheme_setsEachRuleOnce() throws IOException {
        String css = Files.readString(DARK_THEME, StandardCharsets.UTF_8);
        List<String> selectors = Pattern.compile("(?m)^([.#][^{\\n]*?)\\s*\\{").matcher(css)
                .results()
                .map(result -> result.group(1).trim())
                .toList();

        Set<String> seen = new TreeSet<>();
        Set<String> twice = new TreeSet<>();
        for (String selector : selectors) {
            if (!seen.add(selector)) {
                twice.add(selector);
            }
        }

        // the same selector written twice leaves which colour wins to the order of the file
        assertTrue(twice.isEmpty(), "these selectors are set more than once: " + twice);
    }

    /** The palette names the stylesheet defines, which is what the top block is for. */
    private static Set<String> definedPaletteNames() throws IOException {
        String css = Files.readString(DARK_THEME, StandardCharsets.UTF_8);
        String palette = css.substring(0, css.indexOf('}'));
        return PALETTE_NAME.matcher(palette).results()
                .map(MatchResult::group)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** The palette names the rules below the palette refer to. */
    private static Set<String> usedPaletteNames() throws IOException {
        String css = Files.readString(DARK_THEME, StandardCharsets.UTF_8);
        String belowThePalette = css.substring(css.indexOf('}') + 1);
        return PALETTE_NAME.matcher(belowThePalette).results()
                .map(MatchResult::group)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    public void everyStylesheet_namesEveryColourRatherThanRepeatingIt() throws IOException {
        // every stylesheet the window loads, not only the main one: a looked-up colour is
        // resolved against the node it is used on rather than the file it was written in, so
        // they all share one palette and a colour written into any of them escapes it
        List<String> literals = new ArrayList<>();
        for (Path stylesheet : List.of(DARK_THEME, EXTENSIONS, HELP_WINDOW)) {
            String css = Files.readString(stylesheet, StandardCharsets.UTF_8);
            String belowThePalette = stylesheet.equals(DARK_THEME)
                    ? css.substring(css.indexOf('}') + 1)
                    : css;
            Matcher matcher = COLOUR_LITERAL.matcher(belowThePalette);
            matcher.results()
                    .map(result -> stylesheet.getFileName() + ": " + MatchResultText.of(result))
                    .forEach(literals::add);
        }

        assertTrue(literals.isEmpty(),
                "these rules set a colour without going through the palette: " + literals);
    }

    /** Pulls the matched text out, so a failure names the rules at fault. */
    private static final class MatchResultText {
        private static String of(java.util.regex.MatchResult result) {
            return result.group().trim();
        }
    }

    @Test
    public void darkTheme_hidesASelectionOnlyWhereSomethingElseDrawsIt() throws IOException {
        String css = Files.readString(DARK_THEME, StandardCharsets.UTF_8);

        Set<String> hidden = new TreeSet<>();
        Matcher rules = Pattern.compile("(?m)^([.#][^{\\n]*?)\\s*\\{([^}]*)\\}").matcher(css);
        while (rules.find()) {
            if (rules.group(2).contains("-fx-highlight-text-fill: transparent")) {
                hidden.add(rules.group(1).trim());
            }
        }

        // text drawn in a transparent highlight disappears the moment it is selected, which
        // is what selecting it to copy it does first. One place may: the command field,
        // whose text and selection are both drawn by the copy over it
        assertEquals(Set.of(".command-line #commandTextField"), hidden,
                "these hide their own text when it is selected");
    }

    @Test
    public void everyStylesheet_namesOneFontFamilyAtMost() throws IOException {
        // JavaFX reads only the first family of a list and falls back to the system font
        // when it is missing, rather than trying the next as a browser does, so a list names
        // families the window will never use. The one the window is written in is chosen
        // against the families the machine has, in UiFonts, and inherited from there
        List<String> lists = new ArrayList<>();
        for (Path stylesheet : List.of(DARK_THEME, EXTENSIONS, HELP_WINDOW)) {
            String css = Files.readString(stylesheet, StandardCharsets.UTF_8);
            // the property may be spaced out, the shorthand says the same thing, and the
            // last declaration in a rule may have no semicolon after it
            Matcher families = Pattern.compile("-fx-font(-family)?\\s*:([^;}]*)").matcher(css);
            families.results()
                    .filter(result -> result.group(2).contains(","))
                    .map(result -> stylesheet.getFileName() + ": " + result.group().trim())
                    .forEach(lists::add);
        }

        assertTrue(lists.isEmpty(), "these name more than one family: " + lists);
    }
}
