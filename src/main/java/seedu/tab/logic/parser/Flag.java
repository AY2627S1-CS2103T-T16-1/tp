package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

/**
 * A flag that marks the field a token belongs to, such as {@code -e} in
 * {@code add John -e john@example.com}.
 */
public class Flag {
    private final String flag;
    private final String fieldName;

    /**
     * Constructs a flag that marks a named field, so that a message can say which field it
     * means rather than only which characters mark it.
     *
     * @param flag The characters that mark the field, such as {@code -e}.
     * @param fieldName The name of the field, such as {@code EMAIL}.
     */
    public Flag(String flag, String fieldName) {
        requireNonNull(flag);
        requireNonNull(fieldName);
        this.flag = flag;
        this.fieldName = fieldName;
    }

    public String getFlag() {
        return flag;
    }

    /**
     * Returns the flag together with the name of the field it takes, such as {@code -e EMAIL}.
     * Two flags are still equal when their characters match, whatever they are named.
     */
    public String getLabel() {
        return flag + " " + fieldName;
    }

    @Override
    public String toString() {
        return flag;
    }

    @Override
    public int hashCode() {
        return flag.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Flag otherFlag)) {
            return false;
        }

        return flag.equals(otherFlag.flag);
    }
}
