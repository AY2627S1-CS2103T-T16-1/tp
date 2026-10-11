package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.ui.CommandHighlighter.Kind.COMMAND_WORD;
import static seedu.tab.ui.CommandHighlighter.Kind.OPTION;
import static seedu.tab.ui.CommandHighlighter.Kind.QUOTED_VALUE;
import static seedu.tab.ui.CommandHighlighter.Kind.SPACE;
import static seedu.tab.ui.CommandHighlighter.Kind.VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.ui.CommandHighlighter.Kind;
import seedu.tab.ui.CommandHighlighter.Span;

public class CommandHighlighterTest {

    /** The kinds in order, with the runs of whitespace left out, which carry no colour. */
    private static List<Kind> kindsOf(String command) {
        return CommandHighlighter.highlight(command).stream()
                .map(Span::kind)
                .filter(kind -> kind != SPACE)
                .toList();
    }

    /** Everything typed must come back, or the coloured line would not read as it was typed. */
    private static String rejoined(String command) {
        return CommandHighlighter.highlight(command).stream().map(Span::text)
                .reduce("", String::concat);
    }

    @Test
    public void highlight_everyCommand_coversEveryCharacter() {
        for (String command : List.of("", "   ", "list", "add John -p 123",
                "edit 1 -n \"Ravi s/o Kumaran\" --follow-up", "add \"unclosed -p 1",
                "add Dwayne \\\"The Rock\\\" Johnson -p 1")) {
            assertEquals(command, rejoined(command), "lost text in: " + command);
        }
    }

    @Test
    public void highlight_commandWithOptionsAndValues_tellsThemApart() {
        assertEquals(List.of(COMMAND_WORD, VALUE, OPTION, VALUE),
                kindsOf("add John -p 98765432"));
    }

    @Test
    public void highlight_longOption_isAnOptionToo() {
        assertEquals(List.of(COMMAND_WORD, VALUE, OPTION), kindsOf("edit 1 --follow-up"));
    }

    @Test
    public void highlight_quotedValue_isAValueEvenWhenItOpensWithTheOptionMarker() {
        // the quotes are what separate the value "-Ahmad" from the option -p
        assertEquals(List.of(COMMAND_WORD, QUOTED_VALUE, OPTION, VALUE),
                kindsOf("add \"-Ahmad\" -p 84001122"));
    }

    @Test
    public void highlight_quotedValueHoldingSpaces_staysOneSpan() {
        assertEquals(List.of(COMMAND_WORD, VALUE, OPTION, QUOTED_VALUE),
                kindsOf("edit 1 -t \"Lab 3\""));
    }

    @Test
    public void highlight_escapedQuote_doesNotOpenAQuotedValue() {
        assertEquals(List.of(COMMAND_WORD, VALUE, VALUE, VALUE, VALUE, OPTION, VALUE),
                kindsOf("add Dwayne \\\"The Rock\\\" Johnson -p 91234567"));
    }

    @Test
    public void highlight_unclosedQuote_coloursWhatIsThere() {
        // half-typed commands are ordinary here: the parser complains, the colouring does not
        assertEquals(List.of(COMMAND_WORD, QUOTED_VALUE), kindsOf("add \"Siti Nur-Aisyah"));
    }

    @Test
    public void highlight_unknownCommandWord_isStillTheCommandWord() {
        assertEquals(List.of(COMMAND_WORD, VALUE), kindsOf("frobnicate 1"));
    }

    @Test
    public void highlight_nothingTyped_hasNoSpans() {
        assertEquals(List.of(), CommandHighlighter.highlight(""));
    }

    @Test
    public void highlight_leadingSpace_doesNotBecomeTheCommandWord() {
        assertEquals(List.of(COMMAND_WORD, VALUE), kindsOf("   list 3"));
    }

    @Test
    public void highlight_aBareHyphen_isAValueRatherThanAnOption() {
        // "-" alone marks nothing, exactly as the parser reads it
        assertEquals(List.of(COMMAND_WORD, VALUE), kindsOf("add -"));
    }
}
