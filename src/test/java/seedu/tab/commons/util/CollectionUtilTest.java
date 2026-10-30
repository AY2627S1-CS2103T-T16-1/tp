package seedu.tab.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.tab.testutil.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CollectionUtilTest {
    @Test
    public void requireAllNonNullVarargs() {
        // empty argument list: accepted at the zero-element boundary
        assertNullPointerExceptionNotThrown();

        // non-empty argument list with no null elements: accepted, including an empty string
        assertNullPointerExceptionNotThrown(new Object(), new Object());
        assertNullPointerExceptionNotThrown("test");
        assertNullPointerExceptionNotThrown("");

        // argument lists with just one null at the beginning
        assertNullPointerExceptionThrown((Object) null);
        assertNullPointerExceptionThrown(null, "", new Object());
        assertNullPointerExceptionThrown(null, new Object(), new Object());

        // argument lists with nulls in the middle
        assertNullPointerExceptionThrown(new Object(), null, null, "test");
        assertNullPointerExceptionThrown("", null, new Object());

        // argument lists with one null as the last argument
        assertNullPointerExceptionThrown("", new Object(), null);
        assertNullPointerExceptionThrown(new Object(), new Object(), null);

        // null reference
        assertNullPointerExceptionThrown((Object[]) null);

        // confirms nulls inside lists in the argument list are not considered
        List<Object> containingNull = Arrays.asList((Object) null);
        assertNullPointerExceptionNotThrown(containingNull, new Object());
    }

    @Test
    public void requireAllNonNullCollection() {
        // lists containing nulls in the front
        assertNullPointerExceptionThrown(Arrays.asList((Object) null));
        assertNullPointerExceptionThrown(Arrays.asList(null, new Object(), ""));

        // lists containing nulls in the middle
        assertNullPointerExceptionThrown(Arrays.asList("spam", null, new Object()));
        assertNullPointerExceptionThrown(Arrays.asList("spam", null, "eggs", null, new Object()));

        // lists containing nulls at the end
        assertNullPointerExceptionThrown(Arrays.asList("spam", new Object(), null));
        assertNullPointerExceptionThrown(Arrays.asList(new Object(), null));

        // null reference
        assertNullPointerExceptionThrown((Collection<Object>) null);

        // empty list: accepted at the zero-element boundary
        assertNullPointerExceptionNotThrown(Collections.emptyList());

        // list with all non-null elements
        assertNullPointerExceptionNotThrown(Arrays.asList(new Object(), "ham", Integer.valueOf(1)));
        assertNullPointerExceptionNotThrown(Arrays.asList(new Object()));

        // confirms nulls inside nested lists are not considered
        List<Object> containingNull = Arrays.asList((Object) null);
        assertNullPointerExceptionNotThrown(Arrays.asList(containingNull, new Object()));
    }

    @Test
    public void isAnyNonNull() {
        // Empty varargs, all-null elements, and a null array contain no non-null object.
        assertFalse(CollectionUtil.isAnyNonNull());
        assertFalse(CollectionUtil.isAnyNonNull((Object) null));
        assertFalse(CollectionUtil.isAnyNonNull((Object[]) null));
        // At least one non-null object: true for both all-non-null and mixed arguments.
        assertTrue(CollectionUtil.isAnyNonNull(new Object()));
        assertTrue(CollectionUtil.isAnyNonNull(new Object(), null));
    }

    @Test
    public void indexOfSame_listHoldsTheObject_returnsItsPosition() {
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();
        List<List<String>> items = Arrays.asList(first, second);

        assertEquals(first, second, "the two are equal, so only identity tells them apart");
        assertEquals(0, CollectionUtil.indexOfSame(items, first));
        assertEquals(1, CollectionUtil.indexOfSame(items, second));
    }

    @Test
    public void indexOfSame_noSuchObject_returnsNotFound() {
        List<List<String>> items = Arrays.asList(new ArrayList<>(), new ArrayList<>());

        // Equal value but absent reference: not found in a non-empty list.
        assertEquals(CollectionUtil.NOT_FOUND, CollectionUtil.indexOfSame(items, new ArrayList<String>()),
                "an element equal to the target is not the target");
        // Absent null target and empty list are separate not-found representatives.
        assertEquals(CollectionUtil.NOT_FOUND, CollectionUtil.indexOfSame(items, null));
        assertEquals(CollectionUtil.NOT_FOUND, CollectionUtil.indexOfSame(Collections.emptyList(), "ham"));
    }

    @Test
    public void indexOfSame_nullList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CollectionUtil.indexOfSame(null, "ham"));
    }

    /**
     * Asserts that {@code CollectionUtil#requireAllNonNull(Object...)} throws {@code NullPointerException}
     * if {@code objects} or any element of {@code objects} is null.
     */
    private void assertNullPointerExceptionThrown(Object... objects) {
        assertThrows(NullPointerException.class, () -> requireAllNonNull(objects));
    }

    /**
     * Asserts that {@code CollectionUtil#requireAllNonNull(Collection<?>)} throws {@code NullPointerException}
     * if {@code collection} or any element of {@code collection} is null.
     */
    private void assertNullPointerExceptionThrown(Collection<?> collection) {
        assertThrows(NullPointerException.class, () -> requireAllNonNull(collection));
    }

    private void assertNullPointerExceptionNotThrown(Object... objects) {
        requireAllNonNull(objects);
    }

    private void assertNullPointerExceptionNotThrown(Collection<?> collection) {
        requireAllNonNull(collection);
    }
}
