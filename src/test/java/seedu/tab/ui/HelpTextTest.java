package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.text.Text;
import seedu.tab.logic.commands.HelpCommand;

public class HelpTextTest {

    @BeforeAll
    public static void startToolkit() throws Exception {
        FxToolkit.start();
    }

    /** Returns the spans as "text|class" pairs, with the runs of spaces left out. */
    private static List<String> spansOf(String reference) throws Exception {
        return FxToolkit.onFxThread(() -> HelpText.color(reference).stream()
                .filter(createSpan -> !createSpan.getText().isBlank())
                .map(createSpan -> createSpan.getText() + "|" + determineStyle(createSpan))
                .collect(Collectors.toList()));
    }

    private static String determineStyle(Text createSpan) {
        return createSpan.getStyleClass().stream().filter(name -> !name.equals("help-token")).findFirst().orElseThrow();
    }

    private static String joined(String reference) throws Exception {
        return FxToolkit.onFxThread(() -> HelpText.color(reference).stream()
                .map(Text::getText)
                .collect(Collectors.joining()));
    }

    @Test
    public void color_wholeReference_keepsEveryCharacter() throws Exception {
        // the pager measures the block by its lines, and the user reads it: coloring must not
        // add, drop or reorder so much as a space
        assertEquals(HelpCommand.SHOWING_HELP_MESSAGE, joined(HelpCommand.SHOWING_HELP_MESSAGE));
    }

    @Test
    public void color_lineAgainstTheMargin_isAHeading() throws Exception {
        assertEquals(List.of("TAB COMMAND REFERENCE|help-heading"), spansOf("TAB COMMAND REFERENCE"));
        assertEquals(List.of("COMMANDS|help-heading"), spansOf("COMMANDS"));
    }

    @Test
    public void color_syntaxLine_marksTheCommandWordOptionsAndPlaceholders() throws Exception {
        assertEquals(List.of(
                        "add|help-command-word",
                        "NAME|help-placeholder",
                        "-p|help-option",
                        "PHONE|help-placeholder"),
                spansOf("    add NAME -p PHONE"));
    }

    @Test
    public void color_optionalField_leavesItsBracketsPlain() throws Exception {
        // the brackets say the field may be left out, which is not part of the marker
        assertEquals(List.of(
                        "[|help-plain",
                        "--id|help-option",
                        "NUS_STUDENT_ID|help-placeholder",
                        "]|help-plain"),
                spansOf("        [--id NUS_STUDENT_ID]"));
    }

    @Test
    public void color_commandWordInASentence_staysPlain() throws Exception {
        // coloring the first of four and none of the rest would read as an accident
        List<String> spans = spansOf("  help, list, clear and exit take no arguments.");
        assertTrue(spans.stream().noneMatch(createSpan -> createSpan.endsWith("|help-command-word")), spans.toString());
        assertEquals("help|help-plain", spans.get(0));
    }

    @Test
    public void color_prose_staysPlain() throws Exception {
        assertEquals(List.of(
                        "The|help-plain", "target|help-plain", "may|help-plain", "be|help-plain",
                        "an|help-plain", "ID|help-plain", "instead|help-plain", "of|help-plain",
                        "an|help-plain", "index|help-plain", ".|help-plain"),
                spansOf("    The target may be an ID instead of an index."));
    }

    @Test
    public void color_blankLine_isPlain() throws Exception {
        assertEquals(List.of(), spansOf(""));
        assertEquals("\n\n", joined("\n\n"));
    }
}
