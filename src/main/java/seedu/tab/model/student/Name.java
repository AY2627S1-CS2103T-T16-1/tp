package seedu.tab.model.student;

import static java.util.Objects.requireNonNull;
import static seedu.tab.commons.util.AppUtil.checkArgument;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Represents a Student's name in the student book.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final String MESSAGE_CONSTRAINTS =
            "Names should contain at least one letter, character or number, from any writing system";

    /**
     * Zero-width characters that carry no meaning of their own. They are removed rather than
     * kept, because two names that look identical must not be stored as different students.
     * The zero-width joiner and non-joiner are deliberately absent: scripts such as Sinhala
     * and Arabic need them to shape correctly.
     */
    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B\\uFEFF]");

    /** Any Unicode whitespace, so that a stored name holds only the ASCII space. */
    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");

    private static final Pattern HAS_LETTER_OR_NUMBER = Pattern.compile("[\\p{L}\\p{N}]");

    public final String fullName;

    /**
     * Constructs a {@code Name}, storing it in the form described by {@link #normalize(String)}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = normalize(name);
    }

    /**
     * Returns true if a given string holds a name. A name is stored and displayed, but it is
     * also split into words by the search and compared as the identity of a student, so the
     * only requirement is that something remains once it is normalized: one character
     * that Unicode calls a letter or a number, which covers logographic scripts such as
     * Chinese as well as alphabets.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        return HAS_LETTER_OR_NUMBER.matcher(normalize(test)).find();
    }

    /**
     * Returns {@code raw} in the form the student book stores, searches and compares: composed,
     * without zero-width characters, and with every run of whitespace reduced to one ASCII
     * space and the ends trimmed.
     */
    private static String normalize(String raw) {
        String composed = Normalizer.normalize(raw, Normalizer.Form.NFC);
        String visible = ZERO_WIDTH.matcher(composed).replaceAll("");
        return WHITESPACE.matcher(visible).replaceAll(" ").trim();
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
