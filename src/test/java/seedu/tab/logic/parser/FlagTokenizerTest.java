package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;
import static seedu.tab.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.logic.parser.exceptions.ParseException;

public class FlagTokenizerTest {

    private static FlagArgumentMap parse(String arguments) throws ParseException {
        return FlagTokenizer.tokenize(arguments, FLAG_PHONE, FLAG_EMAIL, FLAG_TAG);
    }

    @Test
    public void tokenize_nameThenFlags_readsBoth() throws Exception {
        FlagArgumentMap parsed = parse("John -p 98765432 -e john@example.com -t T1");

        assertEquals("John", parsed.getName());
        assertEquals(Optional.of("98765432"), parsed.getValue(FLAG_PHONE));
        assertEquals(Optional.of("john@example.com"), parsed.getValue(FLAG_EMAIL));
        assertEquals(List.of("T1"), parsed.getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_unquotedNameOfSeveralWords_joinsThem() throws Exception {
        assertEquals("Siti Nur-Aisyah", parse("Siti Nur-Aisyah -t T1").getName());
        assertEquals("Ma Ying-jeou", parse("Ma Ying-jeou -t T1").getName());
    }

    @Test
    public void tokenize_quotedName_keepsWhatWouldOtherwiseBeRead() throws Exception {
        // a slash no longer starts a field, and a quoted hyphen no longer reads as a flag
        assertEquals("Ravi s/o Kumaran", parse("\"Ravi s/o Kumaran\" -t T1").getName());
        assertEquals("-Ahmad", parse("\"-Ahmad\" -t T2").getName());
    }

    @Test
    public void tokenize_repeatedTagFlag_keepsEveryValueInOrder() throws Exception {
        FlagArgumentMap parsed = parse("\"Ravi s/o Kumaran\" -t T1 -t \"Lab 3\"");

        assertEquals("Ravi s/o Kumaran", parsed.getName());
        assertEquals(List.of("T1", "Lab 3"), parsed.getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_noFlags_isAllName() throws Exception {
        assertEquals("John Doe", parse("John Doe").getName());
    }

    @Test
    public void tokenize_nothingGiven_leavesEverythingEmpty() throws Exception {
        FlagArgumentMap parsed = parse("");

        assertEquals("", parsed.getName());
        assertEquals(Optional.empty(), parsed.getValue(FLAG_PHONE));
        assertEquals(List.of(), parsed.getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_flagsWithNoName_leavesTheNameEmpty() throws Exception {
        assertEquals("", parse("-p 98765432").getName());
    }

    @Test
    public void tokenize_loneHyphen_isAValueRatherThanAFlag() throws Exception {
        // a hyphen on its own marks no field, so it is read as part of what surrounds it
        assertEquals("Jean - Luc", parse("Jean - Luc -t T1").getName());
    }

    @Test
    public void tokenize_loneHyphenAfterTheFlagsBegan_belongsToNoOption() {
        assertThrows(ParseException.class, String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "-"), () ->
                parse("Jean -t T1 -"));
    }

    @Test
    public void tokenize_unknownFlag_saysThereIsNoSuchOption() {
        assertThrows(ParseException.class, String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-z"), () ->
                parse("John -z something"));
    }

    @Test
    public void tokenize_flagWithoutValue_namesTheFieldItNeeds() {
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_EMAIL.getLabel()), () ->
                    parse("John -e"));
    }

    @Test
    public void tokenize_valueAfterTheFlagsBegan_saysItBelongsToNoOption() {
        // the likeliest cause is an unquoted name, so the message says so
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_VALUE_AFTER_FLAGS, "Kumaran"), () ->
                    parse("Ravi -t T1 Kumaran"));
    }

    @Test
    public void tokenize_unclosedQuote_isReportedAsSuch() {
        assertThrows(ParseException.class, CommandTokenizer.MESSAGE_UNCLOSED_QUOTE, () ->
                parse("\"John -t T1"));
    }

    @Test
    public void verifyNoDuplicateFlagsFor_repeatedSingleValuedFlag_throws() throws Exception {
        FlagArgumentMap parsed = parse("John -e a@b.com -e c@d.com");

        assertEquals(Optional.of("c@d.com"), parsed.getValue(FLAG_EMAIL));
        assertThrows(ParseException.class, Messages.getErrorMessageForDuplicateFlags(FLAG_EMAIL), () ->
                parsed.verifyNoDuplicateFlagsFor(FLAG_PHONE, FLAG_EMAIL));
    }

    @Test
    public void verifyNoDuplicateFlagsFor_repeatedTagFlag_doesNotThrow() throws Exception {
        parse("John -t T1 -t T2").verifyNoDuplicateFlagsFor(FLAG_PHONE, FLAG_EMAIL);
    }

    @Test
    public void tokenize_emptyQuotedValue_reachesTheFlag() throws Exception {
        // the field parser decides whether an empty value is allowed, not the tokenizer
        assertEquals(Optional.of(""), parse("John -e \"\"").getValue(FLAG_EMAIL));
    }
}
