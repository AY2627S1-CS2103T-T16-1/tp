package seedu.tab.model.student;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.commons.util.ToStringBuilder;
import seedu.tab.model.tag.Tag;

/**
 * Tests that a {@code Student}'s details match any of the keywords given.
 * A keyword matches when it appears in the student's name, phone, email, NUS Student ID,
 * tags, or a follow-up flag, ignoring case and allowing partial matches.
 * Digit-only keywords also match the phone's digits with non-digit characters removed.
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
                || matchesPhone(student.getPhone(), keyword)
                || StringUtil.containsIgnoreCase(student.getEmail().map(Email::toString).orElse(""), keyword)
                || StringUtil.containsIgnoreCase(
                        student.getNusStudentId().map(NusStudentId::toString).orElse(""), keyword)
                || matchesAnyTag(student, keyword);
    }

    private static boolean matchesPhone(Phone phone, String keyword) {
        return StringUtil.containsIgnoreCase(phone.value, keyword)
                || keyword.matches("\\p{Nd}+") && phone.value.replaceAll("\\P{Nd}", "").contains(keyword);
    }

    private static boolean matchesAnyTag(Student student, String keyword) {
        Stream<Tag> tagsAndFlag = Stream.concat(student.getTags().stream(), student.getFlag().stream());
        return tagsAndFlag.anyMatch(tag -> StringUtil.containsIgnoreCase(tag.tagName, keyword));
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

