package seedu.tab.model.student;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.model.tag.Tag;

/**
 * Tests that a {@code Student}'s details match any of the keywords given.
 * A keyword matches when it appears in the student's name, phone, email,
 * or tags, ignoring case and allowing partial matches.
 */
public class DetailsContainsKeywordsPredicate implements Predicate<Student> {
    private final List<String> keywords;

    public DetailsContainsKeywordsPredicate(List<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public boolean test(Student student) {
        return keywords.stream().anyMatch(keyword -> matchesKeyword(student, keyword));
    }

    private static boolean matchesKeyword(Student student, String keyword) {
        return containsIgnoreCase(student.getName().fullName, keyword)
                || containsIgnoreCase(student.getPhone().value, keyword)
                || containsIgnoreCase(student.getEmail().map(Email::toString).orElse(""), keyword)
                || matchesAnyTag(student.getTags(), keyword);
    }

    private static boolean matchesAnyTag(Set<Tag> tags, String keyword) {
        return tags.stream().anyMatch(tag -> containsIgnoreCase(tag.tagName, keyword));
    }

    private static boolean containsIgnoreCase(String sentence, String keyword) {
        return sentence.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof DetailsContainsKeywordsPredicate otherPredicate)) {
            return false;
        }

        return keywords.equals(otherPredicate.keywords);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keywords", keywords).toString();
    }
}
