package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.tab.logic.commands.CommandTestUtil.INVALID_EMAIL_OVER_MAX_LENGTH;
import static seedu.tab.logic.commands.CommandTestUtil.MESSAGE_EMAIL_OVER_MAX_LENGTH;
import static seedu.tab.logic.commands.CommandTestUtil.VALID_EMAIL_AT_MAX_LENGTH;
import static seedu.tab.logic.parser.ParserUtil.MESSAGE_INVALID_INDEX;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.tag.FollowUpFlag;
import seedu.tab.model.tag.Tag;

public class ParserUtilTest {
    private static final String INVALID_NAME = "---"; // no letter or number
    private static final String INVALID_PHONE = "12"; // fewer than 3 digits
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_NUS_STUDENT_ID = "A0123-456X";
    private static final String INVALID_TAG = "---"; // no letter or number

    private static final String VALID_NAME = "Rachel Walker";
    private static final String VALID_PHONE = "123456";
    private static final String VALID_EMAIL = "rachel@example.com";
    private static final String VALID_NUS_STUDENT_ID = "A01234567X";
    private static final String VALID_TAG_1 = "friend";

    private static final String WHITESPACE = " \t\r\n";

    /** What a calling command hands parseStudentTarget to report a missing target through. */
    private static final String USAGE = "delete: deletes a student by index or NUS Student ID.";

    @Test
    public void parseRequiredIndex_missingInput_namesSuppliedField() {
        assertThrows(ParseException.class, "Missing required field(s): STUDENT_INDEX", () ->
                ParserUtil.parseRequiredIndex("", "STUDENT_INDEX"));
    }

    @Test
    public void parseRequiredIndex_invalidInput_throwsParseException() {
        // Rejected forms: whitespace-only, zero at the lower boundary, negative, and non-integer text.
        for (String index : List.of(" ", "0", "-1", "abc")) {
            assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, () ->
                    ParserUtil.parseRequiredIndex(index, "STUDENT_INDEX"));
        }
    }

    @Test
    public void parseRequiredIndex_validInput_returnsIndex() throws Exception {
        assertEquals(INDEX_FIRST_STUDENT, ParserUtil.parseRequiredIndex("1", "STUDENT_INDEX"));
        assertEquals(INDEX_FIRST_STUDENT, ParserUtil.parseRequiredIndex("  1  ", "STUDENT_INDEX"));
    }

    @Test
    public void parseRequiredIndex_extraValue_reportsValueWithoutMisidentifyingIndex() {
        // Valid index followed by an extra token: reject the surplus value for each separator.
        for (String separator : List.of(" ", "\t", "\u00a0")) {
            assertThrows(ParseException.class,
                    "Unexpected value after STUDENT_INDEX: \"Alex\". Put field values after an option.", () ->
                    ParserUtil.parseRequiredIndex("1" + separator + "Alex", "STUDENT_INDEX"));
        }
        // Invalid index plus extra token: report the index failure first.
        assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, () ->
                ParserUtil.parseRequiredIndex("0 Alex", "STUDENT_INDEX"));
    }

    @Test
    public void parseIndex_invalidInput_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, () -> ParserUtil.parseIndex("10 a"));
    }

    @Test
    public void parseIndex_outOfRangeInput_throwsParseException() {
        // the first index that does not fit in an int, one more than the largest one that
        // does. It is written out rather than added up, because an int addition would wrap
        // around and produce a negative index instead
        for (String index : List.of("2147483648", "6591234567", "9".repeat(1_000))) {
            String expected = String.format(ParserUtil.MESSAGE_INDEX_TOO_LARGE, Messages.shorten(index));
            assertThrows(ParseException.class, expected, () -> ParserUtil.parseIndex(index));
            assertThrows(ParseException.class, expected, () -> ParserUtil.parseRequiredIndex(index, "INDEX"));
            assertThrows(ParseException.class, expected, () -> ParserUtil.parseStudentTarget(index, USAGE));
        }
    }

    @Test
    public void parseIndex_largestIndex_isAccepted() throws Exception {
        // Upper accepted boundary of the supported index range.
        assertEquals(Index.fromOneBased(Integer.MAX_VALUE),
                ParserUtil.parseIndex(String.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    public void parseIndex_zeroOrNegativeInput_throwsParseException() {
        for (String index : List.of("0", "-1")) {
            assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, () ->
                    ParserUtil.parseIndex(index));
        }
    }

    @Test
    public void parseIndex_validInput_success() throws Exception {
        // Lower accepted boundary: 1 is the smallest positive index.
        assertEquals(INDEX_FIRST_STUDENT, ParserUtil.parseIndex("1"));

        // Surrounding whitespace is accepted and trimmed, preserving the same index.
        assertEquals(INDEX_FIRST_STUDENT, ParserUtil.parseIndex("  1  "));
    }

    @Test
    public void parseStudentTarget_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseStudentTarget(null, USAGE));
        assertThrows(NullPointerException.class, () -> ParserUtil.parseStudentTarget("1", null));
    }

    @Test
    public void parseStudentTarget_blankTarget_throwsUsageMessage() {
        for (String target : List.of("", " ", WHITESPACE)) {
            assertThrows(ParseException.class,
                    String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, USAGE), () ->
                    ParserUtil.parseStudentTarget(target, USAGE));
        }
    }

    @Test
    public void parseStudentTarget_indexTarget_returnsIndexTarget() throws Exception {
        assertEquals(StudentTarget.byIndex(INDEX_FIRST_STUDENT),
                ParserUtil.parseStudentTarget("1", USAGE));
        assertEquals(StudentTarget.byIndex(INDEX_FIRST_STUDENT),
                ParserUtil.parseStudentTarget("  1  ", USAGE));
    }

    @Test
    public void parseStudentTarget_unusableIndexTarget_reportsPositiveIntegerReason() {
        // a target of this shape reads as a mistyped index rather than as a candidate
        // NUS Student ID, and keeps the concise reason rather than the command usage
        for (String target : List.of("0", "-1", "1.5", "+1", "!@#", "1a")) {
            assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, () ->
                    ParserUtil.parseStudentTarget(target, USAGE));
        }
    }

    @Test
    public void parseStudentTarget_nusStudentIdTarget_returnsIdTarget() throws Exception {
        assertEquals(StudentTarget.byNusStudentId(new NusStudentId(VALID_NUS_STUDENT_ID)),
                ParserUtil.parseStudentTarget(VALID_NUS_STUDENT_ID, USAGE));

        // NusStudentId normalizes its value, so a lowercase ID is the same target
        assertEquals(StudentTarget.byNusStudentId(new NusStudentId(VALID_NUS_STUDENT_ID)),
                ParserUtil.parseStudentTarget("a01234567x", USAGE));
    }

    @Test
    public void parseRequiredStudentTarget_missingInput_namesSuppliedField() {
        assertThrows(ParseException.class, "Missing required field(s): INDEX or NUS_STUDENT_ID", () ->
                ParserUtil.parseRequiredStudentTarget("", "INDEX or NUS_STUDENT_ID", "STUDENT_TARGET"));
        assertThrows(ParseException.class, "Missing required field(s): INDEX or NUS_STUDENT_ID", () ->
                ParserUtil.parseRequiredStudentTarget("   ", "INDEX or NUS_STUDENT_ID", "STUDENT_TARGET"));
    }

    @Test
    public void parseRequiredStudentTarget_invalidIndex_reportsPositiveIntegerReason() {
        for (String target : List.of("0", "-1", "1.5", "+1", "!@#", "1a")) {
            assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, () ->
                    ParserUtil.parseRequiredStudentTarget(target, "STUDENT_TARGET", "STUDENT_TARGET"));
        }
    }

    @Test
    public void parseRequiredStudentTarget_indexOrId_returnsTarget() throws Exception {
        // Digit-leading positive target: interpreted as an index.
        assertEquals(StudentTarget.byIndex(INDEX_FIRST_STUDENT),
                ParserUtil.parseRequiredStudentTarget("1", "STUDENT_TARGET", "STUDENT_TARGET"));
        // Letter-leading alphanumeric target: interpreted as an ID.
        assertEquals(StudentTarget.byNusStudentId(new NusStudentId(VALID_NUS_STUDENT_ID)),
                ParserUtil.parseRequiredStudentTarget(VALID_NUS_STUDENT_ID, "STUDENT_TARGET", "STUDENT_TARGET"));
    }

    @Test
    public void parseRequiredStudentTarget_extraValue_reportsValueAfterTarget() {
        assertThrows(ParseException.class,
                "Unexpected value after STUDENT_TARGET: \"Alex\". Put field values after an option.", () ->
                ParserUtil.parseRequiredStudentTarget("1 Alex", "STUDENT_TARGET", "STUDENT_TARGET"));
    }

    @Test
    public void parseRequiredStudentTarget_extraValueAfterId_namesTheIdForm() {
        // a target opening with a letter is an NUS Student ID rather than an index
        for (String target : List.of(VALID_NUS_STUDENT_ID, "Alex")) {
            assertThrows(ParseException.class,
                    "Unexpected value after NUS_STUDENT_ID: \"Yeoh\". Put field values after an option.", () ->
                    ParserUtil.parseRequiredStudentTarget(target + " Yeoh", "STUDENT_TARGET", "STUDENT_TARGET"));
        }
    }

    @Test
    public void getTargetFieldName_eitherTargetForm_namesTheFormRead() {
        // Letter-leading targets name the ID field, including normalized lowercase input.
        for (String target : List.of("Alex", "A01234567X", "  a01234567x  ")) {
            assertEquals(ParserUtil.FIELD_NUS_STUDENT_ID, ParserUtil.getTargetFieldName(target, "INDEX"));
        }
        // an empty target names no form of its own, and every other target reads as an index
        for (String target : List.of("1", "0", "-1", "1.5", "1ABC", "!@#", "", "   ")) {
            assertEquals("INDEX", ParserUtil.getTargetFieldName(target, "INDEX"));
        }
    }

    @Test
    public void parseStudentTarget_invalidNusStudentId_throwsParseExceptionWithReason() {
        assertThrows(ParseException.class, Messages.getErrorMessageForInvalidValue(
                NusStudentId.FIELD_NAME, INVALID_NUS_STUDENT_ID, NusStudentId.MESSAGE_NON_ALPHANUMERIC), () ->
                ParserUtil.parseStudentTarget(INVALID_NUS_STUDENT_ID, USAGE));
    }

    @Test
    public void parseName_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseName((String) null));
    }

    @Test
    public void parseName_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, Messages.getErrorMessageForInvalidValue(
                Name.FIELD_NAME, INVALID_NAME, Name.getFailureReason(INVALID_NAME)), () ->
                ParserUtil.parseName(INVALID_NAME));
    }

    @Test
    public void parseName_validValueWithoutWhitespace_returnsName() throws Exception {
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(VALID_NAME));
    }

    @Test
    public void parseName_validValueWithWhitespace_returnsTrimmedName() throws Exception {
        String nameWithWhitespace = WHITESPACE + VALID_NAME + WHITESPACE;
        Name expectedName = new Name(VALID_NAME);
        assertEquals(expectedName, ParserUtil.parseName(nameWithWhitespace));
    }

    @Test
    public void parsePhone_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parsePhone((String) null));
    }

    @Test
    public void parsePhone_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, Messages.getErrorMessageForInvalidValue(
                Phone.FIELD_NAME, INVALID_PHONE, Phone.getFailureReason(INVALID_PHONE)), () ->
                ParserUtil.parsePhone(INVALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithoutWhitespace_returnsPhone() throws Exception {
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(VALID_PHONE));
    }

    @Test
    public void parsePhone_validValueWithWhitespace_returnsTrimmedPhone() throws Exception {
        String phoneWithWhitespace = WHITESPACE + VALID_PHONE + WHITESPACE;
        Phone expectedPhone = new Phone(VALID_PHONE);
        assertEquals(expectedPhone, ParserUtil.parsePhone(phoneWithWhitespace));
    }

    @Test
    public void parseEmail_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseEmail((String) null));
    }

    @Test
    public void parseEmail_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, Messages.getErrorMessageForInvalidValue(
                Email.FIELD_NAME, INVALID_EMAIL, Email.getFailureReason(INVALID_EMAIL)), () ->
                ParserUtil.parseEmail(INVALID_EMAIL));
    }

    @Test
    public void parseEmail_validValueWithoutWhitespace_returnsEmail() throws Exception {
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(VALID_EMAIL));
    }

    @Test
    public void parseEmail_validValueWithWhitespace_returnsTrimmedEmail() throws Exception {
        String emailWithWhitespace = WHITESPACE + VALID_EMAIL + WHITESPACE;
        Email expectedEmail = new Email(VALID_EMAIL);
        assertEquals(expectedEmail, ParserUtil.parseEmail(emailWithWhitespace));
    }

    @Test
    public void parseEmail_valueWithInvisibleMark_returnsNormalizedEmail() throws Exception {
        // a pasted address keeps a format character that the message cannot show, so the value
        // is normalized before it is judged rather than quoted back with the mark hidden
        assertEquals(new Email(VALID_EMAIL), ParserUtil.parseEmail("\u200E" + VALID_EMAIL));
    }

    @Test
    public void parseEmail_addressAtTheLengthLimit_returnsEmail() throws Exception {
        assertEquals(new Email(VALID_EMAIL_AT_MAX_LENGTH), ParserUtil.parseEmail(VALID_EMAIL_AT_MAX_LENGTH));
    }

    @Test
    public void parseEmail_addressOneCharacterOverTheLengthLimit_throwsParseException() {
        // the limit is applied here rather than in Email.isValidEmail, so that each input
        // boundary can report a length failure separately from a format failure
        assertThrows(ParseException.class, MESSAGE_EMAIL_OVER_MAX_LENGTH, () ->
                ParserUtil.parseEmail(INVALID_EMAIL_OVER_MAX_LENGTH));
    }

    @Test
    public void parseNusStudentId_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseNusStudentId(null));
    }

    @Test
    public void parseNusStudentId_invalidValue_throwsParseExceptionNamingField() {
        String expected = "NUS Student ID \"A0123-456X\" is not valid: "
                + NusStudentId.MESSAGE_NON_ALPHANUMERIC;
        assertThrows(ParseException.class, expected, () -> ParserUtil.parseNusStudentId(INVALID_NUS_STUDENT_ID));
    }

    @Test
    public void parseNusStudentId_tooLongValue_throwsParseExceptionNamingLimit() {
        String tooLong = "A".repeat(NusStudentId.MAX_LENGTH + 1);
        String expected = "NUS Student ID \"" + tooLong + "\" is not valid: "
                + NusStudentId.MESSAGE_TOO_LONG;

        assertThrows(ParseException.class, expected, () -> ParserUtil.parseNusStudentId(tooLong));
    }

    @Test
    public void parseNusStudentId_validValueWithWhitespace_returnsNormalizedNusStudentId() throws Exception {
        String nusStudentIdWithWhitespace = WHITESPACE + VALID_NUS_STUDENT_ID.toLowerCase() + WHITESPACE;
        assertEquals(new NusStudentId(VALID_NUS_STUDENT_ID), ParserUtil.parseNusStudentId(nusStudentIdWithWhitespace));
    }

    @Test
    public void parseNusStudentId_valueWithInvisibleMark_returnsNormalizedNusStudentId() throws Exception {
        String withMark = "\u200E" + VALID_NUS_STUDENT_ID.toLowerCase();
        assertEquals(new NusStudentId(VALID_NUS_STUDENT_ID), ParserUtil.parseNusStudentId(withMark));
    }

    @Test
    public void parseTag_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseTag(null));
    }

    @Test
    public void parseTag_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, Messages.getErrorMessageForInvalidValue(
                Tag.FIELD_NAME, INVALID_TAG, Tag.getFailureReason(INVALID_TAG)), () ->
                ParserUtil.parseTag(INVALID_TAG));
    }

    @Test
    public void parseTag_validValueWithoutWhitespace_returnsTag() throws Exception {
        Tag expectedTag = new Tag(VALID_TAG_1);
        assertEquals(expectedTag, ParserUtil.parseTag(VALID_TAG_1));
    }

    @Test
    public void parseTag_validValueWithWhitespace_returnsTrimmedTag() throws Exception {
        String tagWithWhitespace = WHITESPACE + VALID_TAG_1 + WHITESPACE;
        Tag expectedTag = new Tag(VALID_TAG_1);
        assertEquals(expectedTag, ParserUtil.parseTag(tagWithWhitespace));
    }

    @Test
    public void parseTag_followUpLabel_returnsTag() throws Exception {
        // removing it is still read as a tag, so that TagCommand can explain the marker
        assertEquals(new Tag(FollowUpFlag.FOLLOW_UP_NAME), ParserUtil.parseTag(FollowUpFlag.FOLLOW_UP_NAME));
    }

    @Test
    public void parseTagToAdd_followUpLabelHoweverSpelt_throwsParseException() {
        List<String> spellings = List.of(FollowUpFlag.FOLLOW_UP_NAME, "needs follow-up", "NEEDS FOLLOW-UP",
                WHITESPACE + "nEEds" + WHITESPACE + "follow-up" + WHITESPACE);
        for (String spelling : spellings) {
            assertThrows(ParseException.class, ParserUtil.MESSAGE_FOLLOW_UP_IS_NOT_A_TAG, () ->
                    ParserUtil.parseTagToAdd(spelling));
        }
    }

    @Test
    public void parseTagToAdd_ordinaryTag_returnsTrimmedTag() throws Exception {
        assertEquals(new Tag(VALID_TAG_1), ParserUtil.parseTagToAdd(WHITESPACE + VALID_TAG_1 + WHITESPACE));
    }

    @Test
    public void parseTagToAdd_tagSpeltAroundTheLabel_returnsTag() throws Exception {
        // the label itself is reserved rather than every tag that reads like it
        // "Needs follow up" (space instead of hyphen) is now caught as a near spelling
        for (String tag : List.of("Needs follow-up soon", "Follow-up")) {
            assertEquals(new Tag(tag), ParserUtil.parseTagToAdd(tag));
        }
        // These near spellings are now rejected as they read like the follow-up marker
        assertThrows(ParseException.class, ParserUtil.MESSAGE_FOLLOW_UP_IS_NOT_A_TAG, () ->
                ParserUtil.parseTagToAdd("Needs follow up"));
    }

    @Test
    public void parseTagToAdd_givenFlagCommand_refusalNamesThatCommand() {
        assertThrows(ParseException.class,
                ParserUtil.getFollowUpIsNotATagMessage("edit 7 --follow-up"), () ->
                        ParserUtil.parseTagToAdd(FollowUpFlag.FOLLOW_UP_NAME, "edit 7 --follow-up"));
        assertEquals(FollowUpFlag.MESSAGE_RESERVED + " Use edit 7 --follow-up to mark the student.",
                ParserUtil.getFollowUpIsNotATagMessage("edit 7 --follow-up"));
    }
}
