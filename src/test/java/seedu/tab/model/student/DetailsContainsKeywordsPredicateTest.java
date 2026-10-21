package seedu.tab.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import seedu.tab.model.student.DetailsContainsKeywordsPredicate.Field;
import seedu.tab.testutil.StudentBuilder;

public class DetailsContainsKeywordsPredicateTest {

    @Test
    public void equals() {
        List<String> firstPredicateKeywordList = List.of("first");
        List<String> secondPredicateKeywordList = List.of("first", "second");

        DetailsContainsKeywordsPredicate firstPredicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, firstPredicateKeywordList));
        DetailsContainsKeywordsPredicate secondPredicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, secondPredicateKeywordList));

        // same object -> returns true
        assertTrue(firstPredicate.equals(firstPredicate));

        // same values -> returns true
        DetailsContainsKeywordsPredicate firstPredicateCopy =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, firstPredicateKeywordList));
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
        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("Alice")));
        assertTrue(predicate.test(new StudentBuilder().withName("Alice Bob").build()));

        // Single word of a multi-word name
        predicate = new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("Bob")));
        assertTrue(predicate.test(new StudentBuilder().withName("Alice Bob").build()));

        // Partial match
        predicate = new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("lic")));
        assertTrue(predicate.test(new StudentBuilder().withName("Alice Bob").build()));

        // Mixed-case keyword
        predicate = new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("aLIce")));
        assertTrue(predicate.test(new StudentBuilder().withName("Alice Bob").build()));
    }

    @Test
    public void test_digitsInFormattedPhone_returnsTrue() {
        for (String phone : List.of("+65 9123 4567", "9123-4567", "(9123).4567", "9123\u00a04567",
                "9123\u20114567", "9123 4567 ext 21")) {
            Student student = new StudentBuilder().withPhone(phone).build();
            for (String keyword : List.of("91234567", "91234", "345", "7")) {
                assertTrue(new DetailsContainsKeywordsPredicate(Map.of(Field.PHONE, List.of(keyword))).test(student),
                        phone + " should match " + keyword);
            }
        }
        Student student = new StudentBuilder().withPhone("+65 9123 4567").build();
        assertTrue(new DetailsContainsKeywordsPredicate(Map.of(Field.PHONE, List.of("6591234567"))).test(student));
        Student unicodeStudent = new StudentBuilder().withPhone("٩١٢٣-٤٥٦٧").build();
        assertTrue(new DetailsContainsKeywordsPredicate(Map.of(Field.PHONE, List.of("٩١٢٣٤٥٦٧"))).test(unicodeStudent));
    }

    @Test
    public void test_punctuatedDigitsInFormattedPhone_returnsTrue() {
        // a number copied from a contact list carries the +, and someone may type the
        // hyphenated form the UG shows for add while the stored number uses a space
        Student student = new StudentBuilder().withPhone("+65 9123 4567").build();
        for (String keyword : List.of("+6591234567", "9123-4567", "(9123)4567", "9123.4567",
                "+65-9123-4567", "+65")) {
            assertTrue(new DetailsContainsKeywordsPredicate(Map.of(Field.PHONE, List.of(keyword))).test(student),
                    keyword + " should match +65 9123 4567 by its digits");
        }
    }

    @Test
    public void test_punctuatedDigitsNotInPhone_returnsFalse() {
        Student student = new StudentBuilder().withPhone("+65 9123 4567").build();
        for (String keyword : List.of("9123-4568", "999-9999", "ext-21", "---", "+()-.")) {
            assertFalse(new DetailsContainsKeywordsPredicate(Map.of(Field.PHONE, List.of(keyword))).test(student),
                    keyword + " should not match +65 9123 4567");
        }
    }

    @Test
    public void test_formattedPhoneText_preservesLiteralMatching() {
        Student student = new StudentBuilder().withPhone("+65 9123 4567 ext 21").build();
        for (String keyword : List.of("+65", "9123 4567", "EXT 21", "ext")) {
            assertTrue(new DetailsContainsKeywordsPredicate(Map.of(Field.PHONE, List.of(keyword))).test(student));
        }
        for (String keyword : List.of("91234568", "76543219", "123456789", "missing91234567",
                "ext 22", "---")) {
            assertFalse(new DetailsContainsKeywordsPredicate(Map.of(Field.PHONE, List.of(keyword))).test(student));
        }
    }

    @Test
    public void test_digitsAcrossOtherDetailFormatting_returnsFalse() {
        Student student = new StudentBuilder().withName("Lab 9123 4567").withPhone("88888888")
                .withEmail("9123-4567@example.com").withTags("9123-4567").build();
        assertFalse(new DetailsContainsKeywordsPredicate(Map.of(Field.PHONE, List.of("91234567"))).test(student));
    }

    @Test
    public void test_keywordInEmail_returnsTrue() {
        // Student without email should not match email keyword
        StudentBuilder studentBuilder = new StudentBuilder().withName("Alice")
                .withPhone("91234567").withoutEmail()
                .withTags("friend");

        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.EMAIL, List.of("example")));
        assertFalse(predicate.test(studentBuilder.build()));

        // Student with email should match
        studentBuilder = new StudentBuilder().withName("Alice")
                .withPhone("91234567").withEmail("alice@example.com")
                .withTags("friend");
        assertTrue(predicate.test(studentBuilder.build()));
    }

    @Test
    public void test_partialNusStudentIdIgnoringCase_matchesOnlyStudentHoldingIt() {
        Student withoutNusStudentId = new StudentBuilder().withName("Alice").withoutNusStudentId().build();
        Student withNusStudentId = new StudentBuilder(withoutNusStudentId).withNusStudentId("A01234567X").build();
        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NUS_STUDENT_ID, List.of("a0123")));

        assertTrue(predicate.test(withNusStudentId));
        assertFalse(predicate.test(withoutNusStudentId));
    }

    @Test
    public void test_noKeywordMatches_returnsFalse() {
        // Zero keywords
        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of()));
        assertFalse(predicate.test(new StudentBuilder().withName("Alice").build()));

        // Non-matching keyword
        predicate = new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("Carol")));
        assertFalse(predicate.test(new StudentBuilder().withName("Alice Bob")
                .withPhone("91234567").withEmail("alice@example.com")
                .withTags("friend").build()));
    }

    @Test
    public void test_fieldSelectors_matchOnlySelectedField() {
        List<Student> students = List.of(
                new StudentBuilder().withName("Jason Lim").build(),
                new StudentBuilder().withNusStudentId("A01234567X").build(),
                new StudentBuilder().withPhone("+65 9123 4567").build(),
                new StudentBuilder().withEmail("jason@nus.edu.sg").build(),
                new StudentBuilder().withTags("friend").build());
        List<Field> fields = List.of(Field.NAME, Field.NUS_STUDENT_ID, Field.PHONE, Field.EMAIL, Field.TAG);
        assertEquals(List.of(Field.values()), fields);
        List<String> terms = List.of("jASon", "a0123", "9123-4567", "NUS.EDU", "rIeNd");
        for (int i = 0; i < fields.size(); i++) {
            DetailsContainsKeywordsPredicate predicate =
                    new DetailsContainsKeywordsPredicate(Map.of(fields.get(i), List.of(terms.get(i))));
            for (int j = 0; j < students.size(); j++) {
                assertEquals(i == j, predicate.test(students.get(j)), fields.get(i) + " on student " + j);
            }
        }
        Student otherFieldsOnly = new StudentBuilder().withName("Friend Follow-up")
                .withEmail("friend@example.com").withFlag(true).withTags().build();
        assertFalse(new DetailsContainsKeywordsPredicate(Map.of(Field.TAG, List.of("friend")))
                .test(otherFieldsOnly));
        assertFalse(new DetailsContainsKeywordsPredicate(Map.of(Field.TAG, List.of("follow-up")))
                .test(otherFieldsOnly));
    }

    @Test
    public void test_repeatedTagTerms_matchSameOrDifferentTags() {
        DetailsContainsKeywordsPredicate predicate = new DetailsContainsKeywordsPredicate(
                Map.of(Field.NAME, List.of("jason"), Field.TAG, List.of("LAB", "tutorial")));
        assertTrue(predicate.test(new StudentBuilder().withName("Jason Lim").withTags("lab tutorial").build()));
        assertTrue(predicate.test(new StudentBuilder().withName("Jason Zhou")
                .withTags("lab 5", "tutorial 3").build()));
        assertFalse(predicate.test(new StudentBuilder().withName("Jason Tan").withTags("lab").build()));
        assertFalse(predicate.test(new StudentBuilder().withName("Alice").withTags("lab tutorial").build()));
        assertFalse(predicate.test(new StudentBuilder().withName("Jason").withTags().build()));
        DetailsContainsKeywordsPredicate phrase =
                new DetailsContainsKeywordsPredicate(Map.of(Field.TAG, List.of("lab tutorial")));
        assertFalse(phrase.test(new StudentBuilder().withTags("lab", "tutorial").build()));
        assertTrue(phrase.test(new StudentBuilder().withTags("Lab Tutorial 3").build()));
    }

    @Test
    public void test_repeatedFieldTerms_requiresEveryTermWithinThatField() {
        Student student = new StudentBuilder().withName("Jason Lim").withNusStudentId("A01234567X")
                .withPhone("+65 9123 4567").withEmail("jason@nus.edu").build();
        Map<Field, List<String>> matchingTerms = Map.of(
                Field.NAME, List.of("JASON", "Lim"), Field.NUS_STUDENT_ID, List.of("a0123", "567x"),
                Field.PHONE, List.of("+65", "4567"), Field.EMAIL, List.of("jason@", "NUS.EDU"));
        assertTrue(new DetailsContainsKeywordsPredicate(matchingTerms).test(student));
        for (var entry : matchingTerms.entrySet()) {
            assertTrue(new DetailsContainsKeywordsPredicate(Map.of(entry.getKey(), entry.getValue())).test(student));
            assertFalse(new DetailsContainsKeywordsPredicate(Map.of(entry.getKey(),
                    List.of(entry.getValue().getFirst(), "missing"))).test(student));
        }
    }

    @Test
    public void test_optionalFieldsAbsent_returnsFalse() {
        Student student = new StudentBuilder().withoutEmail().withoutNusStudentId().withTags().build();
        for (Field field : List.of(Field.EMAIL, Field.NUS_STUDENT_ID, Field.TAG)) {
            assertFalse(new DetailsContainsKeywordsPredicate(Map.of(field, List.of("x"))).test(student));
        }
    }

    @Test
    public void equals_sameTermsInDifferentFields_returnsFalse() {
        DetailsContainsKeywordsPredicate name =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("friend")));
        DetailsContainsKeywordsPredicate tag =
                new DetailsContainsKeywordsPredicate(Map.of(Field.TAG, List.of("friend")));
        assertFalse(name.equals(tag));
        assertEquals(name, new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("friend"))));
    }

    @Test
    public void toStringMethod() {
        List<String> keywords = List.of("keyword1", "keyword2");
        DetailsContainsKeywordsPredicate predicate =
                new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, keywords));

        String expected = DetailsContainsKeywordsPredicate.class.getCanonicalName()
                + "{keywordsByField=" + Map.of(Field.NAME, keywords) + "}";
        assertEquals(expected, predicate.toString());
    }

}
