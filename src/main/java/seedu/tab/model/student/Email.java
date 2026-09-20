package seedu.tab.model.student;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

/**
 * Represents a Student's email in the student book.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    /** How this field is named when a value of it is reported as invalid. */
    public static final String FIELD_NAME = "Email";

    private static final String SPECIAL_CHARACTERS = "+_.-";

    public static final String MESSAGE_CONSTRAINTS = "Emails should be of the format local-part@domain "
            + "and adhere to the following constraints:\n"
            + "1. The local-part should only contain alphanumeric characters and these special characters, excluding "
            + "the parentheses, (" + SPECIAL_CHARACTERS + "). The local-part may not start or end with any special "
            + "characters.\n"
            + "2. The local-part is followed by an '@' and then a domain name. The domain name is made up of domain "
            + "labels separated by periods.\n"
            + "The domain name must:\n"
            + "    - end with a domain label at least 2 characters long\n"
            + "    - have each domain label start and end with alphanumeric characters\n"
            + "    - have each domain label consist of alphanumeric characters, separated only by hyphens, if any.";
    // alphanumeric and special characters
    private static final String ALPHANUMERIC_NO_UNDERSCORE = "[^\\W_]+"; // alphanumeric characters except underscore
    private static final String LOCAL_PART_REGEX = "^" + ALPHANUMERIC_NO_UNDERSCORE + "([" + SPECIAL_CHARACTERS + "]"
            + ALPHANUMERIC_NO_UNDERSCORE + ")*";
    private static final String DOMAIN_PART_REGEX = ALPHANUMERIC_NO_UNDERSCORE
            + "(-" + ALPHANUMERIC_NO_UNDERSCORE + ")*";
    private static final int MINIMUM_LAST_LABEL_LENGTH = 2;
    private static final String DOMAIN_LAST_PART_REGEX =
            "(" + DOMAIN_PART_REGEX + "){" + MINIMUM_LAST_LABEL_LENGTH + ",}$";
    private static final String DOMAIN_REGEX = "(" + DOMAIN_PART_REGEX + "\\.)*" + DOMAIN_LAST_PART_REGEX;
    public static final String VALIDATION_REGEX = LOCAL_PART_REGEX + "@" + DOMAIN_REGEX;

    public final String value;

    /**
     * Constructs an {@code Email}.
     *
     * @param email A valid email address.
     */
    public Email(String email) {
        requireNonNull(email);
        checkArgument(isValidEmail(email), MESSAGE_CONSTRAINTS);
        value = email;
    }

    /**
     * Returns true if a given string is a valid email.
     */
    public static boolean isValidEmail(String test) {
        return test.matches(VALIDATION_REGEX);
    }

    /**
     * Returns why {@code test} does not hold an email address, naming the part of it that is
     * at fault. The answer is only meaningful when {@link #isValidEmail(String)} rejects it.
     */
    public static String getFailureReason(String test) {
        requireNonNull(test);

        int at = test.indexOf('@');
        if (at < 0) {
            return "an email needs an @ between the local part and the domain";
        }
        if (test.indexOf('@', at + 1) >= 0) {
            return "an email may hold only one @";
        }

        String localPart = test.substring(0, at);
        String domain = test.substring(at + 1);

        if (localPart.isEmpty()) {
            return "there is nothing before the @";
        }
        if (!localPart.matches(LOCAL_PART_REGEX)) {
            return "the part before the @ may hold letters and digits, joined by any of "
                    + SPECIAL_CHARACTERS;
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
