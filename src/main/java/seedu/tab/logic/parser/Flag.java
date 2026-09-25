package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Stream;

/**
 * A flag that marks the field a token belongs to, such as {@code -e} in
 * {@code add John -e john@example.com}.
 */
public class Flag {
    private final String flag;
    private final List<String> aliases;
    private final String fieldName;
    private final boolean takesValue;

    /**
     * Constructs a presence-only flag that takes no value.
     *
     * @param flag The characters that mark the option, such as {@code -f}.
     */
    public Flag(String flag) {
        requireNonNull(flag);
        this.flag = flag;
        this.aliases = List.of();
        this.fieldName = null;
        this.takesValue = false;
    }

    /**
     * Constructs a presence-only flag with alternative markers that take no value.
     *
     * @param flag The primary characters that mark the option.
     * @param aliases Alternative characters that mark the same option.
     */
    public Flag(String flag, List<String> aliases) {
        requireNonNull(flag);
        requireNonNull(aliases);
        this.flag = flag;
        this.aliases = List.copyOf(aliases);
        this.fieldName = null;
        this.takesValue = false;
    }

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
        this.aliases = List.of();
        this.fieldName = fieldName;
        this.takesValue = true;
    }

    public String getFlag() {
        return flag;
    }

    /**
     * Returns every marker that identifies this flag, with the primary marker first.
     */
    public List<String> getMarkers() {
        return Stream.concat(Stream.of(flag), aliases.stream()).toList();
    }

    public boolean takesValue() {
        return takesValue;
    }

    /**
     * Returns the flag together with the name of the field it takes, such as {@code -e EMAIL}.
     * Two flags are still equal when their characters match, whatever they are named.
     */
    public String getLabel() {
        return takesValue ? flag + " " + fieldName : flag;
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
