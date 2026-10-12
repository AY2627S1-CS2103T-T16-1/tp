package seedu.tab.model.student;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import java.util.Locale;

/**
 * Represents a Student's NUS Student ID in the student book.
 * Guarantees: immutable; is valid as declared in {@link #isValidNusStudentId(String)}.
 */
public class NusStudentId {

    /** How this field is named when a value of it is reported as invalid. */
    public static final String FIELD_NAME = "NUS Student ID";

    public static final String MESSAGE_CONSTRAINTS =
            "NUS Student IDs should begin with 'A', be followed by exactly 8 digits, "
                    + "and end with a letter from A to Z";

    public static final String VALIDATION_REGEX = "[aA][0-9]{8}[a-zA-Z]";

    public final String value;

    /**
     * Constructs a {@code NusStudentId}.
     *
     * @param nusStudentId A valid NUS Student ID.
     */
    public NusStudentId(String nusStudentId) {
        requireNonNull(nusStudentId);
        checkArgument(isValidNusStudentId(nusStudentId), MESSAGE_CONSTRAINTS);
        value = nusStudentId.toUpperCase(Locale.ROOT);
    }

    /**
     * Returns true if a given string is a valid NUS Student ID.
     */
    public static boolean isValidNusStudentId(String test) {
        requireNonNull(test);
        return test.matches(VALIDATION_REGEX);
    }

    /**
     * Returns why {@code test} does not hold a NUS Student ID. The answer is only meaningful
     * when {@link #isValidNusStudentId(String)} rejects it.
     */
    public static String getFailureReason(String test) {
        requireNonNull(test);

        if (test.isEmpty()) {
            return "a NUS Student ID cannot be empty";
        }
        if (test.charAt(0) != 'a' && test.charAt(0) != 'A') {
            return "a NUS Student ID needs to start with A";
        }

        if (test.length() == 1) {
            return "a NUS Student ID needs 8 digits and a final letter after A";
        }

        char finalCharacter = test.charAt(test.length() - 1);
        boolean isLetter = finalCharacter >= 'a' && finalCharacter <= 'z'
                || finalCharacter >= 'A' && finalCharacter <= 'Z';
        if (!isLetter) {
            return "a NUS Student ID needs to end with a letter from A to Z";
        }

        String digits = test.substring(1, test.length() - 1);
        if (!digits.chars().allMatch(character -> character >= '0' && character <= '9')) {
            return "the characters between the first A and final letter must all be digits";
        }
        return "a NUS Student ID needs exactly 8 digits between the first A and final letter";
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof NusStudentId otherNusStudentId)) {
            return false;
        }

        return value.equals(otherNusStudentId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
