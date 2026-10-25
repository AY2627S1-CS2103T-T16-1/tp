package seedu.tab.model.student;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import seedu.tab.commons.util.StringUtil;

/**
 * Represents a Student's email in the student book.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    /** How this field is named when a value of it is reported as invalid. */
    public static final String FIELD_NAME = "Email";

    /**
     * The longest address a command may enter, which is the limit RFC 5321 section 4.5.3.1 puts
     * on a forward path. It is far above any address a teaching assistant records.
     *
     * <p>The limit exists because {@link #VALIDATION_REGEX} nests one quantifier inside another
     * and Java matches a repeated group by recursion, so a long enough address exhausts the
     * stack. It is deliberately not applied by {@link #isValidEmail(String)} so callers can
     * report a length failure separately from a format failure.
     */
    public static final int MAX_LENGTH = 254;

    private static final String SPECIAL_CHARACTERS = "!#$%&'*+/=?^_`{|}~-";

    public static final String MESSAGE_CONSTRAINTS = "Emails should be of the format local-part@domain "
            + "and adhere to the following constraints:\n"
            + "1. The local-part should only contain alphanumeric characters and these special characters, excluding "
            + "the parentheses, (" + SPECIAL_CHARACTERS + "), and periods. Periods may not appear at the start "
            + "or end, or consecutively.\n"
            + "2. The local-part is followed by an '@' and then a domain name. The domain name is made up of domain "
            + "labels separated by periods.\n"
            + "The domain name must:\n"
            + "    - end with a domain label at least 2 characters long\n"
            + "    - have each domain label start and end with alphanumeric characters\n"
            + "    - have each domain label consist of alphanumeric characters, separated only by hyphens, if any.";
    // alphanumeric and special characters
    private static final String ALPHANUMERIC_NO_UNDERSCORE = "[^\\W_]+"; // alphanumeric characters except underscore
    // Implements the dot-atom syntax in RFC 5322 section 3.2.3:
    // https://www.rfc-editor.org/rfc/rfc5322.html#section-3.2.3
    private static final String LOCAL_ATOM_REGEX = "[a-zA-Z0-9" + SPECIAL_CHARACTERS + "]+";
    private static final String LOCAL_PART_REGEX = "^" + LOCAL_ATOM_REGEX + "(\\." + LOCAL_ATOM_REGEX + ")*";
    private static final String DOMAIN_PART_REGEX = ALPHANUMERIC_NO_UNDERSCORE
            + "(-" + ALPHANUMERIC_NO_UNDERSCORE + ")*";
    private static final int MINIMUM_LAST_LABEL_LENGTH = 2;
    private static final String DOMAIN_LAST_PART_REGEX =
            "(" + DOMAIN_PART_REGEX + "){" + MINIMUM_LAST_LABEL_LENGTH + ",}$";
    private static final String DOMAIN_REGEX = "(" + DOMAIN_PART_REGEX + "\\.)*" + DOMAIN_LAST_PART_REGEX;
    public static final String VALIDATION_REGEX = LOCAL_PART_REGEX + "@" + DOMAIN_REGEX;

    public final String value;

    /**
     * Constructs an {@code Email}, storing the form described by
     * {@link StringUtil#normalizeFieldValue(String)}.
     *
     * @param email A valid email address.
     */
    public Email(String email) {
        requireNonNull(email);
        checkArgument(isValidEmail(email), MESSAGE_CONSTRAINTS);
        value = StringUtil.normalizeFieldValue(email);
    }

    /**
     * Returns true if a given string is a valid email.
     *
     * <p>Callers taking an address from an untrusted source must clear
     * {@link #isWithinLengthLimit(String)} first: this matches {@link #VALIDATION_REGEX} as a
     * whole, which exhausts the stack on a long enough address.
     */
    public static boolean isValidEmail(String test) {
        requireNonNull(test);
        return StringUtil.normalizeFieldValue(test).matches(VALIDATION_REGEX);
    }

    /**
     * Returns true if {@code test} is short enough to be entered as an address, and so short
     * enough for {@link #isValidEmail(String)} to examine safely.
     */
    public static boolean isWithinLengthLimit(String test) {
        requireNonNull(test);
        return StringUtil.normalizeFieldValue(test).length() <= MAX_LENGTH;
    }

    /**
     * Returns why {@code test} does not hold an email address, naming the part of it that is
     * at fault. The answer is only meaningful when {@link #isValidEmail(String)} rejects it.
     */
    public static String getFailureReason(String test) {
        requireNonNull(test);
        String normalizedTest = StringUtil.normalizeFieldValue(test);

        // before anything that matches part of the address, for the reason given on MAX_LENGTH
        if (normalizedTest.length() > MAX_LENGTH) {
            return "an email may hold at most " + MAX_LENGTH + " characters, and this one holds "
                    + normalizedTest.length();
        }

        int at = normalizedTest.indexOf('@');
        if (at < 0) {
            return "an email needs an @ between the local part and the domain";
        }
        if (normalizedTest.indexOf('@', at + 1) >= 0) {
            return "an email may hold only one @";
        }

        String localPart = normalizedTest.substring(0, at);
        String domain = normalizedTest.substring(at + 1);

        if (localPart.isEmpty()) {
            return "there is nothing before the @";
        }
        if (!localPart.matches(LOCAL_PART_REGEX)) {
            return "the part before the @ may hold letters, digits and " + SPECIAL_CHARACTERS
                    + ", with dots allowed only between non-empty groups";
        }
        if (domain.isEmpty()) {
            return "there is nothing after the @";
        }

        String lastLabel = domain.substring(domain.lastIndexOf('.') + 1);
        if (lastLabel.length() < MINIMUM_LAST_LABEL_LENGTH) {
            return "the last part of the domain needs at least "
                    + MINIMUM_LAST_LABEL_LENGTH + " characters";
        }
        return "the domain may hold letters and digits in labels separated by dots, "
                + "with hyphens allowed inside a label";
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
        if (!(other instanceof Email otherEmail)) {
            return false;
        }

        return value.equals(otherEmail.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
