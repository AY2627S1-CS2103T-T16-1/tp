package seedu.tab.commons.util;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Utility methods related to Collections
 */
public class CollectionUtil {

    /** What {@link #indexOfSame(List, Object)} answers when the list holds no such element. */
    public static final int NOT_FOUND = -1;

    private CollectionUtil() {} // this class only holds helpers

    /**
     * Requires every supplied element to be non-null.
     *
     * @see #requireAllNonNull(Collection)
     */
    public static void requireAllNonNull(Object... items) {
        requireNonNull(items);
        Stream.of(items).forEach(Objects::requireNonNull);
    }

    /**
     * Throws NullPointerException if {@code items} or any element of {@code items} is null.
     */
    public static void requireAllNonNull(Collection<?> items) {
        requireNonNull(items);
        items.forEach(Objects::requireNonNull);
    }

    /**
     * Returns true if {@code items} contain any elements that are non-null.
     */
    public static boolean isAnyNonNull(Object... items) {
        return items != null && Arrays.stream(items).anyMatch(Objects::nonNull);
    }

    /**
     * Returns the position of the element that is {@code target} itself, or {@value #NOT_FOUND}
     * when no element is. Identity rather than equality is what callers tracking one object
     * through a list need: two students may be equal while only one of them is the one on hand.
     */
    public static int indexOfSame(List<?> items, Object target) {
        requireNonNull(items);
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index) == target) {
                return index;
            }
        }
        return NOT_FOUND;
    }
}
