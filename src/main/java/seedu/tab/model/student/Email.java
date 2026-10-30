package seedu.tab.model.student;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import java.util.Locale;
import java.util.regex.Pattern;

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
     * <p>Structural validation is independent of this entry limit so callers can report length
     * failures separately and existing model values retain their format contract. Validation
     * examines atoms and domain labels iteratively, including for direct model construction.
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

    private static final String MESSAGE_TOO_LONG =
            "an email may hold at most %d characters, and this one holds %d";
    private static final String MESSAGE_MISSING_SEPARATOR = "an email needs an @ between the local part and the domain";
    private static final String MESSAGE_MULTIPLE_SEPARATORS = "an email may hold only one @";
    private static final String MESSAGE_EMPTY_LOCAL_PART = "there is nothing before the @";
    private static final String MESSAGE_INVALID_LOCAL_PART =
            "the part before the @ may hold ASCII letters, digits and " + SPECIAL_CHARACTERS
                    + ", with dots allowed only between non-empty groups";
    private static final String MESSAGE_EMPTY_DOMAIN = "there is nothing after the @";
    private static final String MESSAGE_EMPTY_DOMAIN_LABEL =
            "the domain may not begin or end with a dot, or hold two dots in a row";
    private static final String MESSAGE_SHORT_LAST_LABEL = "the last part of the domain needs at least %d characters";
    private static final String MESSAGE_INVALID_DOMAIN =
            "the domain may hold ASCII letters and digits in labels separated by dots, "
                    + "with hyphens allowed inside a label";

    private static final Pattern LOCAL_ATOM_PATTERN = Pattern.compile(LOCAL_ATOM_REGEX);

    private final String value;

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
     * <p>This checks format independently of the command-entry length limit and avoids the
     * recursive matching of the equivalent {@link #VALIDATION_REGEX}.
     */
    public static boolean isValidEmail(String test) {
        requireNonNull(test);
        String normalized = StringUtil.normalizeFieldValue(test);
        int separator = normalized.indexOf('@');
        if (separator <= 0 || separator != normalized.lastIndexOf('@')) {
            return false;
        }
        return isValidLocalPart(normalized.substring(0, separator))
                && isValidDomain(normalized.substring(separator + 1));
    }

    private static boolean isValidLocalPart(String localPart) {
        for (String atom : localPart.split("\\.", -1)) {
            if (!LOCAL_ATOM_PATTERN.matcher(atom).matches()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidDomain(String domain) {
        String[] labels = domain.split("\\.", -1);
        for (String label : labels) {
            if (!isValidDomainLabel(label)) {
                return false;
            }
        }
        return hasAdjacentAlphanumericCharacters(labels[labels.length - 1]);
    }

    private static boolean isValidDomainLabel(String label) {
        if (label.isEmpty() || label.charAt(0) == '-' || label.charAt(label.length() - 1) == '-') {
            return false;
        }
        boolean wasHyphen = false;
        for (char character : label.toCharArray()) {
            boolean isHyphen = character == '-';
            if ((!isHyphen && !isAsciiAlphanumeric(character)) || (isHyphen && wasHyphen)) {
                return false;
            }
            wasHyphen = isHyphen;
        }
        return true;
    }

    private static boolean isAsciiAlphanumeric(char character) {
        return character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9';
    }

    /**
     * Preserves the final-label rule in {@link #VALIDATION_REGEX}: the last label has to hold
     * {@code MINIMUM_LAST_LABEL_LENGTH} alphanumeric groups, which is two. A validated label
     * can split into two such groups only at adjacent alphanumeric characters, so that is what
     * this looks for.
     */
    private static boolean hasAdjacentAlphanumericCharacters(String label) {
        for (int index = 1; index < label.length(); index++) {
            if (label.charAt(index - 1) != '-' && label.charAt(index) != '-') {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code test} is short enough to be entered as an address. Format is checked
     * separately, so a stored address already past this limit keeps its format contract.
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
        if (normalizedTest.length() > MAX_LENGTH) {
            return String.format(Locale.ROOT, MESSAGE_TOO_LONG, MAX_LENGTH, normalizedTest.length());
        }
        int separator = normalizedTest.indexOf('@');
        if (separator < 0) {
            return MESSAGE_MISSING_SEPARATOR;
        }
        if (separator != normalizedTest.lastIndexOf('@')) {
            return MESSAGE_MULTIPLE_SEPARATORS;
        }
        String localFailure = getLocalPartFailure(normalizedTest.substring(0, separator));
        return localFailure == null ? getDomainFailure(normalizedTest.substring(separator + 1)) : localFailure;
    }

    /** Returns why {@code localPart} is no local part, or null when it is one. */
    private static String getLocalPartFailure(String localPart) {
        if (localPart.isEmpty()) {
            return MESSAGE_EMPTY_LOCAL_PART;
        }
        return isValidLocalPart(localPart) ? null : MESSAGE_INVALID_LOCAL_PART;
    }

    /**
     * Returns why {@code domain} is no domain. It is reached only once the local part has passed,
     * so some reason always applies and the answer is never null.
     */
    private static String getDomainFailure(String domain) {
        if (domain.isEmpty()) {
            return MESSAGE_EMPTY_DOMAIN;
        }
        if (hasEmptyDomainLabel(domain)) {
            return MESSAGE_EMPTY_DOMAIN_LABEL;
        }
        String lastLabel = domain.substring(domain.lastIndexOf('.') + 1);
        if (lastLabel.length() < MINIMUM_LAST_LABEL_LENGTH) {
            return String.format(Locale.ROOT, MESSAGE_SHORT_LAST_LABEL, MINIMUM_LAST_LABEL_LENGTH);
        }
        return MESSAGE_INVALID_DOMAIN;
    }

    /**
     * Returns true if a dot in {@code domain} has no label on one of its sides. The trailing-dot
     * case has to be named before the last label is measured: a reader of {@code eve@example.com.}
     * sees {@code com} as the last part, so being told it is too short sends them hunting for a
     * fault that is not there.
     */
    private static boolean hasEmptyDomainLabel(String domain) {
        return domain.startsWith(".") || domain.endsWith(".") || domain.contains("..");
    }

    /**
     * Returns true if the domain is a single label, such as {@code john@gmail}. Such an address
     * is valid, because a host name needs no dot, but a student address always has one, so this
     * reports an address worth a second look rather than one to refuse.
     */
    public boolean hasDotlessDomain() {
        return value.indexOf('.', value.indexOf('@')) < 0;
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
