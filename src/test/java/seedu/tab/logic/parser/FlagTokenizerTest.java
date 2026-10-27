package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.parser.CliFlags.FLAG_ADD_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_NO_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_REMOVE_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;
import static seedu.tab.testutil.Assert.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.Messages;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.logic.parser.exceptions.UnknownFlagException;

public class FlagTokenizerTest {

    private static FlagArgumentMap parse(String arguments) throws ParseException {
        return FlagTokenizer.tokenize(arguments, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID, FLAG_TAG, FLAG_FOLLOW_UP);
    }

    @Test
    public void tokenizeWithIndex_negativeNumber_reportsInvalidIndexAndOption() {
        for (String value : List.of("-1", "-0", "-1.5", "-2147483649", "-" + "9".repeat(100))) {
            String expected = ParserUtil.MESSAGE_INVALID_INDEX + "\n"
                    + String.format(Messages.MESSAGE_UNKNOWN_FLAG, Messages.shorten(value));
            assertThrows(ParseException.class, expected, () ->
                    FlagTokenizer.tokenizeWithIndex(value + " -p 123", FLAG_PHONE));
        }
    }

    @Test
    public void tokenizeWithIndex_nonNegativePreamble_usesNormalFlagRules() throws Exception {
        assertEquals("", FlagTokenizer.tokenizeWithIndex("", FLAG_PHONE).getPreamble());
        assertEquals("", FlagTokenizer.tokenizeWithIndex("-p 123", FLAG_PHONE).getPreamble());
        assertEquals("1", FlagTokenizer.tokenizeWithIndex("1 -p 123", FLAG_PHONE).getPreamble());
        assertEquals("-1", FlagTokenizer.tokenizeWithIndex("\"-1\" -p 123", FLAG_PHONE).getPreamble());
        assertThrows(ParseException.class, "There is no -x option.", () ->
                FlagTokenizer.tokenizeWithIndex("-x -p 123", FLAG_PHONE));
        assertThrows(ParseException.class, String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, "-p PHONE"), () ->
                FlagTokenizer.tokenizeWithIndex("1 -p -1", FLAG_PHONE));
    }

    @Test
    public void tokenize_nameThenFlags_readsBoth() throws Exception {
        FlagArgumentMap parsed = parse("John -p 98765432 -e john@example.com -t T1");

        assertEquals("John", parsed.getPreamble());
        assertEquals(Optional.of("98765432"), parsed.getValue(FLAG_PHONE));
        assertEquals(Optional.of("john@example.com"), parsed.getValue(FLAG_EMAIL));
        assertEquals(List.of("T1"), parsed.getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_longAliases_readsTheSameFlags() throws Exception {
        FlagArgumentMap parsed = parse(
                "John --phone 98765432 --email john@example.com --tag T1 -t \"Lab 3\"");

        assertEquals("John", parsed.getPreamble());
        assertEquals(Optional.of("98765432"), parsed.getValue(FLAG_PHONE));
        assertEquals(Optional.of("john@example.com"), parsed.getValue(FLAG_EMAIL));
        assertEquals(List.of("T1", "Lab 3"), parsed.getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_valueTakingFlagAlias_readsTheSameField() throws Exception {
        assertEquals(Optional.of("A01234567X"), parse("John -i A01234567X").getValue(FLAG_NUS_STUDENT_ID));
        assertEquals(Optional.of("A76543210Y"),
                parse("John --id A76543210Y").getValue(FLAG_NUS_STUDENT_ID));
    }

    @Test
    public void tokenize_valuesJoinedByEquals_readsEveryValueTakingOptionAndAlias() throws Exception {
        for (CliFlag flag : List.of(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID,
                FLAG_TAG, FLAG_ADD_TAG, FLAG_REMOVE_TAG)) {
            for (String marker : flag.getMarkers()) {
                FlagArgumentMap parsed = FlagTokenizer.tokenize(marker + "=value", flag);

                assertEquals(Optional.of("value"), parsed.getValue(flag));
            }
        }
    }

    @Test
    public void tokenize_valueContainingEquals_splitsOnlyOnTheFirstEquals() throws Exception {
        assertEquals(List.of("needs=review=soon"),
                parse("John --tag=needs=review=soon").getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_unquotedEmptyValueAfterEquals_saysFlagNeedsValue() {
        for (CliFlag flag : List.of(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID,
                FLAG_TAG, FLAG_ADD_TAG, FLAG_REMOVE_TAG)) {
            for (String marker : flag.getMarkers()) {
                assertThrows(ParseException.class,
                        String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, flag.getLabel(marker)), () ->
                            FlagTokenizer.tokenize(marker + "=", flag));
            }
        }
    }

    @Test
    public void tokenize_quotedEmptyValueAfterEquals_reachesTheFlag() throws Exception {
        for (CliFlag flag : List.of(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID,
                FLAG_TAG, FLAG_ADD_TAG, FLAG_REMOVE_TAG)) {
            for (String marker : flag.getMarkers()) {
                assertEquals(Optional.of(""),
                        FlagTokenizer.tokenize(marker + "=\"\"", flag).getValue(flag));
            }
        }
    }

    @Test
    public void tokenize_quotedAttachedValue_keepsSpacesAndLeadingHyphen() throws Exception {
        assertEquals(List.of("Lab 3", "-urgent"),
                parse("John --tag=\"Lab 3\" -t=-urgent").getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_fullyQuotedEqualsForm_staysInThePreamble() throws Exception {
        assertEquals("--phone=123", parse("\"--phone=123\"").getPreamble());
    }

    @Test
    public void tokenize_unquotedNameOfSeveralWords_joinsThem() throws Exception {
        assertEquals("Siti Nur-Aisyah", parse("Siti Nur-Aisyah -t T1").getPreamble());
        assertEquals("Ma Ying-jeou", parse("Ma Ying-jeou -t T1").getPreamble());
    }

    @Test
    public void tokenize_quotedName_keepsWhatWouldOtherwiseBeRead() throws Exception {
        // a slash no longer starts a field, and a quoted hyphen no longer reads as a flag
        assertEquals("Ravi s/o Kumaran", parse("\"Ravi s/o Kumaran\" -t T1").getPreamble());
        assertEquals("-Ahmad", parse("\"-Ahmad\" -t T2").getPreamble());
    }

    @Test
    public void tokenize_repeatedTagFlag_keepsEveryValueInOrder() throws Exception {
        FlagArgumentMap parsed = parse("\"Ravi s/o Kumaran\" -t T1 -t \"Lab 3\"");

        assertEquals("Ravi s/o Kumaran", parsed.getPreamble());
        assertEquals(List.of("T1", "Lab 3"), parsed.getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_noFlags_isAllName() throws Exception {
        assertEquals("John Doe", parse("John Doe").getPreamble());
    }

    @Test
    public void tokenize_nothingGiven_leavesEverythingEmpty() throws Exception {
        FlagArgumentMap parsed = parse("");

        assertEquals("", parsed.getPreamble());
        assertEquals(Optional.empty(), parsed.getValue(FLAG_PHONE));
        assertEquals(List.of(), parsed.getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_flagsWithNoName_leavesTheNameEmpty() throws Exception {
        assertEquals("", parse("-p 98765432").getPreamble());
    }

    @Test
    public void tokenize_loneHyphen_isAValueRatherThanAFlag() throws Exception {
        // a hyphen on its own marks no field, so it is read as part of what surrounds it
        assertEquals("Jean - Luc", parse("Jean - Luc -t T1").getPreamble());
    }

    @Test
    public void tokenize_loneHyphenAfterTheFlagsBegan_belongsToNoOption() {
        assertThrows(ParseException.class, Messages.getErrorMessageForValueAfterFlags("-", true), () ->
                parse("Jean -t T1 -"));
    }

    @Test
    public void tokenize_sameMarkerNamedTwice_readsItAsOneFlag() throws Exception {
        // a caller listing a marker twice means one field, and must not break the collector
        FlagArgumentMap parsed = FlagTokenizer.tokenize(" John -e a@b.com",
                FLAG_EMAIL, new CliFlag("-e", "MAIL"));
        assertEquals(Optional.of("a@b.com"), parsed.getValue(FLAG_EMAIL));
    }

    @Test
    public void tokenize_unknownFlag_saysThereIsNoSuchOption() {
        assertThrows(ParseException.class, String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-z"), () ->
                parse("John -z something"));
        assertThrows(ParseException.class, String.format(Messages.MESSAGE_UNKNOWN_FLAG, "--unknown"), () ->
                parse("John --unknown something"));
        assertThrows(ParseException.class, String.format(Messages.MESSAGE_UNKNOWN_FLAG, "--phon"), () ->
                parse("John --phon=1"));
        assertThrows(ParseException.class, String.format(Messages.MESSAGE_UNKNOWN_FLAG, "--unknown"), () ->
                parse("John --unknown=\"some value\""));
    }

    @Test
    public void tokenize_flagWithoutValue_namesTheFieldItNeeds() {
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_EMAIL.getLabel()), () ->
                    parse("John -e"));
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_EMAIL.getLabel("--email")), () ->
                    parse("John --email"));
    }

    @Test
    public void tokenize_knownFlagInTheValuePosition_saysTheFirstNeedsAValue() {
        // -t is empty here; storing "-e" as a tag would be the wrong reading of a plain mistake
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_TAG.getLabel()), () ->
                    parse("John -p 123 -t -e"));

        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_PHONE.getLabel()), () ->
                    parse("John -p -e john@example.com"));
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_PHONE.getLabel()), () ->
                    parse("John -p --tag=\"Lab 3\""));
    }

    @Test
    public void tokenize_unknownFlagInTheValuePosition_saysTheFirstNeedsAValue() {
        // an unknown option is still an option, so it does not become the value of the one before
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_TAG.getLabel()), () ->
                    parse("John -p 123 -t -z"));
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_TAG.getLabel()), () ->
                    parse("John -p 123 -t --phon=1"));
    }

    @Test
    public void tokenize_quotedValueOpeningWithAHyphen_isTakenAsTheValue() throws Exception {
        // quoting is how a value that looks like an option is given
        assertEquals(List.of("-urgent"), parse("John -p 123 -t \"-urgent\"").getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_escapedQuote_staysInTheValue() throws Exception {
        // a quote is syntax, so it needs an escape to survive rather than being dropped in silence
        assertEquals("Dwayne \"The Rock\" Johnson", parse("Dwayne \\\"The Rock\\\" Johnson -p 123").getPreamble());
        assertEquals(List.of("say \"hi\""), parse("John -p 123 -t \"say \\\"hi\\\"\"").getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_escapedBackslash_staysInTheValue() throws Exception {
        assertEquals("a\\b", parse("\"a\\\\b\" -p 123").getPreamble());
    }

    @Test
    public void tokenize_backslashBeforeAnythingElse_isLeftAlone() throws Exception {
        // only a quote and a backslash are escapable, so a stray backslash does not disappear
        assertEquals("a\\nb", parse("\"a\\nb\" -p 123").getPreamble());
    }

    @Test
    public void tokenize_valueAfterTheFlagsBegan_saysItBelongsToNoOption() {
        // a value once the options have begun belongs to nothing, whatever was meant by it
        assertThrows(ParseException.class,
                Messages.getErrorMessageForValueAfterFlags("Kumaran", true), () ->
                    parse("Ravi -t T1 Kumaran"));
        assertFalse(Messages.getErrorMessageForValueAfterFlags("Kumaran", true)
                .contains(Messages.MESSAGE_PREAMBLE_BEFORE_OPTIONS));
    }

    @Test
    public void tokenize_valueAfterTheFlagsWithoutAPreamble_offersTheFrontOfTheCommand() {
        // nothing was given before the options, so the front of the command is still free
        assertThrows(ParseException.class,
                Messages.getErrorMessageForValueAfterFlags("Ravi", false), () ->
                    parse("-t T1 Ravi"));
        assertTrue(Messages.getErrorMessageForValueAfterFlags("Ravi", false)
                .contains(Messages.MESSAGE_PREAMBLE_BEFORE_OPTIONS));
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
        assertThrows(ParseException.class, Messages.getErrorMessageForDuplicateFlags("-e"), () ->
                parsed.verifyNoDuplicateFlagsFor(FLAG_PHONE, FLAG_EMAIL));
    }

    @Test
    public void verifyNoDuplicateFlagsFor_mixedAliasesOfOneFlag_throws() throws Exception {
        FlagArgumentMap parsed = parse("John --email a@b.com -e c@d.com");

        assertEquals(Optional.of("c@d.com"), parsed.getValue(FLAG_EMAIL));
        assertThrows(ParseException.class, Messages.getErrorMessageForDuplicateFlags("--email"), () ->
                parsed.verifyNoDuplicateFlagsFor(FLAG_PHONE, FLAG_EMAIL));
    }

    @Test
    public void verifyNoDuplicateFlagsFor_primaryAndAlias_throwsForOneField() throws Exception {
        FlagArgumentMap parsed = parse("John --id A01234567X -i A76543210Y");

        assertThrows(ParseException.class, Messages.getErrorMessageForDuplicateFlags("--id"), () ->
                parsed.verifyNoDuplicateFlagsFor(FLAG_NUS_STUDENT_ID));
    }

    @Test
    public void tokenize_repeatedTagFlag_keepsEveryValue() throws Exception {
        FlagArgumentMap parsed = parse("John -t T1 --tag T2");

        assertEquals(List.of("T1", "T2"), parsed.getAllValues(FLAG_TAG));
    }

    @Test
    public void tokenize_emptyQuotedValue_reachesTheFlag() throws Exception {
        // the field parser decides whether an empty value is allowed, not the tokenizer
        assertEquals(Optional.of(""), parse("John -e \"\"").getValue(FLAG_EMAIL));
    }

    @Test
    public void tokenize_markerOnlyFlagAtMiddleOrEnd_recordsEmptyValue() throws Exception {
        FlagArgumentMap middle = parse("John --follow-up -p 123");
        FlagArgumentMap end = parse("John -p 123 -f");

        assertEquals(Optional.of(""), middle.getValue(FLAG_FOLLOW_UP));
        assertEquals(Optional.of("123"), middle.getValue(FLAG_PHONE));
        assertEquals(Optional.of(""), end.getValue(FLAG_FOLLOW_UP));
    }

    @Test
    public void tokenize_repeatedMarkerOnlyFlag_keepsEveryOccurrence() throws Exception {
        assertEquals(List.of("", ""),
                parse("John --follow-up -f").getAllValues(FLAG_FOLLOW_UP));
    }

    @Test
    public void tokenize_separateValueAfterMarkerOnlyFlag_saysFlagTakesNoValue() {
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_TAKES_NO_VALUE, "--follow-up"), () ->
                    parse("John --follow-up true"));
    }

    @Test
    public void tokenize_attachedValueAfterMarkerOnlyFlag_saysFlagTakesNoValue() {
        for (CliFlag flag : List.of(FLAG_FOLLOW_UP, FLAG_NO_FOLLOW_UP)) {
            for (String marker : flag.getMarkers()) {
                for (String suffix : List.of("=true", "=")) {
                    assertThrows(ParseException.class,
                            String.format(Messages.MESSAGE_FLAG_TAKES_NO_VALUE, marker), () ->
                                FlagTokenizer.tokenize("John " + marker + suffix, flag));
                }
            }
        }
    }

    @Test
    public void tokenize_markerOnlyFlagAfterValueTakingFlag_saysFirstNeedsValue() {
        assertThrows(ParseException.class,
                String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, FLAG_EMAIL.getLabel()), () ->
                    parse("John -e -f"));
    }
    @Test
    public void tokenize_unknownOption_retainsMarkerSeparatelyFromMessage() {
        for (String input : List.of("Ann -n Ada", "Ann --name=Ada", "Ann --name=\"\"")) {
            UnknownFlagException failure = org.junit.jupiter.api.Assertions.assertThrows(
                    UnknownFlagException.class, () -> FlagTokenizer.tokenize(input, FLAG_PHONE));
            String marker = input.contains("--name") ? "--name" : "-n";
            assertEquals(marker, failure.getMarker());
            assertEquals(String.format(Messages.MESSAGE_UNKNOWN_FLAG, marker), failure.getMessage());
        }
    }

}
