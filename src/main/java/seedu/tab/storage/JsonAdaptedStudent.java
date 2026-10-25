package seedu.tab.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import seedu.tab.commons.exceptions.IllegalValueException;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.Messages;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * Jackson-friendly version of {@link Student}.
 */
// Optional and default-valued fields leave their keys out rather than writing null, so that the
// file stays as readable by hand as one written before those fields were introduced.
@JsonInclude(JsonInclude.Include.NON_NULL)
class JsonAdaptedStudent {
    static final String MISSING_FIELD_MESSAGE_FORMAT = "%s field is missing.";

    private final String name;
    private final String phone;
    private final String email;
    private final String nusStudentId;
    private final List<JsonAdaptedTag> tags = new ArrayList<>();
    private final Boolean flag;

    /**
     * Constructs a {@code JsonAdaptedStudent} with the given student details.
     */
    @JsonCreator
    public JsonAdaptedStudent(@JsonProperty("name") String name, @JsonProperty("phone") String phone,
            @JsonProperty("email") String email,
            @JsonProperty("nusStudentId") String nusStudentId,
            @JsonProperty("tags") @JsonDeserialize(using = JsonTagsDeserializer.class) List<JsonAdaptedTag> tags,
            @JsonProperty("flag") @JsonDeserialize(using = JsonFollowUpFlagDeserializer.class) Boolean flag) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.nusStudentId = nusStudentId;
        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.flag = flag;
    }

    /**
     * Constructs a {@code JsonAdaptedStudent} without a NUS Student ID.
     */
    public JsonAdaptedStudent(String name, String phone, String email,
            List<JsonAdaptedTag> tags, Boolean flag) {
        this(name, phone, email, null, tags, flag);
    }

    /**
     * Converts a given {@code Student} into this class for Jackson use.
     */
    public JsonAdaptedStudent(Student source) {
        name = source.getName().fullName;
        phone = source.getPhone().value;
        email = source.getEmail().map(sourceEmail -> sourceEmail.value).orElse(null);
        nusStudentId = source.getNusStudentId().map(sourceNusStudentId -> sourceNusStudentId.value).orElse(null);
        tags.addAll(source.getTags().stream()
                .map(JsonAdaptedTag::new)
                .collect(Collectors.toList()));
        flag = source.getFlag().isPresent() ? Boolean.TRUE : null;
    }

    /** Rejects unrecognized keys before their values can be lost on the next save. */
    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        // TAB data files from before the address field was removed still contain it and must still load.
        if (field.equals("address")) {
            return;
        }
        String record = name == null ? "student record" : "student \"" + Messages.shorten(name) + "\"";
        throw new IllegalArgumentException("Unknown field \"" + Messages.shorten(field) + "\" in " + record
                + ". Check its spelling. Expected: name, phone, email, nusStudentId, tags, flag."
                + " The legacy address field is also accepted and ignored.");
    }

    /**
     * Converts this Jackson-friendly adapted student object into the model's {@code Student} object.
     *
     * @throws IllegalValueException if there were any data constraints violated in the adapted student.
     */
    public Student toModelType() throws IllegalValueException {
        final List<Tag> studentTags = new ArrayList<>();
        for (JsonAdaptedTag tag : tags) {
            if (tag == null) {
                throw new IllegalValueException("Tag field is missing.");
            }
            studentTags.add(tag.toModelType());
        }

        if (name == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.FIELD_NAME));
        }
        if (!Name.isValidName(name)) {
            throw new IllegalValueException(Messages.getErrorMessageForInvalidValue(
                    Name.FIELD_NAME, name, Name.getFailureReason(name)));
        }
        final Name modelName = new Name(name);

        if (phone == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.FIELD_NAME));
        }
        if (!Phone.isValidPhone(phone)) {
            throw new IllegalValueException(Messages.getErrorMessageForInvalidValue(
                    Phone.FIELD_NAME, phone, Phone.getFailureReason(phone)));
        }
        final Phone modelPhone = new Phone(phone);

        // A hand-edited file may spell an absent email as a missing key, null or a blank string.
        // A non-blank value follows the same trimming behavior as command input before validation.
        final String normalizedEmail = normalizeOptionalEmail(email);
        if (normalizedEmail != null && !Email.isWithinLengthLimit(normalizedEmail)) {
            throw new IllegalValueException(String.format(Locale.ROOT,
                    "%s field is too long to read safely: it holds %d characters, but saved emails may hold at most %d",
                    Email.FIELD_NAME, normalizedEmail.length(), Email.MAX_LENGTH));
        }
        if (normalizedEmail != null && !Email.isValidEmail(normalizedEmail)) {
            throw new IllegalValueException(Messages.getErrorMessageForInvalidValue(
                    Email.FIELD_NAME, normalizedEmail, Email.getFailureReason(normalizedEmail)));
        }
        final Optional<Email> modelEmail = Optional.ofNullable(normalizedEmail).map(Email::new);

        // A missing or null key represents an ID that has not been recorded. A malformed
        // value is rejected rather than being silently discarded from the student record.
        final String normalizedNusStudentId =
                nusStudentId == null ? null : StringUtil.normalizeFieldValue(nusStudentId);
        if (normalizedNusStudentId != null && !NusStudentId.isValidNusStudentId(normalizedNusStudentId)) {
            throw new IllegalValueException(NusStudentId.MESSAGE_CONSTRAINTS);
        }
        final Optional<NusStudentId> modelNusStudentId = Optional.ofNullable(normalizedNusStudentId)
                .map(NusStudentId::new);

        final Set<Tag> modelTags = new HashSet<>(studentTags);
        return new Student(modelName, modelPhone, modelEmail, modelNusStudentId, modelTags, Boolean.TRUE.equals(flag));
    }

    private static String normalizeOptionalEmail(String rawEmail) {
        if (rawEmail == null) {
            return null;
        }
        String normalizedEmail = StringUtil.normalizeFieldValue(rawEmail);
        return normalizedEmail.isEmpty() ? null : normalizedEmail;
    }

    String getNameForErrorMessage() {
        return name == null ? null : Messages.shorten(name);
    }
}
