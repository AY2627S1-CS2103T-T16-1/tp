package seedu.tab.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.storage.JsonAdaptedStudent.MISSING_FIELD_MESSAGE_FORMAT;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalStudents.BENSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.exceptions.IllegalValueException;
import seedu.tab.logic.Messages;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.tag.Flag;

public class JsonAdaptedStudentTest {
    private static final String INVALID_NAME = "---"; // no letter or number
    private static final String INVALID_PHONE = "12"; // fewer than 3 digits
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_NUS_STUDENT_ID = "A0123-456X";
    private static final String INVALID_TAG = "---"; // no letter or number

    private static final String VALID_NAME = BENSON.getName().toString();
    private static final String VALID_PHONE = BENSON.getPhone().toString();
    private static final String VALID_EMAIL = BENSON.getEmail().orElseThrow().toString();
    private static final String VALID_NUS_STUDENT_ID = "A01234567X";
    private static final List<JsonAdaptedTag> VALID_TAGS = BENSON.getTags().stream()
            .map(JsonAdaptedTag::new)
            .collect(Collectors.toList());

    @Test
    public void toModelType_validStudentDetails_returnsStudent() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(BENSON);
        assertEquals(BENSON, student.toModelType());
    }

    @Test
    public void toModelType_validEmail_buildsStudentHoldingIt() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_TAGS, false);
        assertEquals(Optional.of(new Email(VALID_EMAIL)), student.toModelType().getEmail());
    }

    @Test
    public void toModelType_handEditedEmailSymbols_preservesAddress() throws Exception {
        for (String address : List.of("mei__tan@hotmail.com", "sean.o'brien@example.com", "mei_@example.com")) {
            JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, address, VALID_TAGS, false);
            assertEquals(address, student.toModelType().getEmail().orElseThrow().value);
        }
    }

    @Test
    public void toModelType_consecutiveEmailDots_throwsIllegalValueException() {
        JsonAdaptedStudent student = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, "mei..tan@example.com", VALID_TAGS, false);
        String expectedMessage = "Email \"mei..tan@example.com\" is not valid: "
                + "the part before the @ may hold letters, digits and !#$%&'*+/=?^_`{|}~-"
                + ", with dots allowed only between non-empty groups";
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_validEmailWithSurroundingWhitespace_buildsStudentHoldingTrimmedEmail() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, "  " + VALID_EMAIL + "\t", VALID_TAGS, false);

        assertEquals(Optional.of(new Email(VALID_EMAIL)), student.toModelType().getEmail());
    }

    @Test
    public void toModelType_validEmailWithInvisibleMark_buildsStudentHoldingNormalizedEmail() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, "\u200E" + VALID_EMAIL, VALID_TAGS, false);

        assertEquals(Optional.of(new Email(VALID_EMAIL)), student.toModelType().getEmail());
    }

    @Test
    public void toModelType_invalidName_throwsIllegalValueException() {
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(INVALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_TAGS, false);
        String expectedMessage = "Name \"---\" is not valid: it holds no letter or number";
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_longInvalidName_shortensValueInError() {
        String invalidName = "-".repeat(61);
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(invalidName, VALID_PHONE, VALID_EMAIL, VALID_TAGS, false);
        String expectedMessage = "Name \"" + "-".repeat(Messages.MAX_QUOTED_VALUE_LENGTH)
                + "…\" is not valid: it holds no letter or number";

        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_nullName_throwsIllegalValueException() {
        JsonAdaptedStudent student = new JsonAdaptedStudent(null, VALID_PHONE, VALID_EMAIL, VALID_TAGS, false);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_invalidPhone_throwsIllegalValueException() {
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, INVALID_PHONE, VALID_EMAIL, VALID_TAGS, false);
        String expectedMessage = "Phone \"12\" is not valid: a phone number needs at least 3 digits";
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_nullPhone_throwsIllegalValueException() {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, null, VALID_EMAIL, VALID_TAGS, false);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_invalidEmail_throwsIllegalValueException() {
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, INVALID_EMAIL, VALID_TAGS, false);
        String expectedMessage = "Email \"example.com\" is not valid: "
                + "an email needs an @ between the local part and the domain";
        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_paddedInvalidEmail_throwsIllegalValueExceptionNamingTrimmedValue() {
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, "  " + INVALID_EMAIL + "  ", VALID_TAGS, false);
        String expectedMessage = "Email \"example.com\" is not valid: "
                + "an email needs an @ between the local part and the domain";

        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_worstCaseEmailAtStorageLimit_stillLoads() {
        String atLimit = repeatedLocalPartEmail(Email.MAX_LENGTH);
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, atLimit, VALID_TAGS, false);

        assertDoesNotThrow(student::toModelType);
    }

    @Test
    public void toModelType_paddedEmailAtStorageLimit_buildsStudentHoldingTrimmedEmail() throws Exception {
        String atLimit = repeatedLocalPartEmail(Email.MAX_LENGTH);
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, "  " + atLimit + "  ", VALID_TAGS, false);

        assertEquals(Optional.of(new Email(atLimit)), student.toModelType().getEmail());
    }

    @Test
    public void toModelType_emailOverStorageLimit_throwsIllegalValueExceptionWithoutOverflowing() {
        String overLimit = repeatedLocalPartEmail(Email.MAX_LENGTH + 1);
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, overLimit, VALID_TAGS, false);
        String expectedMessage = String.format(
                "Email field is too long to read safely: it holds %d characters, but saved emails may hold at most %d",
                overLimit.length(), Email.MAX_LENGTH);

        assertThrows(IllegalValueException.class, expectedMessage, student::toModelType);
    }

    @Test
    public void toModelType_extremelyLongEmail_throwsIllegalValueExceptionWithoutOverflowing() {
        String overflowing = "a.".repeat(5000) + "a@example.com";
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, overflowing, VALID_TAGS, false);

        assertThrows(IllegalValueException.class, student::toModelType);
    }

    @Test
    public void toModelType_nullEmail_buildsStudentWithoutOne() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, null, VALID_TAGS, false);
        assertEquals(Optional.empty(), student.toModelType().getEmail());
    }

    @Test
    public void toModelType_emptyEmail_buildsStudentWithoutOne() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, "", VALID_TAGS, false);
        assertEquals(Optional.empty(), student.toModelType().getEmail());
    }

    @Test
    public void toModelType_whitespaceOnlyEmail_buildsStudentWithoutOne() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, "  \t", VALID_TAGS, false);
        assertEquals(Optional.empty(), student.toModelType().getEmail());
    }

    @Test
    public void toModelType_validNusStudentId_buildsStudentHoldingIt() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_NUS_STUDENT_ID, VALID_TAGS, false);
        assertEquals(Optional.of(new NusStudentId(VALID_NUS_STUDENT_ID)), student.toModelType().getNusStudentId());
    }

    @Test
    public void toModelType_nusStudentIdWithWhitespaceAndInvisibleMark_buildsStudentHoldingNormalizedId()
            throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, VALID_EMAIL, " \u200E" + VALID_NUS_STUDENT_ID.toLowerCase() + " ",
                VALID_TAGS, false);

        assertEquals(Optional.of(new NusStudentId(VALID_NUS_STUDENT_ID)),
                student.toModelType().getNusStudentId());
    }

    @Test
    public void toModelType_nullNusStudentId_buildsStudentWithoutOne() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, VALID_EMAIL, null, VALID_TAGS, false);
        assertEquals(Optional.empty(), student.toModelType().getNusStudentId());
    }

    @Test
    public void toModelType_invalidNusStudentId_throwsIllegalValueExceptionNamingField() {
        JsonAdaptedStudent student = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, VALID_EMAIL, INVALID_NUS_STUDENT_ID, VALID_TAGS, false);
        assertThrows(IllegalValueException.class, NusStudentId.MESSAGE_CONSTRAINTS, student::toModelType);
    }

    @Test
    public void toModelType_tooLongNusStudentId_throwsIllegalValueExceptionNamingField() {
        String tooLong = "A".repeat(NusStudentId.MAX_LENGTH + 1);
        JsonAdaptedStudent student = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, VALID_EMAIL, tooLong, VALID_TAGS, false);

        assertThrows(IllegalValueException.class, NusStudentId.MESSAGE_CONSTRAINTS, student::toModelType);
    }

    @Test
    public void toModelType_invalidTags_throwsIllegalValueException() {
        List<JsonAdaptedTag> invalidTags = new ArrayList<>(VALID_TAGS);
        invalidTags.add(new JsonAdaptedTag(INVALID_TAG));
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, VALID_EMAIL, invalidTags, false);
        assertThrows(IllegalValueException.class,
                "Tag \"---\" is not valid: it holds no letter or number", student::toModelType);
    }

    @Test
    public void toModelType_nullTag_throwsIllegalValueException() {
        List<JsonAdaptedTag> invalidTags = new ArrayList<>(VALID_TAGS);
        invalidTags.add(null);
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, VALID_EMAIL, invalidTags, false);

        assertThrows(IllegalValueException.class, "Tag field is missing.", student::toModelType);
    }

    @Test
    public void toModelType_nullTagName_throwsIllegalValueException() {
        List<JsonAdaptedTag> invalidTags = new ArrayList<>(VALID_TAGS);
        invalidTags.add(new JsonAdaptedTag((String) null));
        JsonAdaptedStudent student =
                new JsonAdaptedStudent(VALID_NAME, VALID_PHONE, VALID_EMAIL, invalidTags, false);

        assertThrows(IllegalValueException.class, "Tag field is missing.", student::toModelType);
    }

    @Test
    public void toModelType_trueFlag_buildsFlaggedStudent() throws Exception {
        JsonAdaptedStudent student = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_TAGS, true);
        assertTrue(student.toModelType().isFlagged());
        assertEquals(new Flag(), student.toModelType().getFlag().orElseThrow());
    }

    @Test
    public void toModelType_falseOrNullFlag_buildsUnflaggedStudent() throws Exception {
        JsonAdaptedStudent falseFlag = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_TAGS, false);
        JsonAdaptedStudent nullFlag = new JsonAdaptedStudent(
                VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_TAGS, null);
        assertFalse(falseFlag.toModelType().isFlagged());
        assertFalse(nullFlag.toModelType().isFlagged());
    }

    private static String repeatedLocalPartEmail(int length) {
        String domain = "@example.com";
        int localPartLength = length - domain.length();
        StringBuilder localPart = new StringBuilder(localPartLength);
        while (localPart.length() + 3 <= localPartLength) {
            localPart.append("a.");
        }
        localPart.append("a".repeat(localPartLength - localPart.length()));
        return localPart + domain;
    }

}
