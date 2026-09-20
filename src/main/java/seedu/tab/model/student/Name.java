package seedu.tab.model.student;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import seedu.tab.commons.util.StringUtil;

/**
 * Represents a Student's name in the student book.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final String MESSAGE_CONSTRAINTS =
            "Names should contain at least one letter, character or number, from any writing system";

    public final String fullName;

    /**
     * Constructs a {@code Name}, storing the form described by
     * {@link StringUtil#normalizeWhitespace(String)}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = StringUtil.normalizeWhitespace(name);
    }

    /**
     * Returns true if a given string holds a name. A name is stored and displayed, but it is
     * also split into words by the search and compared as the identity of a student, so the
     * only requirement is that something remains once it is normalized: one character
     * that Unicode calls a letter or a number.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        return StringUtil.hasLetterOrNumber(StringUtil.normalizeWhitespace(test));
    }

    /**
     * Returns why {@code test} does not hold a name. The answer is only meaningful when
     * {@link #isValidName(String)} rejects it.
     */
    public static String getFailureReason(String test) {
        requireNonNull(test);
        return "it holds no letter, character or number";
    }

    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Name)) {
            return false;
        }

        Name otherName = (Name) other;
        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}
