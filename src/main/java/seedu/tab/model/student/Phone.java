package seedu.tab.model.student;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import java.util.regex.Pattern;

import seedu.tab.commons.util.StringUtil;

/**
 * Represents a Student's phone number in the student book.
 * Guarantees: immutable; is valid as declared in {@link #isValidPhone(String)}
 */
public class Phone {


    public static final String MESSAGE_CONSTRAINTS = "Phone numbers should contain at least 3 digits";

    private static final Pattern DIGIT = Pattern.compile("\\p{Nd}");
    private static final int MINIMUM_DIGITS = 3;
    public final String value;

    /**
     * Constructs a {@code Phone}.
     *
     * @param phone A valid phone number.
     */
    public Phone(String phone) {
        requireNonNull(phone);
        checkArgument(isValidPhone(phone), MESSAGE_CONSTRAINTS);
        value = StringUtil.normalizeFieldValue(phone);
    }

    /**
     * Returns true if a given string is a valid phone number.
     */
    public static boolean isValidPhone(String test) {
        requireNonNull(test);
        return countDigits(test) >= MINIMUM_DIGITS;
    }

    private static int countDigits(String test) {
        return (int) DIGIT.matcher(test).results().count();
    }

    /**
     * Returns why {@code test} does not hold a phone number. The answer is only meaningful
     * when {@link #isValidPhone(String)} rejects it.
     */
    public static String getFailureReason(String test) {
        requireNonNull(test);
        return "a phone number needs at least " + MINIMUM_DIGITS + " digits";
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
        if (!(other instanceof Phone otherPhone)) {
            return false;
        }

        return value.equals(otherPhone.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
