package seedu.tab.model.student;

import java.util.List;
import java.util.function.Predicate;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Student}'s details match any of the keywords given.
 * A keyword matches when it appears in the student's name, phone, email, NUS Student ID
 * or tags, ignoring case and allowing partial matches, or when it names the follow-up
 * flag as the whole label {@code Needs follow-up} or one of its words.
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
        return StringUtil.containsIgnoreCase(student.getName().fullName, keyword)
                || StringUtil.containsIgnoreCase(student.getPhone().value, keyword)
                || StringUtil.containsIgnoreCase(student.getEmail().map(Email::toString).orElse(""), keyword)
                || StringUtil.containsIgnoreCase(
                        student.getNusStudentId().map(NusStudentId::toString).orElse(""), keyword)
                || matchesAnyTag(student, keyword)
                || student.getFlag().stream().anyMatch(flag -> flag.matchesKeyword(keyword));
    }

    private static boolean matchesAnyTag(Student student, String keyword) {
        return student.getTags().stream().anyMatch(tag -> StringUtil.containsIgnoreCase(tag.tagName, keyword));
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

