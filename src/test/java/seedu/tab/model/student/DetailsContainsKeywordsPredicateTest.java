package seedu.tab.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.testutil.StudentBuilder;

public class DetailsContainsKeywordsPredicateTest {

    @Test
    public void equals() {
        List<String> firstPredicateKeywordList = List.of("first");
        List<String> secondPredicateKeywordList = List.of("first", "second");

        DetailsContainsKeywordsPredicate firstPredicate =
                new DetailsContainsKeywordsPredicate(firstPredicateKeywordList);
        DetailsContainsKeywordsPredicate secondPredicate =
                new DetailsContainsKeywordsPredicate(secondPredicateKeywordList);

        // same object -> returns true
        assertTrue(firstPredicate.equals(firstPredicate));

        // same values -> returns true
        DetailsContainsKeywordsPredicate firstPredicateCopy =
                new DetailsContainsKeywordsPredicate(firstPredicateKeywordList);
        assertTrue(firstPredicate.equals(firstPredicateCopy));

        // different types -> returns false
        assertFalse(firstPredicate.equals(1));

        // null -> returns false
        assertFalse(firstPredicate.equals(null));

        // different student -> returns false
        assertFalse(firstPredicate.equals(secondPredicate));
    }

    @Test
    public void test_keywordInName_returnsTrue() {
        // Full name match
        DetailsContainsKeywordsPredicate predicate = new DetailsContainsKeywordsPredicate(List.of("Alice"));
        assertTrue(predicate.test(new StudentBuilder().withName("Alice Bob").build()));

        // Single word of a multi-word name
        predicate = new DetailsContainsKeywordsPredicate(List.of("Bob"));
        assertTrue(predicate.test(new StudentBuilder().withName("Alice Bob").build()));

        // Partial match
        predicate = new DetailsContainsKeywordsPredicate(List.of("lic"));
        assertTrue(predicate.test(new StudentBuilder().withName("Alice Bob").build()));

        // Mixed-case keyword
        predicate = new DetailsContainsKeywordsPredicate(List.of("aLIce"));
        assertTrue(predicate.test(new StudentBuilder().withName("Alice Bob").build()));
    }

    @Test
    public void test_keywordInOtherDetails_returnsTrue() {
        StudentBuilder studentBuilder = new StudentBuilder().withName("Alice")
                .withPhone("91234567").withEmail("alice@example.com")
                .withTags("friend", "colleague", "home");

        // Partial phone number match
        DetailsContainsKeywordsPredicate predicate = new DetailsContainsKeywordsPredicate(List.of("234"));
        assertTrue(predicate.test(studentBuilder.build()));

        // Partial email match
        predicate = new DetailsContainsKeywordsPredicate(List.of("alice@example"));
        assertTrue(predicate.test(studentBuilder.build()));

        // Tag match
        predicate = new DetailsContainsKeywordsPredicate(List.of("friend"));
        assertTrue(predicate.test(studentBuilder.build()));

        // Partial tag match
        predicate = new DetailsContainsKeywordsPredicate(List.of("ho"));
        assertTrue(predicate.test(studentBuilder.build()));
    }

    @Test
    public void test_keywordInEmail_returnsTrue() {
        // Student without email should not match email keyword
        StudentBuilder studentBuilder = new StudentBuilder().withName("Alice")
                .withPhone("91234567").withoutEmail()
                .withTags("friend");

        DetailsContainsKeywordsPredicate predicate = new DetailsContainsKeywordsPredicate(List.of("example"));
        assertFalse(predicate.test(studentBuilder.build()));

        // Student with email should match
        studentBuilder = new StudentBuilder().withName("Alice")
                .withPhone("91234567").withEmail("alice@example.com")
                .withTags("friend");
        assertTrue(predicate.test(studentBuilder.build()));
    }

    @Test
    public void test_keywordsMatchingDifferentDetails_returnsTrue() {
        // One keyword matches the name, another only the phone number
        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(List.of("ALICE", "234"));
        assertTrue(predicate.test(new StudentBuilder().withName("Alice Bob")
                .withPhone("91234567").build()));
    }

    @Test
    public void test_noKeywordMatches_returnsFalse() {
        // Zero keywords
        DetailsContainsKeywordsPredicate predicate = new DetailsContainsKeywordsPredicate(List.of());
        assertFalse(predicate.test(new StudentBuilder().withName("Alice").build()));

        // Non-matching keyword
        predicate = new DetailsContainsKeywordsPredicate(List.of("Carol"));
        assertFalse(predicate.test(new StudentBuilder().withName("Alice Bob")
                .withPhone("91234567").withEmail("alice@example.com")
                .withTags("friend").build()));
    }

    @Test
    public void toStringMethod() {
        List<String> keywords = List.of("keyword1", "keyword2");
        DetailsContainsKeywordsPredicate predicate = new DetailsContainsKeywordsPredicate(keywords);

        String expected = DetailsContainsKeywordsPredicate.class.getCanonicalName() + "{keywords=" + keywords + "}";
        assertEquals(expected, predicate.toString());
    }
}
