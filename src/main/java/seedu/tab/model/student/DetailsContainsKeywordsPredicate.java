package seedu.tab.model.student;

import static java.util.Objects.requireNonNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.commons.util.ToStringBuilder;

/**
 * Tests that a {@code Student}'s details match every search condition given.
 * Each term matches only its selected name, NUS Student ID, phone, email or tag field,
 * ignoring case and allowing partial matches.
 * A keyword made only of digits, grouping whitespace and phone punctuation
 * also matches the phone's digits with non-digit characters removed and equivalent
 * Unicode decimal digits compared by their numeric values.
 */
public class DetailsContainsKeywordsPredicate implements Predicate<Student> {
    /** A keyword of digits, grouping whitespace and phone punctuation, holding at least one digit. */
    private static final Pattern PHONE_KEYWORD = Pattern.compile("(?U)[\\p{Nd}\\s+().-]*\\p{Nd}[\\p{Nd}\\s+().-]*");

    /** The student field in which a search term must occur. */
    public enum Field {
        NAME((student, keyword) -> StringUtil.containsIgnoreCase(student.getName().fullName, keyword)),
        NUS_STUDENT_ID((student, keyword) -> StringUtil.containsIgnoreCase(
                student.getNusStudentId().map(NusStudentId::toString).orElse(""), keyword)),
        PHONE((student, keyword) -> matchesPhone(student.getPhone(), keyword)),
        EMAIL((student, keyword) -> StringUtil.containsIgnoreCase(
                student.getEmail().map(Email::toString).orElse(""), keyword)),
        TAG(DetailsContainsKeywordsPredicate::matchesAnyTag);

        private final BiPredicate<Student, String> matcher;

        Field(BiPredicate<Student, String> matcher) {
            this.matcher = matcher;
        }

        private boolean matches(Student student, String keyword) {
            return matcher.test(student, keyword);
        }
    }

    private final Map<Field, List<String>> keywordsByField;
    private final Optional<Boolean> followUp;

    /** Creates a predicate requiring every term to match its selected field. */
    public DetailsContainsKeywordsPredicate(Map<Field, List<String>> keywordsByField) {
        this(keywordsByField, Optional.empty());
    }

    /** Creates a predicate combining field terms with an optional follow-up status. */
    public DetailsContainsKeywordsPredicate(Map<Field, List<String>> keywordsByField, Optional<Boolean> followUp) {
        this.followUp = requireNonNull(followUp);
        this.keywordsByField = new EnumMap<>(Field.class);
        keywordsByField.forEach((field, keywords) -> this.keywordsByField.put(field, List.copyOf(keywords)));
    }

    @Override
    public boolean test(Student student) {
        return hasAnyCondition() && matchesFollowUp(student) && matchesAllTerms(student);
    }

    private boolean hasAnyCondition() {
        return followUp.isPresent() || keywordsByField.values().stream().anyMatch(keywords -> !keywords.isEmpty());
    }

    private boolean matchesFollowUp(Student student) {
        return followUp.map(isFlagged -> student.isFlagged() == isFlagged).orElse(true);
    }

    private boolean matchesAllTerms(Student student) {
        return keywordsByField.entrySet().stream().allMatch(entry -> entry.getValue().stream()
                .allMatch(keyword -> entry.getKey().matches(student, keyword)));
    }

    private static boolean matchesPhone(Phone phone, String keyword) {
        return StringUtil.containsIgnoreCase(phone.value, keyword)
                || matchesPhoneByDigits(phone, keyword);
    }

    /**
     * Returns true if the digits {@code keyword} holds appear in {@code phone} in order.
     * The keyword must be one a user would type as a number: made only of digits, grouping
     * whitespace and phone punctuation, and holding at least one digit, so a
     * keyword such as {@code ---} or one drawn from a name or a tag stays with the literal
     * comparison above.
     */
    private static boolean matchesPhoneByDigits(Phone phone, String keyword) {
        if (!PHONE_KEYWORD.matcher(keyword).matches()) {
            return false;
        }
        return phone.getDigits().contains(Phone.normalizeDigits(keyword));
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

        return keywordsByField.equals(otherPredicate.keywordsByField) && followUp.equals(otherPredicate.followUp);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("keywordsByField", keywordsByField).add("followUp", followUp).toString();
    }
}

