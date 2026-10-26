package seedu.tab.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Stream;

/**
 * A flag that marks the field a token belongs to, such as {@code -e} in
 * {@code add John -e john@example.com}.
 */
public class CliFlag {
    private final String primaryMarker;
    private final List<String> aliases;
    private final String fieldName;
    private final boolean takesValue;

    /**
     * Constructs a presence-only flag that takes no value.
     *
     * @param primaryMarker The characters that mark the option, such as {@code -f}.
     */
    public CliFlag(String primaryMarker) {
        requireNonNull(primaryMarker);
        this.primaryMarker = primaryMarker;
        this.aliases = List.of();
        this.fieldName = null;
        this.takesValue = false;
    }

    /**
     * Constructs a presence-only flag with alternative markers that take no value.
     *
     * @param primaryMarker The primary characters that mark the option.
     * @param aliases Alternative characters that mark the same option.
     */
    public CliFlag(String primaryMarker, List<String> aliases) {
        requireNonNull(primaryMarker);
        requireNonNull(aliases);
        this.primaryMarker = primaryMarker;
        this.aliases = List.copyOf(aliases);
        this.fieldName = null;
        this.takesValue = false;
    }

    /**
     * Constructs a flag that marks a named field, so that a message can say which field it
     * means rather than only which characters mark it.
     *
     * @param primaryMarker The characters that mark the field, such as {@code -e}.
     * @param fieldName The name of the field, such as {@code EMAIL}.
     */
    public CliFlag(String primaryMarker, String fieldName) {
        this(primaryMarker, List.of(), fieldName);
    }

    /**
     * Constructs a flag with alternative markers that all mark the same named field.
     *
     * @param primaryMarker The primary characters that mark the field.
     * @param aliases Alternative characters that mark the same field.
     * @param fieldName The name of the field, such as {@code EMAIL}.
     */
    public CliFlag(String primaryMarker, List<String> aliases, String fieldName) {
        requireNonNull(primaryMarker);
        requireNonNull(aliases);
        requireNonNull(fieldName);
        this.primaryMarker = primaryMarker;
        this.aliases = List.copyOf(aliases);
        this.fieldName = fieldName;
        this.takesValue = true;
    }

    /**
     * Returns every marker that identifies this flag, with the primary marker first.
     */
    public List<String> getMarkers() {
        return Stream.concat(Stream.of(primaryMarker), aliases.stream()).toList();
    }

    /**
     * Returns every marker separated by slashes for compact use in command usage messages.
     */
    public String getMarkerSummary() {
        return String.join("/", getMarkers());
    }

    /**
     * Returns the primary marker followed by each alias introduced by "or", such as
     * {@code --follow-up (or -f)}. A flag without aliases returns its primary marker alone.
     */
    public String getMarkerWithAliases() {
        if (aliases.isEmpty()) {
            return primaryMarker;
        }
        return primaryMarker + " (or " + String.join(", ", aliases) + ")";
    }

    public boolean takesValue() {
        return takesValue;
    }

    /**
     * Returns the flag together with the name of the field it takes, such as {@code -e EMAIL}.
     * Two flags are still equal when their characters match, whatever they are named.
     */
    public String getLabel() {
        return takesValue ? primaryMarker + " " + fieldName : primaryMarker;
    }

    /**
     * Returns {@code marker} together with this value-taking flag's field name. This lets
     * feedback quote the spelling the User supplied rather than replacing an alias with the
     * primary marker.
     */
    String getLabel(String marker) {
        requireNonNull(marker);
        assert getMarkers().contains(marker);
        return marker + " " + fieldName;
    }

    @Override
    public String toString() {
        return primaryMarker;
    }

    @Override
    public int hashCode() {
        return primaryMarker.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof CliFlag otherFlag)) {
            return false;
        }

        return primaryMarker.equals(otherFlag.primaryMarker);
    }
}
