package seedu.tab.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import seedu.tab.commons.exceptions.IllegalValueException;
import seedu.tab.commons.util.StringUtil;
import seedu.tab.commons.util.ValueMessages;
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

    private static final String MESSAGE_EMAIL_TOO_LONG =
            "%s field is too long: it holds %d characters, but an email may hold at most %d";

    private static final String LEGACY_ADDRESS_FIELD = "address";
    private static final String MESSAGE_UNKNOWN_FIELD = "Unknown field \"%s\" in %s. Check its spelling. "
            + "Expected: name, phone, email, nusStudentId, tags, flag."
            + " The legacy address field is also accepted and ignored.";

    private final String name;
    private final String phone;
    private final String email;
    private final String nusStudentId;
    private final List<JsonAdaptedTag> tags = new ArrayList<>();
    @JsonProperty("flag")
    private final Boolean isFlagged;

    /**
     * Constructs a {@code JsonAdaptedStudent} with the given student details.
     */
    @JsonCreator
    public JsonAdaptedStudent(
            @JsonProperty("name") @JsonDeserialize(using = JsonTextFieldDeserializer.class) String name,
            @JsonProperty("phone") @JsonDeserialize(using = JsonTextFieldDeserializer.class) String phone,
            @JsonProperty("email") @JsonDeserialize(using = JsonTextFieldDeserializer.class) String email,
            @JsonProperty("nusStudentId")
            @JsonDeserialize(using = JsonTextFieldDeserializer.class) String nusStudentId,
            @JsonProperty("tags") @JsonDeserialize(using = JsonTagsDeserializer.class) List<JsonAdaptedTag> tags,
            @JsonProperty("flag") @JsonDeserialize(using = JsonFollowUpFlagDeserializer.class) Boolean isFlagged) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.nusStudentId = nusStudentId;
        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.isFlagged = isFlagged;
    }

    /**
     * Constructs a {@code JsonAdaptedStudent} without a NUS Student ID.
     */
    public JsonAdaptedStudent(String name, String phone, String email,
            List<JsonAdaptedTag> tags, Boolean isFlagged) {
        this(name, phone, email, null, tags, isFlagged);
    }

    /**
     * Converts a given {@code Student} into this class for Jackson use.
     */
    public JsonAdaptedStudent(Student source) {
        name = source.getName().getFullName();
        phone = source.getPhone().map(sourcePhone -> sourcePhone.getValue()).orElse(null);
        email = source.getEmail().map(sourceEmail -> sourceEmail.getValue()).orElse(null);
        nusStudentId = source.getNusStudentId().map(sourceNusStudentId -> sourceNusStudentId.getValue()).orElse(null);
        tags.addAll(source.getTags().stream()
                .map(JsonAdaptedTag::new)
                .collect(Collectors.toList()));
        isFlagged = source.getFlag().isPresent() ? Boolean.TRUE : null;
    }

    /** Rejects unrecognized keys before their values can be lost on the next save. */
    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        // TAB data files from before the address field was removed still contain it and must still load.
        if (field.equals(LEGACY_ADDRESS_FIELD)) {
            return;
        }
        String record = name == null ? "student record" : "student \"" + ValueMessages.shorten(name) + "\"";
        throw new IllegalArgumentException(String.format(MESSAGE_UNKNOWN_FIELD, ValueMessages.shorten(field), record));
    }

    /**
     * Converts this Jackson-friendly adapted student object into the model's {@code Student} object.
     *
     * @throws IllegalValueException if there were any data constraints violated in the adapted student.
     */
    public Student toModelType() throws IllegalValueException {
        Set<Tag> modelTags = convertTags();
        Name modelName = convertName();
        Optional<Phone> modelPhone = convertPhone();
        Optional<Email> modelEmail = convertEmail();
        Optional<NusStudentId> modelNusStudentId = convertNusStudentId();
        return new Student(modelName, modelPhone, modelEmail, modelNusStudentId,
                modelTags, Boolean.TRUE.equals(isFlagged));
    }

    /**
     * Converts tags, retaining the first spelling of case-insensitive duplicates.
     */
    private Set<Tag> convertTags() throws IllegalValueException {
        Set<Tag> modelTags = new TreeSet<>(Comparator.comparing(Tag::getTagName, String.CASE_INSENSITIVE_ORDER));
        for (JsonAdaptedTag tag : tags) {
            if (tag == null) {
                throw new IllegalValueException(JsonAdaptedTag.MESSAGE_MISSING_TAG);
            }
            modelTags.add(tag.toModelType());
        }
        return new HashSet<>(modelTags);
    }

    /**
     * Validates the required saved name before constructing its model value.
     */
    private Name convertName() throws IllegalValueException {
        if (name == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.FIELD_NAME));
        }
        if (!Name.isValidName(name)) {
            throw new IllegalValueException(ValueMessages.getErrorMessageForInvalidValue(
                    Name.FIELD_NAME, name, Name.getFailureReason(name)));
        }
        return new Name(name);
    }

    /**
     * Treats a missing, null or blank optional phone as absent without dropping invalid nonblank data.
     */
    private Optional<Phone> convertPhone() throws IllegalValueException {
        String normalizedPhone = normalizeOptionalValue(phone);
        if (normalizedPhone != null && !Phone.isValidPhone(normalizedPhone)) {
            throw new IllegalValueException(ValueMessages.getErrorMessageForInvalidValue(
                    Phone.FIELD_NAME, normalizedPhone, Phone.getFailureReason(normalizedPhone)));
        }
        return Optional.ofNullable(normalizedPhone).map(Phone::new);
    }

    /**
     * Converts an optional email while preserving storage-specific length and format diagnostics.
     */
    private Optional<Email> convertEmail() throws IllegalValueException {
        String normalizedEmail = normalizeOptionalValue(email);
        if (normalizedEmail != null && !Email.isWithinLengthLimit(normalizedEmail)) {
            throw new IllegalValueException(String.format(Locale.ROOT,
                    MESSAGE_EMAIL_TOO_LONG, Email.FIELD_NAME, normalizedEmail.length(), Email.MAX_LENGTH));
        }
        if (normalizedEmail != null && !Email.isValidEmail(normalizedEmail)) {
            throw new IllegalValueException(ValueMessages.getErrorMessageForInvalidValue(
                    Email.FIELD_NAME, normalizedEmail, Email.getFailureReason(normalizedEmail)));
        }
        return Optional.ofNullable(normalizedEmail).map(Email::new);
    }

    /**
     * Preserves missing IDs as absent while rejecting malformed IDs, including explicit blank values.
     */
    private Optional<NusStudentId> convertNusStudentId() throws IllegalValueException {
        String normalizedId = nusStudentId == null ? null : StringUtil.normalizeFieldValue(nusStudentId);
        if (normalizedId != null && !NusStudentId.isValidNusStudentId(normalizedId)) {
            throw new IllegalValueException(NusStudentId.MESSAGE_CONSTRAINTS);
        }
        return Optional.ofNullable(normalizedId).map(NusStudentId::new);
    }

    /**
     * Returns the saved {@code rawValue} in the form a command would have stored, or null when the
     * file leaves the detail out: a missing key, JSON null, or a string that normalizes to nothing.
     */
    private static String normalizeOptionalValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalizedValue = StringUtil.normalizeFieldValue(rawValue);
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    /**
     * Returns a bounded saved name for record diagnostics, or null if the name is missing.
     */
    String getNameForErrorMessage() {
        return name == null ? null : ValueMessages.shorten(name);
    }
}
