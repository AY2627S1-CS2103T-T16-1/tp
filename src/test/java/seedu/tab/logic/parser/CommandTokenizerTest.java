package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.logic.parser.CommandTokenizer.tokenize;
import static seedu.tab.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.parser.CommandTokenizer.Token;
import seedu.tab.logic.parser.exceptions.ParseException;

public class CommandTokenizerTest {

    /** Returns the text of each token, which is what most of these tests are about. */
    private static List<String> valuesOf(String arguments) throws ParseException {
        return tokenize(arguments).stream().map(Token::value).toList();
    }

    @Test
    public void tokenize_nothingToSplit_returnsNoTokens() throws Exception {
        assertEquals(List.of(), valuesOf(""));
        assertEquals(List.of(), valuesOf("   "));
        assertEquals(List.of(), valuesOf("\t\n "));
    }

    @Test
    public void tokenize_plainWords_splitsOnWhitespace() throws Exception {
        assertEquals(List.of("John", "-t", "T1"), valuesOf("John -t T1"));
    }

    @Test
    public void tokenize_surroundingAndRepeatedSpaces_areIgnored() throws Exception {
        assertEquals(List.of("John", "-t", "T1"), valuesOf("   John    -t\t\tT1  "));
    }

    @Test
    public void tokenize_quotedValue_isOneToken() throws Exception {
        assertEquals(List.of("Siti Nur-Aisyah", "-t", "Lab 3"),
                valuesOf("\"Siti Nur-Aisyah\" -t \"Lab 3\""));
    }

    @Test
    public void tokenize_quotedValue_keepsCharactersThatWouldOtherwiseBeRead() throws Exception {
        // a name holding a slash, and one opening with the hyphen that marks a flag
        assertEquals(List.of("Ravi s/o Kumaran"), valuesOf("\"Ravi s/o Kumaran\""));
        assertEquals(List.of("-Ahmad", "-t", "T2"), valuesOf("\"-Ahmad\" -t T2"));
    }

    @Test
    public void tokenize_quotesInsideAToken_joinWithWhatSurroundsThem() throws Exception {
        // the quote groups, it does not delimit, so a token may be part quoted
        assertEquals(List.of("Lab 3x"), valuesOf("\"Lab 3\"x"));
        assertEquals(List.of("ab cd"), valuesOf("a\"b c\"d"));
    }

    @Test
    public void tokenize_emptyQuotes_areATokenOfTheirOwn() throws Exception {
        // "" is how a caller spells a value that is deliberately empty
        assertEquals(List.of(""), valuesOf("\"\""));
        assertEquals(List.of("-e", ""), valuesOf("-e \"\""));
    }

    @Test
    public void tokenize_whitespaceInsideQuotes_isKept() throws Exception {
        assertEquals(List.of("Tan  Wei   Ming"), valuesOf("\"Tan  Wei   Ming\""));
    }

    @Test
    public void tokenize_quotedToken_isMarkedAsQuoted() throws Exception {
        // a value that opens with the flag marker is only telling apart from a flag by this
        List<Token> tokens = tokenize("\"-Ahmad\" -t T2");
        assertEquals(new Token("-Ahmad", true), tokens.get(0));
        assertEquals(new Token("-t", false), tokens.get(1));
        assertEquals(new Token("T2", false), tokens.get(2));
    }

    @Test
    public void tokenize_partlyQuotedToken_countsAsQuoted() throws Exception {
        // a quote anywhere in the token means the user spelled a value, not an option
        assertEquals(new Token("-tx", true), tokenize("\"-t\"x").get(0));
    }

    @Test
    public void tokenize_unclosedQuote_throwsParseException() {
        assertThrows(ParseException.class, CommandTokenizer.MESSAGE_UNCLOSED_QUOTE, () ->
                valuesOf("\"John -t T1"));
        assertThrows(ParseException.class, CommandTokenizer.MESSAGE_UNCLOSED_QUOTE, () ->
                valuesOf("John -t \"Lab 3"));
    }

    @Test
    public void tokenize_nonAsciiValues_areLeftIntact() throws Exception {
        assertEquals(List.of("陈伟明", "-t", "ᠮᠣᠩᠭᠣᠯ ᠪᠢᠴᠢᠭ"),
                valuesOf("陈伟明 -t \"ᠮᠣᠩᠭᠣᠯ ᠪᠢᠴᠢᠭ\""));
    }

    @Test
    public void tokenize_supplementaryCharacters_surviveAsAPair() throws Exception {
        // a surrogate pair must not be split, or the name comes back as two broken halves
        String withEmoji = "Ann😀Lee";
        assertEquals(List.of(withEmoji), valuesOf(withEmoji));
    }
}
