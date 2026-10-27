package seedu.tab.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.ui.CommandHighlighter.Kind.COMMAND_WORD;
import static seedu.tab.ui.CommandHighlighter.Kind.OPTION;
import static seedu.tab.ui.CommandHighlighter.Kind.QUOTED_VALUE;
import static seedu.tab.ui.CommandHighlighter.Kind.SPACE;
import static seedu.tab.ui.CommandHighlighter.Kind.VALUE;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.parser.CommandTokenizer;
import seedu.tab.ui.CommandHighlighter.Kind;
import seedu.tab.ui.CommandHighlighter.Span;

public class CommandHighlighterTest {

    /**
     * The kinds in order, with the runs of whitespace left out, which carry no color.
     */
    private static List<Kind> kindsOf(String command) {
        return CommandHighlighter.highlight(command).stream()
                .map(Span::kind)
                .filter(kind -> kind != SPACE)
                .toList();
    }

    /**
     * Everything typed must come back, or the colored line would not read as it was typed.
     */
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
    public void highlight_supportedQuotePairs_matchParserAndPreserveText() throws Exception {
        for (String opening : List.of("\"", "“", "”", "＂")) {
            for (String closing : List.of("\"", "“", "”", "＂")) {
                String command = "edit 1 --tag=" + opening + "-组 1" + closing + " -f";
                assertEquals(List.of(COMMAND_WORD, VALUE, OPTION, QUOTED_VALUE, OPTION), kindsOf(command));
                assertEquals(command, rejoined(command));
                assertEquals(CommandTokenizer.tokenize(command.substring(4)).size(), argumentTokensOf(command));
            }
            assertEquals(List.of(COMMAND_WORD, VALUE, OPTION), kindsOf("add a\\" + opening + "b -f"));
            assertEquals(List.of(COMMAND_WORD, QUOTED_VALUE), kindsOf("add " + opening + "Unclosed value"));
            assertEquals(List.of(COMMAND_WORD, QUOTED_VALUE), kindsOf("add --ta" + opening + "g=x"));
        }
    }

    @Test
    public void highlight_escapedQuote_doesNotOpenAQuotedValue() {
        assertEquals(List.of(COMMAND_WORD, VALUE, VALUE, VALUE, VALUE, OPTION, VALUE),
                kindsOf("add Dwayne \\\"The Rock\\\" Johnson -p 91234567"));
    }

    @Test
    public void highlight_unclosedQuote_coloursWhatIsThere() {
        // half-typed commands are ordinary here: the parser complains, the coloring does not
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
        // the token however it is colored
        assertEquals(List.of(COMMAND_WORD, VALUE, VALUE), kindsOf("add a\\ b"));
    }

    @Test
    public void highlight_aBackslashBeforeAQuote_keepsTheQuoteInTheToken() {
        // the escaped quote does not open a quoted value, as it does not for the parser
        assertEquals(List.of(COMMAND_WORD, VALUE), kindsOf("add \\\"one"));
    }

    @Test
    public void highlight_quoteAttachedToTheCommandWord_endsTheWordWhereTheParserDoes() {
        // the parser ends the command word at the first separator and answers
        // `Unknown command "add"Ravi"`, so the bold word has to end there too
        assertEquals(new Span("add\"Ravi", COMMAND_WORD),
                CommandHighlighter.highlight("add\"Ravi s/o Kumaran\" -p 91234567").getFirst());
        assertEquals(new Span("find\"Lab", COMMAND_WORD),
                CommandHighlighter.highlight("find\"Lab 3\"").getFirst());
        assertEquals(List.of(COMMAND_WORD, QUOTED_VALUE), kindsOf("find\"Lab 3\""));
    }

    @Test
    public void highlight_optionCarryingItsValue_coloursTheOptionApartFromTheValue() {
        // the parser reads --tag="Lab 3" as the option --tag and the value Lab 3
        List<Span> spans = CommandHighlighter.highlight("edit 1 --tag=\"Lab 3\"");
        assertEquals(List.of(new Span("--tag=", OPTION), new Span("\"Lab 3\"", QUOTED_VALUE)),
                spans.subList(spans.size() - 2, spans.size()));
        assertEquals(List.of(COMMAND_WORD, VALUE, OPTION, VALUE, OPTION, QUOTED_VALUE, OPTION, QUOTED_VALUE),
                kindsOf("add Mei --phone=91234567 --tag=\"Lab 3\" -t \"Lab 4\""));
    }

    @Test
    public void highlight_attachedValueOpeningWithTheOptionMarker_isStillAValue() {
        assertEquals(List.of(COMMAND_WORD, VALUE, OPTION, VALUE), kindsOf("edit 1 --tag=-weird"));
    }

    @Test
    public void highlight_optionWithNothingAfterItsEqualsSign_addsNoEmptySpan() {
        assertEquals(List.of(COMMAND_WORD, VALUE, OPTION), kindsOf("edit 1 --tag="));
        assertEquals("edit 1 --tag=", rejoined("edit 1 --tag="));
    }

    @Test
    public void highlight_equalsSignWithNoMarkerBeforeIt_isOneOption() {
        // the parser wants a marker longer than the hyphen before the equals sign
        assertEquals(List.of(COMMAND_WORD, OPTION), kindsOf("add -=x"));
    }

    @Test
    public void highlight_quotedWordHoldingAnEqualsSign_isOneValue() {
        // a word the quotes open is a value to the parser, equals sign and all
        assertEquals(List.of(COMMAND_WORD, QUOTED_VALUE), kindsOf("add \"--tag=Lab 3\""));
    }

    @Test
    public void highlight_markerWrittenWithQuotesOrEscapes_isLeftWhole() {
        // a quote or a backslash moves the equals sign the parser counts to, so rather than
        // split such a word in the wrong place the coloring leaves it alone
        assertEquals(List.of(COMMAND_WORD, QUOTED_VALUE), kindsOf("add --ta\"g\"=x"));
        assertEquals(List.of(COMMAND_WORD, OPTION), kindsOf("add --ta\\\\g=x"));
    }

    /**
     * Returns how many tokens the coloring claims the arguments hold, counting an option
     * and the value attached to it by an equals sign as the one token they were typed as.
     */
    private static long argumentTokensOf(String command) {
        long tokens = 0;
        Span previous = null;
        for (Span span : CommandHighlighter.highlight(command)) {
            if (span.kind() == SPACE) {
                previous = null;
                continue;
            }
            boolean isAttachedValue = previous != null && previous.kind() == OPTION
                    && previous.text().endsWith("=");
            if (!isAttachedValue) {
                tokens++;
            }
            previous = span;
        }
        return tokens - 1; // the command word is not one of the arguments
    }

    @Test
    public void highlight_everyCommand_breaksWhereTheParserDoes() throws Exception {
        // the coloring tells the User where one token ends and the next begins, so it has to
        // break a command in the same places the parser does: the command word at the first
        // separator, and the arguments after it the way the tokenizer splits them
        for (String command : List.of("list", "add John -p 123", "add \"Siti Nur-Aisyah\"",
                "add a\\ b", "add \\\"one", "add\u00a0John", "find  Siti   Nur", "edit 1 -t \"\"",
                "add \"-Ahmad\" -p 84001122", "add John\u00a0-p\u00a0123", "add\"\"x y",
                "edit 1 --tag=\"Lab 3\"", "add Mei --phone=91234567", "add \"--tag=x\"",
                "add -=x", "edit 1 --tag=", "edit 1 --tag= x")) {
            int wordEnd = endOfCommandWord(command);
            long parsed = CommandTokenizer.tokenize(command.substring(wordEnd)).size();
            assertEquals(parsed, argumentTokensOf(command), "disagreed about \"" + command + "\"");
        }
    }

    /**
     * Returns where the parser ends the command word, which is at the first separator.
     */
    private static int endOfCommandWord(String command) {
        for (int index = 0; index < command.length(); index++) {
            if (StringUtil.isWhitespace(command.charAt(index))) {
                return index;
            }
        }
        return command.length();
    }
}
