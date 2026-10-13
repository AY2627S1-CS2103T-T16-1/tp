package seedu.tab.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import seedu.tab.commons.exceptions.IllegalValueException;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
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
    static final int MAX_STORED_EMAIL_LENGTH = 512;
    static final String MISSING_FIELD_MESSAGE_FORMAT = "%s field is missing.";

    private static final int MAX_REPORTED_VALUE_LENGTH = 60;
    private static final String INVALID_FIELD_MESSAGE_FORMAT = "%s \"%s\" is invalid: %s";

    private final String name;
    private final String phone;
    private final String email;
    private final List<JsonAdaptedTag> tags = new ArrayList<>();
    private final Boolean flag;

    /**
     * Constructs a {@code JsonAdaptedStudent} with the given student details.
     */
    @JsonCreator
    public JsonAdaptedStudent(@JsonProperty("name") String name, @JsonProperty("phone") String phone,
            @JsonProperty("email") String email,
            @JsonProperty("tags") List<JsonAdaptedTag> tags,
            @JsonProperty("flag") Boolean flag) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.flag = flag;
    }

    /**
     * Converts a given {@code Student} into this class for Jackson use.
     */
    public JsonAdaptedStudent(Student source) {
        name = source.getName().fullName;
        phone = source.getPhone().value;
        email = source.getEmail().map(sourceEmail -> sourceEmail.value).orElse(null);
        tags.addAll(source.getTags().stream()
                .map(JsonAdaptedTag::new)
                .collect(Collectors.toList()));
        flag = source.getFlag().isPresent() ? Boolean.TRUE : null;
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
            throw new IllegalValueException(formatInvalidField(
                    Name.FIELD_NAME, name, Name.getFailureReason(name)));
        }
        final Name modelName = new Name(name);

        if (phone == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.FIELD_NAME));
        }
        if (!Phone.isValidPhone(phone)) {
            throw new IllegalValueException(formatInvalidField(
                    Phone.FIELD_NAME, phone, Phone.getFailureReason(phone)));
        }
        final Phone modelPhone = new Phone(phone);

        // A file may hold no email, either because the key is absent or because it is null.
        // Anything else has to be a valid address; a broken one is not silently dropped.
        if (email != null && email.length() > MAX_STORED_EMAIL_LENGTH) {
            throw new IllegalValueException(String.format(
                    "%s field is too long to read safely: it holds %d characters, but saved emails may hold at most %d",
                    Email.FIELD_NAME, email.length(), MAX_STORED_EMAIL_LENGTH));
        }
        if (email != null && !Email.isValidEmail(email)) {
            throw new IllegalValueException(formatInvalidField(
                    Email.FIELD_NAME, email, Email.MESSAGE_CONSTRAINTS));
        }
        final Email modelEmail = email == null ? null : new Email(email);

        final Set<Tag> modelTags = new HashSet<>(studentTags);
        return new Student(modelName, modelPhone, modelEmail, modelTags, Boolean.TRUE.equals(flag));
    }

    String getNameForErrorMessage() {
        return name == null ? null : shorten(name);
    }

    static String formatInvalidField(String fieldName, String value, String reason) {
        return String.format(INVALID_FIELD_MESSAGE_FORMAT, fieldName, shorten(value), reason);
    }

    private static String shorten(String value) {
        if (value.length() <= MAX_REPORTED_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_REPORTED_VALUE_LENGTH) + "…";
    }
}
