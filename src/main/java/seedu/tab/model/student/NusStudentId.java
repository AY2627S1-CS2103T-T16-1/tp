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

    /** Longest supported ID, keeping the other details on a student's card visible. */
    public static final int MAX_LENGTH = 32;

    public static final String MESSAGE_CONSTRAINTS =
            "NUS Student IDs should be non-empty, contain only ASCII letters and digits, "
                    + "and be at most " + MAX_LENGTH + " characters long";

    public static final String MESSAGE_EMPTY = "a NUS Student ID cannot be empty";

    public static final String MESSAGE_TOO_LONG =
            "a NUS Student ID may hold at most " + MAX_LENGTH + " characters";

    public static final String MESSAGE_NON_ALPHANUMERIC =
            "a NUS Student ID can only contain ASCII letters and digits";

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
        return getFailureReason(test).isEmpty();
    }

    /**
     * Returns why {@code test} does not hold a NUS Student ID, or an empty string when it is valid.
     */
    public static String getFailureReason(String test) {
        requireNonNull(test);

        if (test.isEmpty()) {
            return MESSAGE_EMPTY;
        }
        if (test.length() > MAX_LENGTH) {
            return MESSAGE_TOO_LONG;
        }
        if (!test.chars().allMatch(character -> character < 128 && Character.isLetterOrDigit(character))) {
            return MESSAGE_NON_ALPHANUMERIC;
        }
        return "";
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
