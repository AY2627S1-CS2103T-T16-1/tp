package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.ui.CommandHighlighter.Kind.COMMAND_WORD;
import static seedu.tab.ui.CommandHighlighter.Kind.OPTION;
import static seedu.tab.ui.CommandHighlighter.Kind.QUOTED_VALUE;
import static seedu.tab.ui.CommandHighlighter.Kind.SPACE;
import static seedu.tab.ui.CommandHighlighter.Kind.VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.parser.CommandTokenizer;
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
    public void highlight_trailingBackslash_hasNothingToEscapeAndIsKept() {
        // a backslash typed as the last character escapes nothing, and must neither be
        // swallowed nor send the scan past the end of the command
        assertEquals(List.of(COMMAND_WORD, VALUE), kindsOf("add John\\"));
        assertEquals("add John\\", rejoined("add John\\"));
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

    @Test
    public void highlight_aNonBreakingSpace_endsTheTokenAsTheParserDoes() {
        // a command pasted from a web page carries these, and the parser splits on them
        assertEquals(List.of(COMMAND_WORD, VALUE), kindsOf("add\u00a0John"));
        assertEquals("add\u00a0John", rejoined("add\u00a0John"));
    }

    @Test
    public void highlight_aBackslashBeforeASpace_endsTheTokenAsTheParserDoes() {
        // a backslash escapes only the quote and another backslash, so this space still ends
        // the token however it is coloured
        assertEquals(List.of(COMMAND_WORD, VALUE, VALUE), kindsOf("add a\\ b"));
    }

    @Test
    public void highlight_aBackslashBeforeAQuote_keepsTheQuoteInTheToken() {
        // the escaped quote does not open a quoted value, as it does not for the parser
        assertEquals(List.of(COMMAND_WORD, VALUE), kindsOf("add \\\"one"));
    }

    @Test
    public void highlight_everyCommand_breaksWhereTheParserDoes() throws Exception {
        // the colouring tells the User where one token ends and the next begins, so it has to
        // break a command in the same places the parser does
        for (String command : List.of("list", "add John -p 123", "add \"Siti Nur-Aisyah\"",
                "add a\\ b", "add \\\"one", "add\u00a0John", "find  Siti   Nur", "edit 1 -t \"\"",
                "add \"-Ahmad\" -p 84001122", "add John\u00a0-p\u00a0123")) {
            long coloured = CommandHighlighter.highlight(command).stream()
                    .filter(span -> span.kind() != SPACE)
                    .count();
            long parsed = CommandTokenizer.tokenize(command).size();
            assertEquals(parsed, coloured, "disagreed about \"" + command + "\"");
        }
    }
}
