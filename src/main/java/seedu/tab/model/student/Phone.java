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


    /** How this field is named when a value of it is reported as invalid. */
    public static final String FIELD_NAME = "Phone";

    public static final String MESSAGE_CONSTRAINTS = "Phone numbers should contain at least 3 digits";

    private static final Pattern DIGIT = Pattern.compile("\\p{Nd}");
    private static final Pattern NON_DIGIT = Pattern.compile("\\P{Nd}");
    private static final int MINIMUM_DIGITS = 3;
    private static final int DECIMAL_RADIX = 10;
    /**
     * How many digits the shorter of two numbers needs before being the tail of the longer one
     * counts as the same number. Seven is the shortest complete subscriber number that dialling
     * plans in common use assign, so a shorter tail belongs to an extension or a service code,
     * which numbers reaching different people routinely share.
     */
    private static final int MINIMUM_SHARED_TAIL_DIGITS = 7;
    private final String value;

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
     * Returns the digits this phone number holds, in order, with every other character
     * removed and Unicode decimal digits converted to ASCII. Search and duplicate warnings
     * compare this form while the stored value retains its formatting.
     */
    public String getDigits() {
        return normalizeDigits(value);
    }

    /** Returns the ASCII decimal digits in {@code value}, excluding all non-digit characters. */
    public static String normalizeDigits(String value) {
        requireNonNull(value);
        return NON_DIGIT.matcher(value).replaceAll("").codePoints()
                .map(codePoint -> Character.digit(codePoint, DECIMAL_RADIX) + '0')
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * Returns true if this number and {@code otherPhone} are likely the same number written
     * differently, which a duplicate warning treats as evidence of one person held twice.
     * A country code written in front of a number leaves the digits that identify the subscriber
     * untouched at the end, so the shorter number being the tail of the longer one covers both
     * {@code +65 8743 8807} and {@code 0065 8743 8807} against {@code 8743 8807} without naming
     * any one country. The shorter number has to carry at least seven digits of its own, or a
     * short number would match the great many longer ones that happen to end with it.
     *
     * <p>Digits at the end are a different matter, because they reach a different endpoint. An
     * extension such as {@code 8743 8807 ext 12} therefore does not match {@code 8743 8807}, and
     * neither does a number that merely begins the same way, such as {@code 8743 8807 1}.
     */
    public boolean isLikelySameNumber(Phone otherPhone) {
        requireNonNull(otherPhone);
        String digits = getDigits();
        String otherDigits = otherPhone.getDigits();
        if (digits.equals(otherDigits)) {
            return true;
        }
        boolean isThisShorter = digits.length() < otherDigits.length();
        String shorterDigits = isThisShorter ? digits : otherDigits;
        String longerDigits = isThisShorter ? otherDigits : digits;
        return shorterDigits.length() >= MINIMUM_SHARED_TAIL_DIGITS
                && longerDigits.endsWith(shorterDigits);
    }

    /**
     * Returns why {@code test} does not hold a phone number. The answer is only meaningful
     * when {@link #isValidPhone(String)} rejects it. The reason is constant because
     * every invalid phone number has fewer than the minimum required digits.
     */
    public static String getFailureReason(String test) {
        requireNonNull(test);
        return "a phone number needs at least " + MINIMUM_DIGITS + " digits";
    }

    public String getValue() {
        return value;
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
