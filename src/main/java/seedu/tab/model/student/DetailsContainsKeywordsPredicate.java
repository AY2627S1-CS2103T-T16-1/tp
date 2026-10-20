package seedu.tab.model.student;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Student}'s details match any of the keywords given.
 * A keyword matches when it appears in the student's name, phone, email, NUS Student ID
 * or tags, ignoring case and allowing partial matches, or when it names the follow-up
 * flag by the whole label {@code Needs follow-up} or by the opening letters of one of
 * its words.
 * A keyword made only of digits and the punctuation a phone number is written with
 * also matches the phone's digits with non-digit characters removed.
 */
public class DetailsContainsKeywordsPredicate implements Predicate<Student> {
    /** A keyword of digits and phone punctuation, holding at least one digit. */
    private static final Pattern PHONE_KEYWORD = Pattern.compile("[\\p{Nd}+().-]*\\p{Nd}[\\p{Nd}+().-]*");
    private static final Pattern NON_DIGIT = Pattern.compile("\\P{Nd}");

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
                || matchesAnyTag(student, keyword)
                || student.getFlag().stream().anyMatch(flag -> flag.matchesKeyword(keyword));
    }

    private static boolean matchesPhone(Phone phone, String keyword) {
        return StringUtil.containsIgnoreCase(phone.value, keyword)
                || matchesPhoneByDigits(phone, keyword);
    }

    /**
     * Returns true if the digits {@code keyword} holds appear in {@code phone} in order.
     * The keyword must be one a user would type as a number: made only of digits and the
     * punctuation a phone number is written with, and holding at least one digit, so a
     * keyword such as {@code ---} or one drawn from a name or a tag stays with the literal
     * comparison above.
     */
    private static boolean matchesPhoneByDigits(Phone phone, String keyword) {
        if (!PHONE_KEYWORD.matcher(keyword).matches()) {
            return false;
        }
        String digits = NON_DIGIT.matcher(keyword).replaceAll("");
        return phone.getDigits().contains(digits);
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

