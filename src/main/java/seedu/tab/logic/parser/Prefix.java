package seedu.tab.logic.parser;

/**
 * A prefix that marks the beginning of an argument in an arguments string.
 * E.g. 't/' in 'edit 1 t/friend'.
 */
public class Prefix {
    private final String prefix;
    private final String fieldName;

    public Prefix(String prefix) {
        this(prefix, "");
    }

    /**
     * Constructs a prefix that marks a named field, so that a message can say
     * which field it means rather than only which characters mark it.
     *
     * @param prefix The characters that mark the field, such as {@code e/}.
     * @param fieldName The name of the field, such as {@code EMAIL}.
     */
    public Prefix(String prefix, String fieldName) {
        this.prefix = prefix;
        this.fieldName = fieldName;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the prefix together with the name of the field, such as {@code e/EMAIL}.
     * Two prefixes are still equal when their characters match, whatever they are named.
     */
    public String getLabel() {
        return prefix + fieldName;
    }

    @Override
    public String toString() {
        return getPrefix();
    }

    @Override
    public int hashCode() {
        return prefix == null ? 0 : prefix.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Prefix otherPrefix)) {
            return false;
        }

        return prefix.equals(otherPrefix.prefix);
    }
}
