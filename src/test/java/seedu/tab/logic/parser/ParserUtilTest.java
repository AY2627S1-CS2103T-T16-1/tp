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

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
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

    /** What a calling command hands parseStudentTarget to report a mistyped index through. */
    private static final String USAGE = "delete: deletes a student by index or NUS Student ID.";

    @Test
    public void parseRequiredIndex_missingInput_namesSuppliedField() {
        assertThrows(ParseException.class, "Missing required field(s): STUDENT_INDEX", () ->
                ParserUtil.parseRequiredIndex("", "STUDENT_INDEX"));
    }

    @Test
    public void parseRequiredIndex_invalidInput_throwsParseException() {
        for (String index : List.of(" ", "0", "-1", "abc", "2147483648")) {
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
        for (String separator : List.of(" ", "\t", "\u00a0")) {
            assertThrows(ParseException.class,
                    "Unexpected value after STUDENT_INDEX: \"Alex\". Put field values after an option.", () ->
                    ParserUtil.parseRequiredIndex("1" + separator + "Alex", "STUDENT_INDEX"));
        }
        assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, () ->
                ParserUtil.parseRequiredIndex("0 Alex", "STUDENT_INDEX"));
    }

    @Test
    public void parseIndex_invalidInput_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseIndex("10 a"));
    }

    @Test
    public void parseIndex_outOfRangeInput_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_INVALID_INDEX, ()
            -> ParserUtil.parseIndex(Long.toString(Integer.MAX_VALUE + 1)));
    }

    @Test
    public void parseIndex_validInput_success() throws Exception {
        // No whitespaces
        assertEquals(INDEX_FIRST_STUDENT, ParserUtil.parseIndex("1"));

        // Leading and trailing whitespaces
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
    public void parseStudentTarget_unusableIndexTarget_throwsUsageMessage() {
        // a target of this shape reads as a mistyped index rather than as a candidate
        // NUS Student ID, so the usage message carries the refusal
        for (String target : List.of("0", "-1", "!@#", "1a", "2147483648")) {
            assertThrows(ParseException.class,
                    String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, USAGE), () ->
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
        assertThrows(ParseException.class, () -> ParserUtil.parseName(INVALID_NAME));
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
        assertThrows(ParseException.class, () -> ParserUtil.parsePhone(INVALID_PHONE));
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
        assertThrows(ParseException.class, () -> ParserUtil.parseEmail(INVALID_EMAIL));
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
    public void parseTag_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ParserUtil.parseTag(null));
    }

    @Test
    public void parseTag_invalidValue_throwsParseException() {
        assertThrows(ParseException.class, () -> ParserUtil.parseTag(INVALID_TAG));
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
}
